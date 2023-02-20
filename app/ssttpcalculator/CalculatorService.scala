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

import config.AppConfig
import journey.Journey
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.{
  FixedInterestPeriod,
  Instalment,
  InterestRate,
  LatePaymentInterest,
  Payable,
  Payables,
  PaymentSchedule,
  TaxLiability,
  TaxPaymentPlan
}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth
import java.time.{Clock, LocalDate, Year}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class CalculatorService @Inject() (
    clockProvider:   ClockProvider,
    durationService: DurationService,
    interestService: InterestRateService
)
  (implicit ec: ExecutionContext,
   appConfig: AppConfig) {

  val logger: Logger = Logger(getClass)

  val proportionsOfNetMonthlyIncome: Seq[Double] = List(0.5, 0.6, 0.8)

  def scheduleOptions(
      sa:                           SelfAssessmentDetails,
      upfrontPayment:               BigDecimal,
      maybeArrangementDayOfMonth:   Option[ArrangementDayOfMonth],
      remainingIncomeAfterSpending: BigDecimal
  )(implicit clock: Clock, config: AppConfig): Map[Int, PaymentSchedule] = {

    val taxLiabilities: Seq[TaxLiability] = for {
      selfAssessmentDebit <- sa.debits
    } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)

    val firstTaxPaymentPlan = TaxPaymentPlan.safeNew(
      taxLiabilities             = taxLiabilities,
      upfrontPayment             = upfrontPayment,
      regularPaymentAmount       = proportionsOfNetMonthlyIncome(0) * remainingIncomeAfterSpending,
      maybeArrangementDayOfMonth = maybeArrangementDayOfMonth
    )(clock, appConfig)

    val firstSchedule = schedule(firstTaxPaymentPlan)
    val scheduleList = firstSchedule match {
      case None => List()
      case Some(schedule) if schedule.instalments.length <= 1 => List(firstSchedule).flatten
      case _ =>
        val secondTaxPaymentPlan = firstTaxPaymentPlan
          .copy(regularPaymentAmount = proportionsOfNetMonthlyIncome(1) * remainingIncomeAfterSpending)(appConfig)
        val secondSchedule = schedule(secondTaxPaymentPlan)
        secondSchedule match {
          case Some(schedule) if schedule.instalments.length <= 1 => List(firstSchedule, secondSchedule).flatten
          case _ =>
            val thirdTaxPaymentPlan = secondTaxPaymentPlan
              .copy(regularPaymentAmount = proportionsOfNetMonthlyIncome(2) * remainingIncomeAfterSpending)(appConfig)
            val thirdSchedule = schedule(thirdTaxPaymentPlan)
            List(firstSchedule, secondSchedule, thirdSchedule).flatten
        }
    }
    proportionsOfNetMonthlyIncome.map(p => (p * 100).toInt).zip(scheduleList).toMap
  }

  def selectedSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val taxLiabilities: Seq[TaxLiability] = for {
      selfAssessmentDebit <- journey.taxpayer.selfAssessment.debits
    } yield TaxLiability(selfAssessmentDebit.amount, selfAssessmentDebit.dueDate)

    val taxPaymentPlan: TaxPaymentPlan = TaxPaymentPlan(
      taxLiabilities,
      journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0)),
      clockProvider.nowDate(), journey.maybePlanAmountSelection.getOrElse(
        throw new IllegalArgumentException("could not find plan amount selection but there should be one")
      ),
      journey.maybeArrangementDayOfMonth,
      journey.maybePaymentToday
    )

    schedule(taxPaymentPlan)
  }

  def schedule(implicit taxPaymentPlan: TaxPaymentPlan): Option[PaymentSchedule] = {
    val payables = Payables(taxPaymentPlan.taxLiabilities)
    val principal = payables.liabilities.map(_.amount).sum

    val maybeInterestAccruedUpToStartDate = totalHistoricInterest(payables, taxPaymentPlan.planStartDate, interestService.getRatesForPeriod)

    val hasAnUpfrontPayment = taxPaymentPlan.upfrontPayment > 0
    val upfrontPaymentLateInterest = if (hasAnUpfrontPayment) {
      val initialPaymentDate = taxPaymentPlan.planStartDate.plusDays(appConfig.daysToProcessFirstPayment)
      val debitsDueBeforeInitialPayment = taxPaymentPlan.taxLiabilities.filter(_.dueDate.isBefore(initialPaymentDate))
      calculateInitialPaymentInterest(debitsDueBeforeInitialPayment)
    } else { BigDecimal(0) }
    val maybeUpfrontPaymentLateInterest = taxPaymentPlan.maybeUpfrontPaymentDate match {
      case Some(_) => Option(LatePaymentInterest(upfrontPaymentLateInterest))
      case None    => None
    }

    val payablesFromPlanStartDateLessUpfrontPayment = payablesResetLessUpfrontPayment(
      taxPaymentPlan.upfrontPayment,
      payables.liabilities.map(Payable.payableToTaxLiability),
      taxPaymentPlan.planStartDate)
    val liabilitiesUpdated = Seq[Option[Payable]](maybeInterestAccruedUpToStartDate, maybeUpfrontPaymentLateInterest)
      .foldLeft(payablesFromPlanStartDateLessUpfrontPayment)((ls, maybeInterest) => maybeInterest match {
        case Some(interest) => ls :+ interest
        case None           => ls
      })
    val payablesUpdatedWithHistoricAndUpfrontPaymentLateInterest = Payables(liabilitiesUpdated)

    val instalments = regularInstalments(
      taxPaymentPlan.planStartDate,
      taxPaymentPlan.regularPaymentAmount,
      taxPaymentPlan.regularPaymentDates,
      payablesUpdatedWithHistoricAndUpfrontPaymentLateInterest,
      interestService.rateOn)

    instalments match {
      case None => None
      case Some(instalments) =>
        val instalmentLatePaymentInterest = instalments.map(_.interest).sum
        val totalInterestCharged = maybeInterestAccruedUpToStartDate.map(_.amount).getOrElse(BigDecimal(0)) +
          maybeUpfrontPaymentLateInterest.map(_.amount).getOrElse(0) + instalmentLatePaymentInterest
        Some(PaymentSchedule(
          startDate            = taxPaymentPlan.planStartDate,
          endDate              = instalments
            .lastOption.map(_.paymentDate.plusDays(appConfig.lastPaymentDelayDays))
            .getOrElse(throw new IllegalArgumentException("no instalments found to create the schedule")),
          initialPayment       = taxPaymentPlan.upfrontPayment,
          amountToPay          = principal,
          instalmentBalance    = principal - taxPaymentPlan.upfrontPayment,
          totalInterestCharged = totalInterestCharged,
          totalPayable         = principal + totalInterestCharged,
          instalments          = instalments))
    }
  }

  private def payablesResetLessUpfrontPayment(
      initialPayment: BigDecimal,
      liabilities:    Seq[TaxLiability],
      startDate:      LocalDate
  ): Seq[Payable] = {
    val result = liabilities.sortBy(_.dueDate).foldLeft((initialPayment, Seq.empty[TaxLiability])) {
      case ((p, s), lt) if p <= 0         => (p, s :+ lt.copy(dueDate = if (startDate.isBefore(lt.dueDate)) lt.dueDate else startDate))
      case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)
      case ((p, s), lt) =>
        (0, s :+ lt.copy(amount  = lt.amount - p, dueDate = {
          if (startDate.plusWeeks(1).isBefore(lt.dueDate)) lt.dueDate else {
            startDate.plusWeeks(1)
          }
        }))
    }
    result._2
  }

  private def totalHistoricInterest(
      payables:      Payables,
      planStartDate: LocalDate,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val historicInterest = for {
      debt <- payables.liabilities.filter(_.hasInterestCharge(planStartDate))
      debtFixedInterestPeriods = historicalFixedInterestPeriods(debt, planStartDate, periodToRates)

    } yield calculateHistoricInterest(debtFixedInterestPeriods, planStartDate)

    val totalHistoricInterest = historicInterest.sum
    if (totalHistoricInterest <= 0) {
      None
    } else {
      Some(LatePaymentInterest(amount = historicInterest.sum))
    }
  }

  private def historicalFixedInterestPeriods(
      payable:       Payable,
      planStartDate: LocalDate,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  ): Seq[FixedInterestPeriod] = {
    val DebitDueAndCalculationDatesWithinRate: (Boolean, Boolean) = Tuple2(true, true)
    val DebitDueDateWithinRate: (Boolean, Boolean) = Tuple2(true, false)
    val CalculationDateWithinRate: (Boolean, Boolean) = Tuple2(false, true)

    payable match {
      case LatePaymentInterest(_) => Seq()
      case TaxLiability(amount, dueDate) =>
        periodToRates(dueDate, planStartDate)
          .map { rate =>
            (rate.containsDate(dueDate), rate.containsDate(planStartDate)) match {
              case DebitDueAndCalculationDatesWithinRate => FixedInterestPeriod(
                amountAccruingInterest = amount,
                dueDate                = dueDate,
                endDate                = planStartDate,
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
                endDate                = planStartDate,
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

  private def regularInstalments(planStartDate:        LocalDate,
                                 regularPaymentAmount: BigDecimal,
                                 regularPaymentDates:  Seq[LocalDate],
                                 payables:             Payables,
                                 dateToRate:           LocalDate => InterestRate
  ): Option[Seq[Instalment]] = {
    regularInstalmentsRecursive(
      planStartDate,
      regularPaymentDates,
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
          val maybeLatePaymentInterest = latePaymentInterest(maxPaymentAmount, nextPaymentDate, payables, dateToRate)
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

  private def latePaymentInterest(paymentAmount: BigDecimal, paymentDate: LocalDate, payables: Payables,
                                  dateToRate: LocalDate => InterestRate): Option[LatePaymentInterest] = {
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
        .rateOn(calculation.planStartDate)
        .rate

    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(calculation.planStartDate.getYear).length()) / BigDecimal(100)
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
        if (debit.dueDate.isBefore(calculation.planStartDate))
          appConfig.daysToProcessFirstPayment
        else
          durationService.getDaysBetween(debit.dueDate, calculation.planStartDate.plusWeeks(1))
      }

      // Return - amount (used in the calculation), remaining downPayment
      def calculateAmount(amount: BigDecimal, debit: TaxLiability): (BigDecimal, BigDecimal) = {
        if (amount > debit.amount) {
          (debit.amount, amount - debit.amount)
        } else {
          (amount, 0)
        }
      }

    val initPaymentInterest = processDebits(calculation.upfrontPayment, sortedDebits)
    logger.info(s"InitialPayment Interest: $initPaymentInterest")
    initPaymentInterest
  }

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  private def calculateHistoricInterest(debits: Seq[FixedInterestPeriod], planStartDate: LocalDate): BigDecimal = {
    debits.map { debit =>
      val debitRateEndDate = debit.rate.endDate
      val inclusive = if (!(debits.head.equals(debit) | debits.last.equals(debit))) 1 else 0
      val endDate = historicRateEndDateNew(debitRateEndDate, planStartDate)

      val numberOfDays = durationService.getDaysBetween(debit.dueDate, endDate) + inclusive
      val historicRate = debit.historicDailyRate
      val total = historicRate * debit.amountAccruingInterest * numberOfDays

      logger.info(s"Historic interest: rate $historicRate days $numberOfDays amount ${debit.amountAccruingInterest} total = $total")
      logger.info(s"Debit due date: ${debit.dueDate} and end date: $endDate is inclusive: $inclusive")
      logger.info(s"Debit Rate date: $debitRateEndDate and calculation start date: $planStartDate")
      total
    }.sum
  }

  private def historicRateEndDateNew(debitEndDate: LocalDate, planStartDate: LocalDate): LocalDate =
    if (debitEndDate.getYear.equals(planStartDate.getYear)) planStartDate else debitEndDate
}
