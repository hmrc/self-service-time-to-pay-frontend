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
import play.api.mvc.Request
import ssttpcalculator.model.{Instalment, InterestRate, LatePaymentInterest, Payable, Payables, PaymentsCalendar, TaxLiability}
import times.ClockProvider

import java.time.LocalDate
import javax.inject.Inject
import scala.annotation.tailrec

class InstalmentsService @Inject() (
    clockProvider:              ClockProvider,
    interestRateService:        InterestRateService,
    latePaymentInterestService: LatePaymentInterestService
)(implicit appConfig: AppConfig) {

  def maximumPossibleInstalmentAmount(journey: Journey)(implicit request: Request[_]): BigDecimal = {
    val liabilities: Seq[TaxLiability] = Payable.taxLiabilities(journey)
    val upfrontPayment = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0))
    val dateNow = clockProvider.nowDate()
    payablesForInstalments(
      liabilities      = liabilities,
      paymentsCalendar = PaymentsCalendar.generate(liabilities, upfrontPayment, dateNow, journey.maybePaymentDayOfMonth),
      upfrontPayment   = upfrontPayment
    ).balance
  }

  def payablesForInstalments(
      liabilities:      Seq[TaxLiability],
      paymentsCalendar: PaymentsCalendar,
      upfrontPayment:   BigDecimal
  ): Payables = {
    Payables(
      liabilitiesFromPlanStartDateLessUpfrontPayment(upfrontPayment, liabilities, paymentsCalendar.planStartDate) ++
        latePaymentInterestService.maybeTotalHistoricInterest(liabilities, paymentsCalendar.planStartDate, interestRateService.getRatesForPeriod) ++
        latePaymentInterestService.maybeUpfrontPaymentLateInterest(liabilities, paymentsCalendar, upfrontPayment)
    )
  }

  def regularInstalments(planStartDate:          LocalDate,
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
          val maybeLatePaymentInterest = latePaymentInterestService.latePaymentInterest(maxPaymentAmount, nextPaymentDate, payables, dateToRate)
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

  private def min(amountA: BigDecimal, amountB: BigDecimal): BigDecimal = if (amountA < amountB) amountA else amountB

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
