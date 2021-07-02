/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ssttpcalculator

import _root_.model._
import bankholidays.WorkingDaysService.addWorkingDays
import journey.Journey
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.{CalculatorInput, Debit, DebitInput, Instalment, PaymentSchedule}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.DAYS
import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorService @Inject() (
    clockProvider:   ClockProvider,
    durationService: DurationService,
    interestService: InterestRateService
)
  (implicit ec: ExecutionContext) {

  import clockProvider._

  val logger = Logger(getClass)

  val minimumMonthsAllowedTTP: Int = 2
  val DebitDueAndCalculationDatesWithinRate: (Boolean, Boolean) = Tuple2(true, true)
  val DebitDueDateWithinRate: (Boolean, Boolean) = Tuple2(true, false)
  val CalculationDateWithinRate: (Boolean, Boolean) = Tuple2(false, true)
  val defaultInitialPaymentDays: Int = 10
  val `14 day gap between the initial payment date and the first scheduled payment date`: Int = 14

  def computeSchedule(journey: Journey)(implicit request: Request[_]): PaymentSchedule = {
    val availableSchedules: Seq[PaymentSchedule] = availablePaymentSchedules(
      journey.taxpayer.selfAssessment,
      journey.safeInitialPayment,
      journey.maybeArrangementDayOfMonth
    )

    val durationInMonths: Int = journey.calculatorDuration

    val schedule: PaymentSchedule = availableSchedules.find(_.instalments.length == durationInMonths)
      .orElse(
        availableSchedules.find(_.instalments.length == durationInMonths - 1)
      ).getOrElse(
          throw new RuntimeException(s"Could not find schedule corresponding to $durationInMonths [${journey}] [${availableSchedules}]")
        )
    schedule
  }

  def availablePaymentSchedules(sa: SelfAssessmentDetails, initialPayment: BigDecimal = BigDecimal(0),
                                maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth])
    (implicit request: Request[_]): List[PaymentSchedule] = {

    val rangeOfAvailableScheduleDurationsInMonths = minimumMonthsAllowedTTP to 13

    val today: LocalDate = clockProvider.nowDate()

    val isAfterTaxYearEndDate = today.isAfter(today.withMonth(4).withDayOfMonth(5))
    val thresholdDate = today
      .plusYears(if (isAfterTaxYearEndDate) 2 else 1)
      .withMonth(1)
      .withDayOfMonth(29) // the last available payment can happen on 28 Jan next year

    val debits = sa.debits.map(asDebitInput)
    rangeOfAvailableScheduleDurationsInMonths.map { durationInMonths =>

      val defaultPreferredDayOfMonth = today
        .plusDays(`14 day gap between the initial payment date and the first scheduled payment date`)
        .plusDays(defaultInitialPaymentDays)
        .getDayOfMonth

      val dayOfMonth = maybeArrangementDayOfMonth.map(_.dayOfMonth).getOrElse(defaultPreferredDayOfMonth)

      val calculatorInput: CalculatorInput = CalculatorService.changeCalculatorInput(
        durationInMonths,
        dayOfMonth,
        initialPayment,
        debits
      )
      buildSchedule(calculatorInput)
    }
      .filter(_.lastPaymentDate.isBefore(thresholdDate))
      .filter(_.instalments.length <= 12) //max 12 instalments
      .toList
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def buildSchedule(implicit calculatorInput: CalculatorInput): PaymentSchedule = {
    // Builds a seq of seq debits,
    // where each sub seq of debits is built from the different interest rate boundaries a debit crosses
    val overallDebits: Seq[Seq[Debit]] =
      calculatorInput
        .debits
        .filter(_.dueDate.isBefore(calculatorInput.startDate))
        .map(processDebit)

    // Calculate interest on old debits that have incurred interest up to the point of the current calculation date (today)
    val totalHistoricInterest: BigDecimal = (for {
      debit <- overallDebits.map(_.filterNot(_.dueDate.isAfter(calculatorInput.startDate)))
    } yield calculateHistoricInterest(debit)).sum

    // Calculate interest for the initial days leading up until when the initial upfront payment is actually taken out of the taxpayer's account
    val hasAnInitialPayment: Boolean = calculatorInput.initialPayment > 0
    val initialPaymentInterest: BigDecimal = if (hasAnInitialPayment) {
      val initialPaymentDate: LocalDate = calculatorInput.startDate.plusDays(defaultInitialPaymentDays)
      val debitsDueBeforeInitialPayment: Seq[DebitInput] = calculatorInput.debits.filter(_.dueDate.isBefore(initialPaymentDate))
      calculateInitialPaymentInterest(debitsDueBeforeInitialPayment)
    } else {
      BigDecimal(0)
    }

    // Calculate the schedule of regular payments on the all debits due before endDate
    val instalments: Seq[Instalment] = calculateStagedPayments

    // Total amount of debt without interest
    val amountToPay = calculatorInput.debits.map(_.amount).sum

    val totalInterest = (instalments.map(_.interest).sum + totalHistoricInterest + initialPaymentInterest).setScale(2, HALF_UP)

    PaymentSchedule(
      startDate            = calculatorInput.startDate,
      endDate              = calculatorInput.endDate,
      initialPayment       = calculatorInput.initialPayment,
      amountToPay          = amountToPay,
      instalmentBalance    = amountToPay - calculatorInput.initialPayment,
      totalInterestCharged = totalInterest,
      totalPayable         = amountToPay + totalInterest,
      instalments          = instalments.init :+ Instalment(
        instalments.last.paymentDate,
        instalments.last.amount + totalInterest,
        instalments.last.interest
      )
    )
  }

  /**
   * Calculate instalments including interest charged on each instalment, while taking into account
   * interest is not charged on debits where initial payment fully or partially clears the oldest debits or
   * if the debit is not liable for interest (due in the future after the end date).
   */
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.Var"))
  def calculateStagedPayments(implicit calculation: CalculatorInput): Seq[Instalment] = {
    // Get the dates of each instalment payment
    val trueFirstPaymentDate =
      calculation
        .firstPaymentDate
        .getOrElse(calculation.startDate)

    val repayments: Seq[LocalDate] = durationService.getRepaymentDates(trueFirstPaymentDate, calculation.endDate)
    val numberOfPayments = BigDecimal(repayments.size)

    var initialPaymentRemaining: BigDecimal = calculation.initialPayment

      def applyInitialPaymentToDebt(debtAmount: BigDecimal): BigDecimal = debtAmount match {
        case amt if amt <= initialPaymentRemaining =>
          initialPaymentRemaining = initialPaymentRemaining - debtAmount; 0
        case amt => val remainingDebt = amt - initialPaymentRemaining; initialPaymentRemaining = 0; remainingDebt
      }

    val instalments = calculation.debits.sortBy(_.dueDate).flatMap { debit =>
      // Check if initial payment has been cleared - if not, then date to calculate interest from is a week later
      val calculateFrom = if (initialPaymentRemaining > 0)
        calculation.startDate.plusWeeks(1) else calculation.startDate

      val calculationDate = if (calculateFrom.isBefore(debit.dueDate))
        debit.dueDate else calculateFrom

      // Subtract the initial payment amount from the debts, beginning with the oldest
      val principal = applyInitialPaymentToDebt(debit.amount)

      val monthlyCapitalRepayment = (principal / numberOfPayments).setScale(2, HALF_UP)

      val currentInterestRate = interestService.rateOn(calculationDate).rate
      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculationDate.getYear).length()) / BigDecimal(100)

      repayments.map { r =>
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(calculationDate, r.plusDays(1)))

        val interest = monthlyCapitalRepayment * currentDailyRate * daysInterestToCharge

        val ins = Instalment(r, monthlyCapitalRepayment, interest)
        //logger.info(s"Repayment $monthlyCapitalRepayment ($calculationDate - $r) $daysInterestToCharge @ $currentDailyRate = $interest")
        ins
      }
    }
    // Combine instalments that are on the same day
    repayments.map { x =>
      instalments.filter(_.paymentDate.isEqual(x)).reduce((z, y) => Instalment(z.paymentDate, z.amount + y.amount, z.interest + y.interest))
    }
  }

  /**
   * Calculate interest for the initial payment amount for the first 7 days until the initial payment is taken out of the taxpayer's account.
   *
   * @param debits - only debits that are not after calculation date plus a week
   */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def calculateInitialPaymentInterest(debits: Seq[DebitInput])(implicit calculation: CalculatorInput): BigDecimal = {
    val currentInterestRate =
      interestService
        .rateOn(calculation.startDate)
        .rate

    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)
    val sortedDebits: Seq[DebitInput] = debits.sortBy(_.dueDate)

      def processDebits(amount: BigDecimal, debits: Seq[DebitInput]): BigDecimal = {
        debits match {
          case debit :: Nil => calculateAmount(amount, debit)._1 * calculateDays(debit) * currentDailyRate
          case debit :: remaining =>
            val result = calculateAmount(amount, debit)
            processDebits(result._2, remaining) + (result._1 * calculateDays(debit) * currentDailyRate)
          case Nil => 0
        }
      }

      def calculateDays(debit: DebitInput): Long = {
        if (debit.dueDate.isBefore(calculation.startDate))
          defaultInitialPaymentDays
        else
          durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))
      }

      // Return - amount (used in the calculation), remaining downPayment
      def calculateAmount(amount: BigDecimal, debit: DebitInput): (BigDecimal, BigDecimal) = {
        if (amount > debit.amount) {
          (debit.amount, amount - debit.amount)
        } else {
          (amount, 0)
        }
      }

    val initPaymentInterest = processDebits(calculation.initialPayment, sortedDebits)
    logger.info(s"InitialPayment Interest: $initPaymentInterest")
    initPaymentInterest
  }

  /**
   * Get the historic interest rates that should be applied to a given debit and split the debit
   * into multiple debits, covering each interest rate.
   */
  private def processDebit(debit: DebitInput)(implicit calculation: CalculatorInput): Seq[Debit] = {
    interestService.getRatesForPeriod(
      debit.dueDate,
      calculation.endDate
    ).map { rate =>
        (rate.containsDate(debit.dueDate), rate.containsDate(calculation.endDate)) match {
          case DebitDueAndCalculationDatesWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case DebitDueDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = debit.dueDate,
            endDate = rate.endDate,
            rate    = rate
          )
          case CalculationDateWithinRate => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = calculation.endDate,
            rate    = rate
          )
          case _ => Debit(
            amount  = debit.amount,
            dueDate = rate.startDate,
            endDate = rate.endDate,
            rate    = rate
          )
        }
      }
  }

  /**
   * Calculate the amount of historic interest on liable debits, taking into account whether
   * the number of days between two dates is inclusive (count one of the dates) or double
   * inclusive (count both days).
   */
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  private def calculateHistoricInterest(debits: Seq[Debit])(implicit calculation: CalculatorInput): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDate(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amount * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amount} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${calculation.startDate}")
      total
    }.sum
  }

  private def historicRateEndDate(debitEndDate: LocalDate)(implicit calculation: CalculatorInput): LocalDate =
    if (debitEndDate.getYear.equals(calculation.startDate.getYear)) calculation.startDate else debitEndDate
}

object CalculatorService {
  private val latestValidPaymentDayOfMonth = 28

  def makeCalculatorInputForPayToday(debits: Seq[DebitInput])(implicit clock: Clock): CalculatorInput = {

    val calculatorInput =
      changeCalculatorInput(
        durationInMonths           = 0,
        preferredPaymentDayOfMonth = now(clock).getDayOfMonth,
        initialPayment             = 0,
        debits                     = debits
      )
    calculatorInput
  }

  def makeCalculatorInput(debits: Seq[DebitInput], initialPayment: BigDecimal, durationInMonths: Int)
    (implicit clock: Clock): CalculatorInput = {

    val noInitialPayment = BigDecimal(0)
    val workingDaysInAWeek = 5

    val currentDate = now(clock)
    val endDate = currentDate.plusMonths(durationInMonths)
    val possibleFirstPaymentDate = addWorkingDays(currentDate, workingDaysInAWeek)

    val firstPaymentDate: LocalDate =
      if (possibleFirstPaymentDate.getDayOfMonth > latestValidPaymentDayOfMonth)
        possibleFirstPaymentDate.withDayOfMonth(1).plusMonths(1)
      else
        possibleFirstPaymentDate

    if (initialPayment > 0) {
      val deferredEndDate = endDate.plusMonths(1)
      val deferredFirstPaymentDate = firstPaymentDate.plusMonths(1)

      if ((debits.map(_.amount).sum - initialPayment) < BigDecimal.exact("32.00")) {
        CalculatorInput(
          startDate        = currentDate,
          initialPayment   = noInitialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          debits           = debits)
      } else {
        CalculatorInput(
          startDate        = currentDate,
          initialPayment   = initialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          debits           = debits)
      }
    } else //no initial payment
      CalculatorInput(
        startDate        = currentDate,
        initialPayment   = noInitialPayment,
        endDate          = endDate,
        firstPaymentDate = Some(firstPaymentDate),
        debits           = debits
      )
  }

  /**
   * As the user can change which day of the month they wish to make their payments, then a recalculation
   * must be made as this would effect the interest amounts. Rules here must be applied and this function
   * calculates the first payment date and the last payment date by applying these rules.
   *
   * Rules:
   * - First payment must be at least 7 days from today's date
   * - The day of the month cannot be greater than 28, if it is then use the 1st of the following month
   * - There must be at least a 14 day gap between the initial payment date and the first scheduled payment date
   */
  def changeCalculatorInput(
      durationInMonths:           Int,
      preferredPaymentDayOfMonth: Int,
      initialPayment:             BigDecimal,
      debits:                     Seq[DebitInput])
    (implicit clock: Clock): CalculatorInput = {

    /*
     * We add 10 days extra capacity just in case if there are 3 bank holidays within the 10 days
     * so ETMP still has chance to create direct debit instruction within 2 working days
     * (it's assumed that weekends aren't working days)
     */
    val fewDays = 10

    val oneWeek = 7
    val twoWeeks = oneWeek * 2

    val startDate = now(clock)
    val startDatePlusOneWeek = startDate.plusWeeks(1)

    val bestMatchingPaymentDayOfMonth = if (preferredPaymentDayOfMonth > latestValidPaymentDayOfMonth) 1 else preferredPaymentDayOfMonth
    val defaultPaymentDate = startDate.withDayOfMonth(bestMatchingPaymentDayOfMonth)
    val defaultPaymentDatePlusOneMonth = defaultPaymentDate.plusMonths(1)
    val defaultPaymentDatePlusTwoMonths = defaultPaymentDate.plusMonths(2)

    val defaultEndDate = defaultPaymentDate.plusMonths(durationInMonths)
    val defaultEndDatePlusOneMonth = defaultEndDate.plusMonths(1)
    val defaultEndDatePlusTwoMonths = defaultEndDate.plusMonths(2)

    val (firstPaymentDate: LocalDate, endDate: LocalDate) =
      if (initialPayment.equals(BigDecimal(0))) {
        val daysBetweenStartAndPaymentDates = DAYS.between(startDate, defaultPaymentDate)

        if (daysBetweenStartAndPaymentDates < fewDays && DAYS.between(startDate, defaultPaymentDatePlusOneMonth) < fewDays)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (daysBetweenStartAndPaymentDates < fewDays)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      } else {
        val paymentDateWithinAWeekOfStartDate = defaultPaymentDate.isBefore(startDatePlusOneWeek)

        if (paymentDateWithinAWeekOfStartDate && DAYS.between(startDatePlusOneWeek, defaultPaymentDatePlusOneMonth) < twoWeeks)
          (defaultPaymentDatePlusTwoMonths, defaultEndDatePlusTwoMonths)
        else if (paymentDateWithinAWeekOfStartDate)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else if (DAYS.between(startDatePlusOneWeek, defaultPaymentDate) < twoWeeks)
          (defaultPaymentDatePlusOneMonth, defaultEndDatePlusOneMonth)
        else
          (defaultPaymentDate, defaultEndDate)
      }

    CalculatorInput(debits, initialPayment, startDate, endDate.minusDays(1), Some(firstPaymentDate))
  }

  val minimumMonthsAllowedTTP = 2

  implicit def ordered[A](implicit ev$1: A => Comparable[_ >: A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

}
