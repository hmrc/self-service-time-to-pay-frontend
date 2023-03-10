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
  PaymentsCalendar
}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.RegularPaymentDay
import java.time.{LocalDate, Year}
import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class CalculatorService @Inject() (
    clockProvider:   ClockProvider,
    durationService: DurationService,
    interestService: InterestRateService
)(
    implicit
    ec:        ExecutionContext,
    appConfig: AppConfig
) {

  val logger: Logger = Logger(getClass)

  val proportionsOfNetMonthlyIncome: Seq[Double] = List(0.5, 0.6, 0.8)
  val _100 = 100

  def defaultSchedules(
      sa:                           SelfAssessmentDetails,
      upfrontPayment:               BigDecimal,
      maybeRegularPaymentDay:       Option[RegularPaymentDay],
      remainingIncomeAfterSpending: BigDecimal
  )(implicit request: Request[_]): Map[Int, PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(sa)
    val paymentsCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybeRegularPaymentDay)

    val firstPlanAmount = proportionsOfNetMonthlyIncome(0) * remainingIncomeAfterSpending
    val firstSchedule = schedule(taxLiabilities, firstPlanAmount, paymentsCalendar, upfrontPayment)
    val scheduleList = firstSchedule match {
      case None => List()
      case Some(schedule) if schedule.instalments.length <= 1 => List(firstSchedule).flatten
      case _ =>
        val secondPlanAmount = proportionsOfNetMonthlyIncome(1) * remainingIncomeAfterSpending
        val secondSchedule = schedule(taxLiabilities, secondPlanAmount, paymentsCalendar, upfrontPayment)
        secondSchedule match {
          case Some(schedule) if schedule.instalments.length <= 1 => List(firstSchedule, secondSchedule).flatten
          case _ =>
            val thirdPlanAmount = proportionsOfNetMonthlyIncome(2) * remainingIncomeAfterSpending
            val thirdSchedule = schedule(taxLiabilities, thirdPlanAmount, paymentsCalendar, upfrontPayment)
            List(firstSchedule, secondSchedule, thirdSchedule).flatten
        }
    }
    proportionsOfNetMonthlyIncome.map(p => (p * 100).toInt).zip(scheduleList).toMap
  }

  def customSchedule(
      sa:                     SelfAssessmentDetails,
      upfrontPayment:         BigDecimal,
      maybeRegularPaymentDay: Option[RegularPaymentDay],
      customAmount:           BigDecimal
  )(implicit request: Request[_]): Option[PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(sa)
    val paymentCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybeRegularPaymentDay)
    schedule(taxLiabilities, customAmount, paymentCalendar, upfrontPayment)
  }

  def selectedSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(journey)
    val upfrontPayment = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0))
    val maybeRegularPaymentDay = journey.maybeRegularPaymentDay
    val paymentsCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybeRegularPaymentDay)
    schedule(taxLiabilities, journey.selectedPlanAmount, paymentsCalendar, upfrontPayment)
  }

  def schedule(
      taxLiabilities:       Seq[TaxLiability],
      regularPaymentAmount: BigDecimal,
      paymentsCalendar:     PaymentsCalendar,
      upfrontPayment:       BigDecimal
  ): Option[PaymentSchedule] = {

    regularInstalments(
      paymentsCalendar.planStartDate,
      regularPaymentAmount,
      paymentsCalendar.regularPaymentDates,
      payablesForInstalments(taxLiabilities, paymentsCalendar, upfrontPayment),
      interestService.rateOn
    ) match {
        case None => None

        case Some(instalments) =>
          val principal = taxLiabilities.map(_.amount).sum

          val instalmentLatePaymentInterest = instalments.map(_.interest).sum
          val totalInterestCharged = {
            totalHistoricInterest(Payables(taxLiabilities), paymentsCalendar.planStartDate, interestService.getRatesForPeriod) +
              upfrontPaymentLateInterest(taxLiabilities, paymentsCalendar, upfrontPayment) +
              instalmentLatePaymentInterest
          }

          Some(PaymentSchedule(
            startDate            = paymentsCalendar.planStartDate,
            endDate              = instalments
              .lastOption.map(_.paymentDate.plusDays(appConfig.lastPaymentDelayDays))
              .getOrElse(throw new IllegalArgumentException("no instalments found to create the schedule")),
            initialPayment       = upfrontPayment,
            amountToPay          = principal,
            instalmentBalance    = principal - upfrontPayment,
            totalInterestCharged = totalInterestCharged,
            totalPayable         = principal + totalInterestCharged,
            instalments          = instalments))
      }
  }

  def maximumPossibleInstalmentAmount(journey: Journey)(implicit request: Request[_]): BigDecimal = {
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(journey)
    val upfrontPayment = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0))
    payablesForInstalments(
      liabilities      = taxLiabilities,
      paymentsCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, clockProvider.nowDate(), journey.maybeRegularPaymentDay),
      upfrontPayment   = upfrontPayment
    ).balance
  }

  private def totalHistoricInterest(
      payables:      Payables,
      planStartDate: LocalDate,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  ): BigDecimal = maybeTotalHistoricInterest(payables, planStartDate, periodToRates).map(_.amount).getOrElse(BigDecimal(0))

  private def maybeUpfrontPaymentLateInterest(
      liabilities:      Seq[TaxLiability],
      paymentsCalendar: PaymentsCalendar,
      upfrontPayment:   BigDecimal
  ): Option[LatePaymentInterest] = paymentsCalendar.maybeUpfrontPaymentDate match {
    case Some(_) => Option(LatePaymentInterest(upfrontPaymentLateInterest(liabilities, paymentsCalendar, upfrontPayment)))
    case None    => None
  }

  private def payablesForInstalments(
      liabilities:      Seq[TaxLiability],
      paymentsCalendar: PaymentsCalendar,
      upfrontPayment:   BigDecimal
  ): Payables = {
    Payables(
      Seq[Option[Payable]](
        maybeTotalHistoricInterest(Payables(liabilities), paymentsCalendar.planStartDate, interestService.getRatesForPeriod),
        maybeUpfrontPaymentLateInterest(liabilities, paymentsCalendar, upfrontPayment)
      )
        .foldLeft(liabilitiesFromPlanStartDateLessUpfrontPayment(upfrontPayment, liabilities, paymentsCalendar.planStartDate))(
          (ls, maybeInterest) => maybeInterest match {
            case Some(interest) => ls :+ interest
            case None           => ls
          }
        )
    )
  }

  private def maybeTotalHistoricInterest(
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

  private def liabilitiesFromPlanStartDateLessUpfrontPayment(
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

  private def regularInstalments(planStartDate:          LocalDate,
                                 regularPaymentAmount:   BigDecimal,
                                 regularPaymentDates:    Seq[LocalDate],
                                 payablesForInstalments: Payables,
                                 dateToRate:             LocalDate => InterestRate
  ): Option[Seq[Instalment]] = {
    regularInstalmentsRecursive(
      planStartDate,
      regularPaymentDates,
      regularPaymentAmount,
      payablesForInstalments,
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
          val remainingBalance = payablesUpdatedLessPayment(maxPaymentAmount, payables, maybeLatePaymentInterest).balance

          if (remainingBalance > 0 && remainingPaymentDates.isEmpty) {
            None
          } else {
            regularInstalmentsRecursive(
              planStartDate,
              remainingPaymentDates,
              maxPaymentAmount,
              payablesUpdatedLessPayment(maxPaymentAmount, payables, maybeLatePaymentInterest),
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
        val currentDailyRate = currentInterestRate / BigDecimal(Year.of(p.dueDate.getYear).length()) / BigDecimal(_100)
        val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(p.dueDate, p.payment.date))
        p.payment.amount * currentDailyRate * daysInterestToCharge
      }.sum

      Some(LatePaymentInterest(latePaymentsInterestTotal))
    }
  }

  private def payablesUpdatedLessPayment(paymentAmount: BigDecimal, payables: Payables, maybeInterest: Option[LatePaymentInterest]): Payables = {
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

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def upfrontPaymentLateInterest(
      liabilities:      Seq[TaxLiability],
      paymentsCalendar: PaymentsCalendar,
      upfrontPayment:   BigDecimal
  ): BigDecimal = {
    if (upfrontPayment == 0) BigDecimal(0) else {
      val currentInterestRate =
        interestService
          .rateOn(paymentsCalendar.planStartDate)
          .rate

      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(paymentsCalendar.planStartDate.getYear).length()) / BigDecimal(_100)
      val sortedDebits: Seq[TaxLiability] = liabilities.sortBy(_.dueDate)

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
          if (debit.dueDate.isBefore(paymentsCalendar.planStartDate))
            appConfig.daysToProcessFirstPayment
          else
            durationService.getDaysBetween(debit.dueDate, paymentsCalendar.planStartDate.plusWeeks(1))
        }

        // Return - amount (used in the calculation), remaining downPayment
        def calculateAmount(amount: BigDecimal, debit: TaxLiability): (BigDecimal, BigDecimal) = {
          if (amount > debit.amount) {
            (debit.amount, amount - debit.amount)
          } else {
            (amount, 0)
          }
        }

      val initPaymentInterest = processDebits(upfrontPayment, sortedDebits)
      logger.info(s"InitialPayment Interest: $initPaymentInterest")
      initPaymentInterest
    }
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

  private def min(amountA: BigDecimal, amountB: BigDecimal): BigDecimal = if (amountA < amountB) amountA else amountB

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
