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

import java.time.temporal.ChronoUnit
import audit.model.AuditPaymentSchedule
import journey.Journey
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import req.RequestSupport._
import ssttparrangement.SubmissionError
import ssttpcalculator.model.PaymentSchedule
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier

object DataEventFactory {
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

  def planNotAffordableEvent(journey: Journey)(implicit request: Request[_]): ExtendedDataEvent = {
    val detail = Json.obj(
      "totalDebt" -> journey.debits.map(_.amount).sum.toString,
      "spending" -> journey.maybeSpending.map(_.totalSpending).getOrElse(BigDecimal(0)).toString,
      "income" -> journey.maybeIncome.map(_.totalIncome).getOrElse(BigDecimal(0)).toString,
      "halfDisposableIncome" -> (journey.remainingIncomeAfterSpending / 2).toString,
      "status" -> notAffordableStatus(journey.remainingIncomeAfterSpending),
      "utr" -> journey.taxpayer.selfAssessment.utr
    )

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "ManualAffordabilityCheck",
      tags        = hcTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
      detail      = detail
    )
  }

  private def notAffordableStatus(insufficientRemainingIncomeAfterSpending: BigDecimal): String = {
    if (insufficientRemainingIncomeAfterSpending < 0) "Negative Disposable Income"
    else if (insufficientRemainingIncomeAfterSpending == 0) "Zero Disposable Income"
    else "Plan duration would exceed maximum"
  }

  def planSetUpEvent(journey:  Journey,
                     schedule: PaymentSchedule
  )(implicit request: Request[_]): ExtendedDataEvent = {
    val detail = Json.obj(
      "bankDetails" -> bankDetails(journey),
      "halfDisposableIncome" -> (journey.remainingIncomeAfterSpending / 2).toString,
      "selectionType" -> typeOfPlan(journey),
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

  private def typeOfPlan(journey: Journey): String = {
    val maybeSelectedPlanAmount = journey.maybeSelectedPlanAmount
    val remainingIncomeAfterSpending = journey.remainingIncomeAfterSpending

    maybeSelectedPlanAmount.fold("None")(amount => {
      if (amount == remainingIncomeAfterSpending * 0.5) "fiftyPercent"
      else if (amount == remainingIncomeAfterSpending * 0.6) "sixtyPercent"
      else if (amount == remainingIncomeAfterSpending * 0.8) "eightyPercent"
      else "customAmount"
    })
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
}
