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

package audit

import audit.model.AuditPaymentSchedule
import config.AppConfig
import journey.Journey
import play.api.libs.json.{JsNumber, JsObject, JsString, Json}
import play.api.mvc.Request
import req.RequestSupport._
import ssttpaffordability.model.{Income, Spending}
import ssttparrangement.SubmissionError
import ssttpcalculator.CalculatorType
import ssttpcalculator.legacy.CalculatorService
import ssttpcalculator.model.PaymentPlanOption.{Additional, Basic, Higher}
import ssttpcalculator.model.PaymentSchedule
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import util.CurrencyUtil.formatToCurrencyString

import java.time.temporal.ChronoUnit

object DataEventFactory {

  val nDDSValidationCheckFailMessage = "Interest greater than or equal to regular payment"

  def directDebitSubmissionFailedEvent(
      journey:         Journey,
      schedule:        PaymentSchedule,
      submissionError: SubmissionError
  )(implicit request: Request[_]): ExtendedDataEvent = {
    val detail = Json.obj(
      "status" -> "failed to submit direct debit didnt bothered to submit TTP Arrangement",
      "submissionError" -> submissionError,
      "utr" -> journey.taxpayer.selfAssessment.utr.value,
      "bankDetails" -> bankDetails(journey),
      "schedule" -> Json.obj(
        "initialPaymentAmount" -> schedule.initialPayment,
        "installments" -> Json.toJson(schedule.instalments.sortBy(_.paymentDate.toEpochDay)),
        "numberOfInstallments" -> schedule.instalments.length,
        "installmentLengthCalendarMonths" -> ChronoUnit.MONTHS.between(schedule.startDate, schedule.endDate),
        "totalPaymentWithoutInterest" -> schedule.amountToPay,
        "totalInterestCharged" -> schedule.totalInterestCharged,
        "totalPayable" -> schedule.totalPayable)
    )

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "directDebitSetup",
      tags        = hcTags("self-assessment-time-to-pay-plan-direct-debit-submission-failed"),
      detail      = detail
    )

  }

  def planNotAvailableEvent(
      journey:             Journey,
      failsNDDSValidation: Boolean = false
  )(implicit request: Request[_]): ExtendedDataEvent = {
    val status = if (failsNDDSValidation) nDDSValidationCheckFailMessage else {
      notAffordableStatus(journey.remainingIncomeAfterSpending)
    }

    val detail = Json.obj(
      "totalDebt" -> journey.debits.map(_.amount).sum.toString,
      "halfDisposableIncome" -> (journey.remainingIncomeAfterSpending / 2).toString,
      "income" -> incomeToJson(journey.maybeIncome),
      "outgoings" -> spendingToJson(journey.maybeSpending),
      "status" -> status,
      "utr" -> journey.taxpayer.selfAssessment.utr
    )

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "ManualAffordabilityCheckFailed",
      tags        = hcTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
      detail      = detail
    )
  }

  private def notAffordableStatus(insufficientRemainingIncomeAfterSpending: BigDecimal): String = {
    if (insufficientRemainingIncomeAfterSpending < 0) "Negative Disposable Income"
    else if (insufficientRemainingIncomeAfterSpending == 0) "Zero Disposable Income"
    else "Plan duration would exceed maximum"
  }

  def planSetUpEvent(journey:           Journey,
                     schedule:          PaymentSchedule,
                     calculatorService: CalculatorService
  )(implicit request: Request[_], appConfig: AppConfig): ExtendedDataEvent = {
    val detail = Json.obj(
      "bankDetails" -> bankDetails(journey),
      "halfDisposableIncome" -> (journey.remainingIncomeAfterSpending / 2).toString,
      "income" -> incomeToJson(journey.maybeIncome),
      "outgoings" -> spendingToJson(journey.maybeSpending),
      "selectionType" -> typeOfPlan(journey, calculatorService),
      "lessThanOrMoreThanTwelveMonths" -> lessThanOrMoreThanTwelveMonths(schedule),
      "schedule" -> Json.toJson(AuditPaymentSchedule(schedule)),
      "status" -> journey.status,
      "arrangementSubmissionStatus" -> journey.maybeArrangementSubmissionStatus,
      "paymentReference" -> journey.ddRef,
      "utr" -> journey.taxpayer.selfAssessment.utr
    )

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "ManualAffordabilityPlanSetUp",
      tags        = hcTags("setup-new-self-assessment-time-to-pay-plan"),
      detail      = detail
    )
  }

  private def typeOfPlan(journey:           Journey,
                         calculatorService: CalculatorService
  )(implicit request: Request[_], appConfig: AppConfig): String = {
    val maybeSelectedPlanAmount = journey.maybeSelectedPlanAmount
    val sa = journey.taxpayer.selfAssessment

    appConfig.calculatorType match {

      case CalculatorType.Legacy =>
        val availablePaymentSchedules = calculatorService.allAvailableSchedules(sa, journey.safeUpfrontPayment, journey.maybePaymentDayOfMonth)
        val closestSchedule = calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
        val defaultPlanOptions = calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

        maybeSelectedPlanAmount.fold("None")(amount => {
          if (amount == defaultPlanOptions(Basic).instalmentAmount) "basic"
          else if (amount == defaultPlanOptions(Higher).instalmentAmount) "higher"
          else if (amount == defaultPlanOptions(Additional).instalmentAmount) "additional"
          else "customAmount"
        })

      case CalculatorType.PaymentOptimised =>
        val remainingIncomeAfterSpending = journey.remainingIncomeAfterSpending

        maybeSelectedPlanAmount.fold("None")(amount => {
          if (amount == remainingIncomeAfterSpending * 0.5) "basic"
          else if (amount == remainingIncomeAfterSpending * 0.6) "higher"
          else if (amount == remainingIncomeAfterSpending * 0.8) "additional"
          else "customAmount"
        })
    }

  }

  private def lessThanOrMoreThanTwelveMonths(selectedSchedule: PaymentSchedule): String = {
    if (selectedSchedule.instalments.length <= 12) "twelveMonthsOrLess" else "moreThanTwelveMonths"
  }

  private def bankDetails(journey: Journey): JsObject = Json.obj(
    "name" -> journey.bankDetails.accountName,
    "accountNumber" -> journey.bankDetails.accountNumber,
    "sortCode" -> journey.bankDetails.sortCode
  )

  private def hcTags(transactionName: String)(implicit request: Request[_]): Map[String, String] = {
    hc.toAuditTags(transactionName, request.path) ++
      Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))
  }

  private def incomeToJson(income: Option[Income]): JsObject = {
    income match {
      case Some(income) =>

        def mapKeyName(key: String) = key match {
            case "monthlyIncome" => "monthlyIncomeAfterTax"
            case "otherIncome"   => "otherMonthlyIncome"
            case other           => other
          }

        val map = income.budgetLines.map { budgetLine =>
          mapKeyName(budgetLine.category.entryName) -> JsString(formatToCurrencyString(budgetLine.amount))
        }.toMap
        JsObject(map) + ("totalIncome" -> JsString(formatToCurrencyString(income.totalIncome)))

      case None => throw new Exception("Income must be in the Journey at this point")
    }
  }

  private def spendingToJson(spending: Option[Spending]): JsObject = {
    spending match {
      case Some(spending) =>

        def mapKeyName(key: String) = key match {
            case "childcare" => "childcareCosts"
            case other       => other
          }

        val map = spending.expenses.map { expense =>
          mapKeyName(expense.category.entryName.stripSuffix("Exp")) -> JsString(formatToCurrencyString(expense.amount))
        }.toMap
        JsObject(map) + ("totalOutgoings" -> JsString(formatToCurrencyString(spending.totalSpending)))

      case None => throw new Exception("Spending must be in the Journey at this point")
    }
  }
}
