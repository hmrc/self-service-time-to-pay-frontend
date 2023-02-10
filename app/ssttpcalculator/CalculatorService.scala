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

import bankholidays.WorkingDaysService.addWorkingDays
import config.AppConfig
import journey.{Journey, PaymentToday}
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.TaxLiability.{amortizedLiabilities, latePayments}
import model.{FixedInterestPeriod, Instalment, InterestRate, LatePayment, LatePaymentInterest, Payable, Payables, Payment, PaymentSchedule, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.DAYS
import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
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

  // NEW for ops-9610

  // TODO [OPS-9610] currently will always create three plans (and hit issues with more than 24 months.
  // should only create plans for 60 and 80% if 50% is not one month long
  // needs to handle not creating a plan if it's more than 24 months long
  // proportions of netMonthlyIncome should be configurable
  def paymentPlanOptions(
      sa:                         SelfAssessmentDetails,
      initialPayment:             BigDecimal                    = BigDecimal(0),
      maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth],
      netMonthlyIncome:           BigDecimal,
      maybePaymentToday:          Option[PaymentToday]
  )(implicit request: Request[_],
    config: AppConfig
  ): List[PaymentSchedule] = {

    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      maybePaymentToday,
      maybeArrangementDayOfMonth,
      clockProvider.nowDate()
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- sa.debits
      } yield TaxLiability(selfAssessmentDebit)
    )

    val schedules = for {
      proportionOfNetMonthlyIncome <- List(0.5, 0.6, 0.7)
      regularPaymentAmount = proportionOfNetMonthlyIncome * netMonthlyIncome
      schedule = buildScheduleNew(
        paymentsCalendar     = paymentsCalendar,
        upfrontPaymentAmount = initialPayment,
        regularPaymentAmount = regularPaymentAmount,
        payables             = payables
      )
    } yield schedule

    schedules.flatten
  }

  def computeSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val paymentsCalendar = paymentDatesService.paymentsCalendar(
      journey.maybePaymentToday,
      journey.maybeArrangementDayOfMonth,
      clockProvider.nowDate()
    )(appConfig)

    val payables = Payables(
      liabilities = for {
        selfAssessmentDebit <- journey.taxpayer.selfAssessment.debits
      } yield TaxLiability(selfAssessmentDebit)
    )

    buildScheduleNew(
      paymentsCalendar     = paymentsCalendar,
      upfrontPaymentAmount = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0)),
      regularPaymentAmount = journey.amount,
      payables             = payables
    )
  }

  def buildScheduleNew(
      paymentsCalendar:     PaymentsCalendar,
      upfrontPaymentAmount: BigDecimal,
      regularPaymentAmount: BigDecimal,
      payables:             Payables
  ): Option[PaymentSchedule] = {
    val principal = payables.liabilities.map(_.amount).sum

    val maybeInterestAccruedUpToStartDate: Option[LatePaymentInterest] = totalHistoricInterest(payables, paymentsCalendar, interestService.getRatesForPeriod)
    val maybeUpfrontPaymentLateInterest: Option[LatePaymentInterest] = paymentsCalendar.maybeUpfrontPaymentDate match {
      case Some(_) => latePaymentInterestFromUpfrontPayment(paymentsCalendar, upfrontPaymentAmount, payables, interestService.getRatesForPeriod)
      case None    => None
    }
    val liabilitiesUpdated = Seq(maybeInterestAccruedUpToStartDate, maybeUpfrontPaymentLateInterest)
      .foldLeft(payables.liabilities)((ls, maybeInterest) => maybeInterest match {
        case Some(interest) => ls :+ interest
        case None           => ls
      })
    val payablesUpdated = payablesLessUpfrontPayment(upfrontPaymentAmount, payables.copy(liabilities = liabilitiesUpdated))

    val instalments = regularInstalments(paymentsCalendar, regularPaymentAmount, payablesUpdated, interestService.rateOn)
    instalments match {
      case None => None
      case Some(instalments) =>
        val instalmentLatePaymentInterest = instalments.map(_.interest).sum

        val totalInterestCharged = maybeInterestAccruedUpToStartDate.map(_.amount).getOrElse(BigDecimal(0)) +
          maybeUpfrontPaymentLateInterest.map(_.amount).getOrElse(0) + instalmentLatePaymentInterest

        Some(PaymentSchedule(
          startDate            = paymentsCalendar.planStartDate,
          endDate              = instalments
            .lastOption.map(_.paymentDate)
            .getOrElse(throw new IllegalArgumentException("no instalments found to create the schedule")),
          initialPayment       = upfrontPaymentAmount,
          amountToPay          = principal,
          instalmentBalance    = (principal - upfrontPaymentAmount),
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
    if (totalHistoricInterest <= 0) { None } else {
      Some(LatePaymentInterest(amount = historicInterest.sum))
    }
  }

  private def latePaymentInterestFromUpfrontPayment(
      paymentsCalendar:     PaymentsCalendar,
      upfrontPaymentAmount: BigDecimal,
      payables:             Payables,
      periodToRates:        (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val upfrontPaymentDate = paymentsCalendar.maybeUpfrontPaymentDate.getOrElse(
      throw new IllegalArgumentException("Calling to calculate late payment interest from upfront payment," +
        " but no upfront payment date found in payments calendar"
      )
    )

    if (payables.inDate(upfrontPaymentDate)) { None } else {
      val latePaymentInterestPortions = for {
        latePayment <- Payables.latePayments(upfrontPaymentAmount, upfrontPaymentDate, payables)
        latePaymentFixedInterestPeriods = fixedInterestPeriodFromLatePayment(latePayment, periodToRates)(paymentsCalendar)
      } yield calculateHistoricInterest(latePaymentFixedInterestPeriods)(paymentsCalendar)

      Some(LatePaymentInterest(amount = latePaymentInterestPortions.sum))
    }
  }

  private def payablesLessUpfrontPayment(upfrontPaymentAmount: BigDecimal, payables: Payables): Payables = {
    payablesUpdated(upfrontPaymentAmount, payables, None)
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

    if (balance <= 0) { Some(instalmentsAggregator) } else {
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
        val currentInterestRate = dateToRate(planStartDate).rate
        val currentDailyRate = currentInterestRate / BigDecimal(Year.of(planStartDate.getYear).length()) / BigDecimal(100)
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(planStartDate, p.payment.date))
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
    if (amountA < amountB) { amountA } else { amountB }
  }

  // End new for ops-9610

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  /**
   * Get the historic interest rates that should be applied to a given debit and split the debit
   * into multiple debits, covering each interest rate.
   */
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

  // TODO OPS 9610: not necessary as only one interest on upfront payment, reimplement to use today's interest rate
  private def fixedInterestPeriodFromLatePayment(
      latePayment:   LatePayment,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  )(implicit paymentsCalendar: PaymentsCalendar): Seq[FixedInterestPeriod] = {
    periodToRates(latePayment.dueDate, paymentsCalendar.planStartDate)
      .map { rate =>
        (rate.containsDate(latePayment.dueDate), rate.containsDate(paymentsCalendar.planStartDate)) match {
          case DebitDueAndCalculationDatesWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = latePayment.dueDate,
            endDate                = paymentsCalendar.planStartDate,
            rate                   = rate
          )
          case DebitDueDateWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = latePayment.dueDate,
            endDate                = rate.endDate,
            rate                   = rate
          )
          case CalculationDateWithinRate => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = rate.startDate,
            endDate                = paymentsCalendar.planStartDate,
            rate                   = rate
          )
          case _ => FixedInterestPeriod(
            amountAccruingInterest = latePayment.amount,
            dueDate                = rate.startDate,
            endDate                = rate.endDate,
            rate                   = rate
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
  private def calculateHistoricInterest(debits: Seq[FixedInterestPeriod])(implicit paymentsCalendar: PaymentsCalendar): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDate(debitRateEndDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amountAccruingInterest * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amountAccruingInterest} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: ${paymentsCalendar.planStartDate}")
      total
    }.sum
  }

  private def historicRateEndDate(debitEndDate: LocalDate)(implicit paymentsCalendar: PaymentsCalendar): LocalDate =
    if (debitEndDate.getYear.equals(paymentsCalendar.planStartDate.getYear)) paymentsCalendar.planStartDate else debitEndDate
}
