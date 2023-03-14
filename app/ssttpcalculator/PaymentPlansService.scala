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
import ssttpcalculator.model.{Payable, Payables, PaymentSchedule, PaymentsCalendar, TaxLiability}
import times.ClockProvider
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.selfservicetimetopay.models.PaymentDayOfMonth

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.math.BigDecimal.RoundingMode.CEILING

class PaymentPlansService @Inject() (
    clockProvider:              ClockProvider,
    interestRateService:        InterestRateService,
    latePaymentInterestService: LatePaymentInterestService,
    instalmentsService:         InstalmentsService
)(implicit ec: ExecutionContext, appConfig: AppConfig) {

  val logger: Logger = Logger(getClass)
  val proportionsOfNetMonthlyIncome: Seq[Double] = List(0.5, 0.6, 0.8)

  def defaultSchedules(
      sa:                           SelfAssessmentDetails,
      upfrontPayment:               BigDecimal,
      maybePaymentDayOfMonth:       Option[PaymentDayOfMonth],
      remainingIncomeAfterSpending: BigDecimal
  )(implicit request: Request[_]): Map[Int, PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(sa)
    val paymentsCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybePaymentDayOfMonth)

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
      maybePaymentDayOfMonth: Option[PaymentDayOfMonth],
      customAmount:           BigDecimal
  )(implicit request: Request[_]): Option[PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(sa)
    val paymentCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybePaymentDayOfMonth)

    schedule(taxLiabilities, customAmount, paymentCalendar, upfrontPayment)
  }

  def selectedSchedule(journey: Journey)(implicit request: Request[_]): Option[PaymentSchedule] = {
    val dateNow = clockProvider.nowDate()
    val taxLiabilities: Seq[TaxLiability] = Payable.taxLiabilities(journey)
    val upfrontPayment = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0))
    val maybePaymentDayOfMonth = journey.maybePaymentDayOfMonth
    val paymentsCalendar = PaymentsCalendar.generate(taxLiabilities, upfrontPayment, dateNow, maybePaymentDayOfMonth)
    schedule(taxLiabilities, journey.selectedPlanAmount, paymentsCalendar, upfrontPayment)
  }

  def schedule(
      liabilities:       Seq[TaxLiability],
      paymentDayOfMonth: BigDecimal,
      paymentsCalendar:  PaymentsCalendar,
      upfrontPayment:    BigDecimal
  ): Option[PaymentSchedule] = {

    instalmentsService.regularInstalments(
      paymentsCalendar.planStartDate,
      paymentDayOfMonth,
      paymentsCalendar.regularPaymentDates,
      instalmentsService.payablesForInstalments(liabilities, paymentsCalendar, upfrontPayment),
      interestRateService.rateOn
    ) match {
        case None => None

        case Some(instalments) =>
          val planStartDate = paymentsCalendar.planStartDate
          val principal = liabilities.map(_.amount).sum

          val instalmentLatePaymentInterest = instalments.map(_.interest).sum
          val totalInterestCharged = {
            latePaymentInterestService.totalHistoricInterest(liabilities, planStartDate, interestRateService.getRatesForPeriod) +
              latePaymentInterestService.upfrontPaymentLateInterest(liabilities, planStartDate, upfrontPayment) +
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
    val sa = journey.taxpayer.selfAssessment
    val upfrontPayment = journey.maybePaymentTodayAmount.map(_.value).getOrElse(BigDecimal(0))
    val maybePaymentDayOfMonth = journey.maybePaymentDayOfMonth
    val principal = sa.debits.map(_.amount).sum

    customSchedule(sa, upfrontPayment, maybePaymentDayOfMonth, principal)
      .map(schedule => schedule.instalmentBalance + schedule.totalInterestCharged)
      .getOrElse(throw new IllegalArgumentException("could not generate plan"))
  }

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

}
