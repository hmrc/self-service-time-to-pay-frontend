/*
 * Copyright 2023 HM Revenue & Customs
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
import config.AppConfig
import journey.{Journey, PaymentToday}
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.TaxLiability.{amortizedLiabilities, latePayments}
import ssttpcalculator.model.{Debit, FixedInterestPeriod, Instalment, InterestRate, LatePayment, LatePaymentInterest, Payable, Payables, Payment, PaymentSchedule, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.DAYS
import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorService @Inject() (
    clockProvider:       ClockProvider,
    durationService:     DurationService,
    interestService:     InterestRateService,
    paymentDatesService: PaymentDatesService
)
  (implicit ec: ExecutionContext,
   appConfig: AppConfig) {

  import clockProvider._

  val logger = Logger(getClass)

  val minimumMonthsAllowedTTP: Int = 2
  val DebitDueAndCalculationDatesWithinRate: (Boolean, Boolean) = Tuple2(true, true)
  val DebitDueDateWithinRate: (Boolean, Boolean) = Tuple2(true, false)
  val CalculationDateWithinRate: (Boolean, Boolean) = Tuple2(false, true)
  val defaultInitialPaymentDays: Int = 11
  val `14 day gap between the initial payment date and the first scheduled payment date`: Int = 14
  val LastPaymentDelayDays = 7

  val proportionsOfNetMonthlyIncome = List(0.5, 0.6, 0.8)

  def scheduleOptions(
      sa:                           SelfAssessmentDetails,
      initialPayment:               BigDecimal                    = BigDecimal(0),
      maybeArrangementDayOfMonth:   Option[ArrangementDayOfMonth],
      remainingIncomeAfterSpending: BigDecimal,
      maybePaymentToday:            Option[PaymentToday],
      dateToday:                    LocalDate
  )(implicit config: AppConfig): List[PaymentSchedule] = {

    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      maybePaymentToday,
      maybeArrangementDayOfMonth,
      dateToday
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- sa.debits
      } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)
    )

    val firstTaxPaymentPlan = TaxPaymentPlan(
      liabilities                = payables.liabilities.map(Payable.payableToTaxLiability),
      initialPayment             = initialPayment,
      startDate                  = paymentsCalendar.planStartDate,
      endDate                    = LocalDate.parse("2060-03-11"),
      firstPaymentDate           = Some(paymentsCalendar.regularPaymentDates.headOption
        .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
      ),
      maybeArrangementDayOfMonth = maybeArrangementDayOfMonth,
      regularPaymentAmount       = proportionsOfNetMonthlyIncome(0) * remainingIncomeAfterSpending,
      maybePaymentToday)

    val firstSchedule = schedule(firstTaxPaymentPlan)
    firstSchedule match {
      case Some(schedule) if (schedule.instalments.length <= 1) => List(firstSchedule).flatten
      case _ =>
        val secondTaxPaymentPlan = firstTaxPaymentPlan
          .copy(regularPaymentAmount = proportionsOfNetMonthlyIncome(1) * remainingIncomeAfterSpending)
        val secondSchedule = schedule(secondTaxPaymentPlan)
        secondSchedule match {
          case Some(schedule) if (schedule.instalments.length <= 1) => List(firstSchedule, secondSchedule).flatten
          case _ =>
            val thirdTaxPaymentPlan = secondTaxPaymentPlan
              .copy(regularPaymentAmount = proportionsOfNetMonthlyIncome(2) * remainingIncomeAfterSpending)
            val thirdSchedule = schedule(thirdTaxPaymentPlan)
            List(firstSchedule, secondSchedule, thirdSchedule).flatten
        }
    }
  }

  def selectedSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      journey.maybePaymentToday,
      journey.maybeArrangementDayOfMonth,
      clockProvider.nowDate()
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- journey.taxpayer.selfAssessment.debits
      } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)
    )

    val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
      payables.liabilities.map(Payable.payableToTaxLiability),
      journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0)),
      paymentsCalendar.planStartDate,
      LocalDate.parse("2060-03-11"),
      Some(paymentsCalendar.regularPaymentDates.headOption
        .getOrElse(throw new IllegalArgumentException("could not find first regular payment date, but there should be one"))
      ), journey.maybeArrangementDayOfMonth,
      journey.maybePlanAmountSelection.getOrElse(throw new IllegalArgumentException("could not find plan amount selection but there should be one")),
      journey.maybePaymentToday
    )

    schedule(taxPaymentPlan)
  }

  def schedule(implicit taxPaymentPlan: TaxPaymentPlan): Option[PaymentSchedule] = {
    val payables: Payables = Payables(taxPaymentPlan.liabilities)
    val principal = payables.liabilities.map(_.amount).sum

    logger.info(s"payables: $payables")
    logger.info(s"principal: $principal")

    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      maybePaymentToday          = taxPaymentPlan.maybePaymentToday,
      maybeArrangementDayOfMonth = taxPaymentPlan.firstPaymentDate.map(date => ArrangementDayOfMonth(date.getDayOfMonth)),
      dateToday                  = taxPaymentPlan.startDate
    )

    logger.info(s"payments:Calendar: $paymentsCalendar")

    val maybeInterestAccruedUpToStartDate: Option[LatePaymentInterest] = {
      totalHistoricInterest(payables, paymentsCalendar, interestService.getRatesForPeriod)
    }

    logger.info(s"maybeInterestAcccruedToStartDate: $maybeInterestAccruedUpToStartDate")

    val hasAnInitialPayment: Boolean = taxPaymentPlan.initialPayment > 0
    val upfrontPaymentLateInterest: BigDecimal = if (hasAnInitialPayment) {
      val initialPaymentDate: LocalDate = taxPaymentPlan.startDate.plusDays(defaultInitialPaymentDays)
      val debitsDueBeforeInitialPayment: Seq[TaxLiability] = taxPaymentPlan.liabilities.filter(_.dueDate.isBefore(initialPaymentDate))
      calculateInitialPaymentInterest(debitsDueBeforeInitialPayment)
    } else {
      BigDecimal(0)
    }

    logger.info(s"hasAnInitialPayment: $hasAnInitialPayment")
    logger.info(s"upfrontPaymentLateInterest: $upfrontPaymentLateInterest")

    val maybeUpfrontPaymentLateInterest: Option[LatePaymentInterest] = paymentsCalendar.maybeUpfrontPaymentDate match {
      case Some(_) => Option(LatePaymentInterest(upfrontPaymentLateInterest))
      case None    => None
    }

    logger.info(s"maybeUpFrontPaymentLateInterest: $maybeUpfrontPaymentLateInterest")

      def payablesResetLessUpfrontPayment(
          initialPayment: BigDecimal,
          liabilities:    Seq[TaxLiability],
          startDate:      LocalDate
      ): Seq[Payable] = {
        val result = liabilities.sortBy(_.dueDate).foldLeft((initialPayment, Seq.empty[TaxLiability])) {
          case ((p, s), lt) if p <= 0         => (p, s :+ lt.copy(dueDate = if (startDate.isBefore(lt.dueDate)) lt.dueDate else startDate))
          case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)
          case ((p, s), lt) => {
            (0, s :+ lt.copy(amount  = lt.amount - p, dueDate = {
              if (startDate.plusWeeks(1).isBefore(lt.dueDate)) lt.dueDate else {
                startDate.plusWeeks(1)
              }
            }))
          }
        }
        result._2
      }

    val resultPayablesResetLessUpfrontAlternative = payablesResetLessUpfrontPayment(
      taxPaymentPlan.initialPayment,
      payables.liabilities.map(Payable.payableToTaxLiability),
      paymentsCalendar.planStartDate
    )

    logger.info(s"resultPayablesResetLessUpfrontAlternative: $resultPayablesResetLessUpfrontAlternative")

    val liabilitiesUpdated: Seq[Payable] = Seq[Option[Payable]](maybeInterestAccruedUpToStartDate, maybeUpfrontPaymentLateInterest)
      .foldLeft(resultPayablesResetLessUpfrontAlternative)((ls, maybeInterest) => maybeInterest match {
        case Some(interest) => ls :+ interest
        case None           => ls
      })

    logger.info(s"liabilitiesUpdated: $liabilitiesUpdated")

    val payablesUpdated = Payables(liabilitiesUpdated)

    logger.info(s"payablesUpdated: $payablesUpdated")

    logger.info(s"tax payment plan: $taxPaymentPlan")

    val instalments = regularInstalments(paymentsCalendar, taxPaymentPlan.regularPaymentAmount, payablesUpdated, interestService.rateOn)

    logger.info(s"instalments: $instalments")

    instalments match {
      case None => None
      case Some(instalments) =>
        val instalmentLatePaymentInterest = instalments.map(_.interest).sum
        val totalInterestCharged = maybeInterestAccruedUpToStartDate.map(_.amount).getOrElse(BigDecimal(0)) +
          maybeUpfrontPaymentLateInterest.map(_.amount).getOrElse(0) + instalmentLatePaymentInterest
        Some(PaymentSchedule(
          startDate            = paymentsCalendar.planStartDate,
          endDate              = instalments
            .lastOption.map(_.paymentDate.plusDays(appConfig.lastPaymentDelayDays))
            .getOrElse(throw new IllegalArgumentException("no instalments found to create the schedule")),
          initialPayment       = taxPaymentPlan.initialPayment,
          amountToPay          = principal,
          instalmentBalance    = (principal - taxPaymentPlan.initialPayment),
          totalInterestCharged = totalInterestCharged,
          totalPayable         = (principal + totalInterestCharged),
          instalments          = instalments
        ))
    }
  }

  private def totalHistoricInterest(
      payables:         Payables,
      paymentsCalendar: PaymentsCalendar,
      periodToRates:    (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val historicInterest = for {
      debt <- payables.liabilities.filter(_.hasInterestCharge(paymentsCalendar.planStartDate))
      debtFixedInterestPeriods = historicalFixedInterestPeriods(debt, periodToRates)(paymentsCalendar)

    } yield calculateHistoricInterest(debtFixedInterestPeriods)(paymentsCalendar)

    val totalHistoricInterest = historicInterest.sum
    if (totalHistoricInterest <= 0) {
      None
    } else {
      Some(LatePaymentInterest(amount = historicInterest.sum))
    }
  }

  private def historicalFixedInterestPeriods(
      payable:       Payable,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  )(implicit paymentsCalendar: PaymentsCalendar): Seq[FixedInterestPeriod] = {
    payable match {
      case LatePaymentInterest(_) => Seq()
      case TaxLiability(amount, dueDate) => {
        periodToRates(dueDate, paymentsCalendar.planStartDate)
          .map { rate =>
            (rate.containsDate(dueDate), rate.containsDate(paymentsCalendar.planStartDate)) match {
              case DebitDueAndCalculationDatesWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = dueDate,
                endDate                = paymentsCalendar.planStartDate,
                rate                   = rate
              )
              case DebitDueDateWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = dueDate,
                endDate                = rate.endDate,
                rate                   = rate
              )
              case CalculationDateWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = rate.startDate,
                endDate                = paymentsCalendar.planStartDate,
                rate                   = rate
              )
              case _ => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = rate.startDate,
                endDate                = rate.endDate,
                rate                   = rate
              )
            }
          }
      }
    }

  }

  private def regularInstalments(paymentsCalendar:     PaymentsCalendar,
                                 regularPaymentAmount: BigDecimal,
                                 payables:             Payables,
                                 dateToRate:           LocalDate => InterestRate
  ): Option[Seq[Instalment]] = {
    regularInstalmentsRecursive(
      paymentsCalendar.planStartDate,
      paymentsCalendar.regularPaymentDates,
      regularPaymentAmount,
      payables,
      dateToRate,
      Seq.empty[Instalment]
    )
  }

  @tailrec
  private def regularInstalmentsRecursive(
      planStartDate:         LocalDate,
      regularPaymentDates:   Seq[LocalDate],
      maxPaymentAmount:      BigDecimal,
      payables:              Payables,
      dateToRate:            LocalDate => InterestRate,
      instalmentsAggregator: Seq[Instalment]
  ): Option[Seq[Instalment]] = {
    val paymentDate = regularPaymentDates.headOption
    val balance = payables.balance

    if (balance <= 0) {
      Some(instalmentsAggregator)
    } else {
      paymentDate match {
        case None => None
        case Some(nextPaymentDate) =>
          val maybeLatePaymentInterest = latePaymentInterest(maxPaymentAmount, nextPaymentDate, payables, dateToRate, planStartDate)
          val latePaymentInterestAmount: BigDecimal = maybeLatePaymentInterest.map(_.amount).getOrElse(0)

          val instalment = Instalment(
            paymentDate = nextPaymentDate,
            amount      = min(maxPaymentAmount, balance + latePaymentInterestAmount),
            interest    = latePaymentInterestAmount
          )
          val updatedInstalments = instalmentsAggregator :+ instalment

          val remainingPaymentDates = regularPaymentDates.drop(1)
          val remainingBalance = payablesUpdated(maxPaymentAmount, payables, maybeLatePaymentInterest).balance

          if (remainingBalance > 0 && remainingPaymentDates.isEmpty) {
            None
          } else {
            regularInstalmentsRecursive(
              planStartDate,
              remainingPaymentDates,
              maxPaymentAmount,
              payablesUpdated(maxPaymentAmount, payables, maybeLatePaymentInterest),
              dateToRate,
              updatedInstalments
            )
          }
      }
    }
  }

  private def latePaymentInterest(
      paymentAmount: BigDecimal,
      paymentDate:   LocalDate,
      payables:      Payables,
      dateToRate:    LocalDate => InterestRate,
      planStartDate: LocalDate
  ): Option[LatePaymentInterest] = {
    if (payables.inDate(paymentDate)) {
      None
    } else {
      val latePayments = Payables.latePayments(paymentAmount, paymentDate, payables)

      val latePaymentsInterestTotal = latePayments.map { p =>
        val currentInterestRate = dateToRate(p.dueDate).rate
        val currentDailyRate = currentInterestRate / BigDecimal(Year.of(p.dueDate.getYear).length()) / BigDecimal(100)
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(p.dueDate, p.payment.date))
        p.payment.amount * currentDailyRate * daysInterestToCharge
      }.sum

      Some(LatePaymentInterest(latePaymentsInterestTotal))
    }
  }

  private def payablesUpdated(paymentAmount: BigDecimal, payables: Payables, maybeInterest: Option[LatePaymentInterest]): Payables = {
    val liabilitiesWithLatePaymentInterest = maybeInterest match {
      case Some(interest) => payables.liabilities :+ interest
      case None           => payables.liabilities
    }
    val updatedLiabilities = liabilitiesWithLatePaymentInterest.foldLeft((paymentAmount, Seq.empty[Payable])) {
      case ((amount, newSeqBuilder), liability) if amount <= 0 =>
        (amount, newSeqBuilder :+ liability)

      case ((amount, newSeqBuilder), liability) if amount >= liability.amount =>
        (amount - liability.amount, newSeqBuilder)

      case ((paymentAmount, newSeqBuilder), TaxLiability(amount, dueDate)) =>
        (0, newSeqBuilder :+ TaxLiability(amount - paymentAmount, dueDate))

      case ((amount, newSeqBuilder), LatePaymentInterest(interest)) =>
        (0, newSeqBuilder :+ LatePaymentInterest(interest - amount))
    }._2
    Payables(updatedLiabilities)
  }

  private def min(amountA: BigDecimal, amountB: BigDecimal): BigDecimal = {
    if (amountA < amountB) {
      amountA
    } else {
      amountB
    }
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  /**
   * Calculate interest for the initial payment amount for the first 7 days until the initial payment is taken out of the taxpayer's account.
   *
   * @param debits - only debits that are not after calculation date plus a week
   */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def calculateInitialPaymentInterest(debits: Seq[TaxLiability])(implicit calculation: TaxPaymentPlan): BigDecimal = {
    val currentInterestRate =
      interestService
        .rateOn(calculation.startDate)
        .rate

    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.startDate.getYear).length()) / BigDecimal(100)
    val sortedDebits: Seq[TaxLiability] = debits.sortBy(_.dueDate)

      def processDebits(amount: BigDecimal, debits: Seq[TaxLiability]): BigDecimal = {
        debits.toList match {
          case List(debit) => calculateAmount(amount, debit)._1 * calculateDays(debit) * currentDailyRate
          case debit :: remaining =>
            val result = calculateAmount(amount, debit)
            processDebits(result._2, remaining) + (result._1 * calculateDays(debit) * currentDailyRate)
          case Nil => 0
        }
      }

      def calculateDays(debit: TaxLiability): Long = {
        if (debit.dueDate.isBefore(calculation.startDate))
          defaultInitialPaymentDays
        else
          durationService.getDaysBetween(debit.dueDate, calculation.startDate.plusWeeks(1))
      }

      // Return - amount (used in the calculation), remaining downPayment
      def calculateAmount(amount: BigDecimal, debit: TaxLiability): (BigDecimal, BigDecimal) = {
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

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  private def calculateHistoricInterest(debits: Seq[FixedInterestPeriod])(implicit paymentsCalendar: PaymentsCalendar): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDateNew(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amountAccruingInterest * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amountAccruingInterest} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${paymentsCalendar.planStartDate}")
      total
    }.sum
  }

  private def historicRateEndDateNew(debitEndDate: LocalDate)(implicit paymentsCalendar: PaymentsCalendar): LocalDate =
    if (debitEndDate.getYear.equals(paymentsCalendar.planStartDate.getYear)) paymentsCalendar.planStartDate else debitEndDate
}

object CalculatorService {
  private val latestValidPaymentDayOfMonth = 28

  def makeCalculatorInputForPayToday(debits: Seq[TaxLiability])(implicit clock: Clock): TaxPaymentPlan = {

    val taxPaymentPlan =
      changePaymentPlan(
        durationInMonths           = 0,
        preferredPaymentDayOfMonth = now(clock).getDayOfMonth,
        initialPayment             = 0,
        debits                     = debits
      )
    taxPaymentPlan
  }

  def makeTaxPaymentPlan(debits: Seq[TaxLiability], initialPayment: BigDecimal, durationInMonths: Int)
    (implicit clock: Clock): TaxPaymentPlan = {

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
        TaxPaymentPlan(
          startDate        = currentDate,
          initialPayment   = noInitialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          liabilities      = debits)
      } else {
        TaxPaymentPlan(
          startDate        = currentDate,
          initialPayment   = initialPayment,
          firstPaymentDate = Some(deferredFirstPaymentDate),
          endDate          = deferredEndDate,
          liabilities      = debits)
      }
    } else //no initial payment
      TaxPaymentPlan(
        startDate        = currentDate,
        initialPayment   = noInitialPayment,
        endDate          = endDate,
        firstPaymentDate = Some(firstPaymentDate),
        liabilities      = debits
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
  def changePaymentPlan(
      durationInMonths:           Int,
      preferredPaymentDayOfMonth: Int,
      initialPayment:             BigDecimal,
      debits:                     Seq[TaxLiability])
    (implicit clock: Clock): TaxPaymentPlan = {

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

    TaxPaymentPlan(debits, initialPayment, startDate, endDate.minusDays(1), Some(firstPaymentDate))
  }

  val minimumMonthsAllowedTTP = 2

  implicit def ordered[A](implicit ev$1: A => Comparable[_ >: A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }

}
