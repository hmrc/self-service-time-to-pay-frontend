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
import play.api.Logger
import ssttpcalculator.model.{FixedInterestPeriod, InterestRate, LatePaymentInterest, Payable, Payables, PaymentsCalendar, TaxLiability}

import java.time.{LocalDate, Year}
import javax.inject.Inject

class LatePaymentInterestService @Inject() (durationService: DurationService, interestService: InterestRateService)(
    implicit
    appConfig: AppConfig) {

  val logger: Logger = Logger(getClass)
  val _100 = 100

  def totalHistoricInterest(
      liabilities:   Seq[TaxLiability],
      planStartDate: LocalDate,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  ): BigDecimal = maybeTotalHistoricInterest(liabilities, planStartDate, periodToRates)
    .map(_.amount)
    .getOrElse(BigDecimal(0))

  def maybeTotalHistoricInterest(
      liabilities:   Seq[TaxLiability],
      planStartDate: LocalDate,
      periodToRates: (LocalDate, LocalDate) => Seq[InterestRate]
  ): Option[LatePaymentInterest] = {
    val historicInterest = for {
      debt <- liabilities.filter(_.hasInterestCharge(planStartDate))
      debtFixedInterestPeriods = historicalFixedInterestPeriods(debt, planStartDate, periodToRates)
    } yield calculateHistoricInterest(debtFixedInterestPeriods, planStartDate)

    val totalHistoricInterest = historicInterest.sum
    if (totalHistoricInterest <= 0) None else Some(LatePaymentInterest(amount = historicInterest.sum))
  }

  def maybeUpfrontPaymentLateInterest(
      liabilities:      Seq[TaxLiability],
      paymentsCalendar: PaymentsCalendar,
      upfrontPayment:   BigDecimal
  ): Option[LatePaymentInterest] = paymentsCalendar
    .maybeUpfrontPaymentDate
    .map(_ => LatePaymentInterest(upfrontPaymentLateInterest(liabilities, paymentsCalendar.planStartDate, upfrontPayment)))

  def latePaymentInterest(paymentAmount: BigDecimal, paymentDate: LocalDate, payables: Payables,
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

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def upfrontPaymentLateInterest(
      liabilities:    Seq[TaxLiability],
      planStartDate:  LocalDate,
      upfrontPayment: BigDecimal
  ): BigDecimal = {
    if (upfrontPayment == 0) BigDecimal(0) else {
      val currentInterestRate =
        interestService
          .rateOn(planStartDate)
          .rate

      val currentDailyRate = currentInterestRate / BigDecimal(Year.of(planStartDate.getYear).length()) / BigDecimal(_100)
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
          if (debit.dueDate.isBefore(planStartDate))
            appConfig.daysToProcessFirstPayment
          else
            durationService.getDaysBetween(debit.dueDate, planStartDate.plusWeeks(1))
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

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
