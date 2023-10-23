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

import audit.model.{AuditIncome, AuditPaymentSchedule, AuditSpending}
import bars.model.ValidateBankDetailsResponse
import config.AppConfig
import journey.Journey
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.mvc.Request
import req.RequestSupport._
import ssttparrangement.SubmissionError
import ssttpcalculator.legacy.CalculatorService
import ssttpcalculator.model.PaymentPlanOption.{Additional, Basic, Higher}
import ssttpcalculator.model.PaymentSchedule
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, TypeOfAccountDetails}
import util.CurrencyUtil.{formatToCurrencyString, formatToCurrencyStringWithTrailingZeros}

import java.time.temporal.ChronoUnit
import scala.math.BigDecimal.RoundingMode.HALF_UP

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
        "initialPaymentAmount" -> formatToCurrencyStringWithTrailingZeros(schedule.initialPayment),
        "installments" -> Json.toJson(schedule.instalments.sortBy(_.paymentDate.toEpochDay)),
        "numberOfInstallments" -> schedule.instalments.length,
        "installmentLengthCalendarMonths" -> ChronoUnit.MONTHS.between(schedule.startDate, schedule.endDate),
        "totalPaymentWithoutInterest" -> formatToCurrencyStringWithTrailingZeros(schedule.amountToPay),
        "totalInterestCharged" -> formatToCurrencyString(schedule.totalInterestCharged),
        "totalPayable" -> formatToCurrencyStringWithTrailingZeros(schedule.totalPayable))
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
      "totalDebt" -> formatToCurrencyStringWithTrailingZeros(journey.debits.map(_.amount).sum),
      "halfDisposableIncome" -> formatToCurrencyStringWithTrailingZeros((journey.remainingIncomeAfterSpending / 2).setScale(2, HALF_UP)),
      "income" -> Json.toJson(AuditIncome.fromIncome(journey.maybeIncome)),
      "outgoings" -> Json.toJson(AuditSpending.fromSpending(journey.maybeSpending)),
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
      "halfDisposableIncome" -> formatToCurrencyStringWithTrailingZeros((journey.remainingIncomeAfterSpending / 2).setScale(2, HALF_UP)),
      "income" -> Json.toJson(AuditIncome.fromIncome(journey.maybeIncome)),
      "outgoings" -> Json.toJson(AuditSpending.fromSpending(journey.maybeSpending)),
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

  def barsValidateEvent(sortCode:                  String,
                        accountNumber:             String,
                        accountName:               String,
                        maybeTypeOfAccountDetails: Option[TypeOfAccountDetails],
                        saUtr:                     SaUtr,
                        barsResp:                  ValidateBankDetailsResponse
  )(implicit request: Request[_]): ExtendedDataEvent = {

    val detail = Json.obj(
      "utr" -> saUtr.value,
      "request" -> barsRequest(sortCode, accountNumber, accountName, maybeTypeOfAccountDetails),
      "response" -> barsResponse(barsResp)
    )

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "BARSCheck",
      tags        = hcTags("BARSCheck"),
      detail      = detail
    )
  }

  def eligibilityNotEnrolledEvent(credentials: Option[Credentials])(implicit request: Request[_]): ExtendedDataEvent =
    eligibilityEventDetail(Left("notEnrolled"), None, None, credentials)

  def eligibilityInactiveEnrolmentEvent(
      saUtr:       Option[SaUtr],
      credentials: Option[Credentials]
  )(implicit request: Request[_]): ExtendedDataEvent =
    eligibilityEventDetail(Left("inactiveEnrolment"), None, saUtr, credentials)

  def eligibilityResultEvent(eligibilityStatus: EligibilityStatus,
                             totalDebt:         BigDecimal,
                             saUtr:             SaUtr,
                             credentials:       Option[Credentials]
  )(implicit request: Request[_]): ExtendedDataEvent =
    eligibilityEventDetail(Right(eligibilityStatus), Some(totalDebt), Some(saUtr), credentials)

  private def eligibilityEventDetail(
      eligibilityResult: Either[String, EligibilityStatus],
      totalDebt:         Option[BigDecimal],
      saUtr:             Option[SaUtr],
      credentials:       Option[Credentials]
  )(implicit request: Request[_]): ExtendedDataEvent = {
    val eligibilityDetail = eligibilityResult.fold(
      enrolmentReason => Json.obj(
        "eligibilityResult" -> "ineligible",
        "enrollmentReasons" -> enrolmentReason),
      {
        eligibilityStatus =>
          val reasonsJson =
            if (eligibilityStatus.reasons.isEmpty) Json.obj()
            else Json.obj("eligibilityReasons" -> eligibilityStatus.reasons.map(_.name))

          reasonsJson ++ Json.obj(
            "eligibilityResult" -> (if (eligibilityStatus.eligible) "eligible" else "ineligible")
          )
      }
    )

    val detail = eligibilityDetail ++
      asJsObject(credentials.map(_.providerId), "authProviderId") ++
      asJsObject(totalDebt.map(formatToCurrencyStringWithTrailingZeros), "totalDebt") ++
      asJsObject(saUtr.map(_.value), "utr") ++
      Json.obj("taxType" -> "SA")

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "EligibilityCheck",
      tags        = hcTags("EligibilityCheck"),
      detail      = detail
    )
  }

  private def typeOfPlan(journey:           Journey,
                         calculatorService: CalculatorService
  )(implicit request: Request[_], appConfig: AppConfig): String = {
    val maybeSelectedPlanAmount = journey.maybeSelectedPlanAmount
    val sa = journey.taxpayer.selfAssessment

    val availablePaymentSchedules = calculatorService.allAvailableSchedules(
      sa,
      journey.safeUpfrontPayment,
      journey.maybePaymentDayOfMonth,
      journey.dateFirstPaymentCanBeTaken
    )
    val closestSchedule = calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
    val defaultPlanOptions = calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

    maybeSelectedPlanAmount.fold("None")(amount => {
      if (amount == defaultPlanOptions(Basic).instalmentAmount) "basic"
      else if (amount == defaultPlanOptions(Higher).instalmentAmount) "higher"
      else if (amount == defaultPlanOptions(Additional).instalmentAmount) "additional"
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

  private def barsRequest(sortCode:             String,
                          accountNumber:        String,
                          accountName:          String,
                          TypeOfAccountDetails: Option[TypeOfAccountDetails]): JsObject =
    Json.obj(
      "account" -> Json.obj(
        "accountType" -> TypeOfAccountDetails.map(_.typeOfAccount),
        "accountHolderName" -> accountName,
        "sortCode" -> sortCode,
        "accountNumber" -> accountNumber
      )
    )

  private def barsResponse(barsResp: ValidateBankDetailsResponse): JsObject = Json.obj(
    "isBankAccountValid" -> barsResp.isValid,
    "barsResponse" -> Json.obj(
      "accountNumberIsWellFormatted" -> barsResp.accountNumberIsWellFormatted.toString,
      "nonStandardAccountDetailsRequiredForBacs" -> barsResp.nonStandardAccountDetailsRequiredForBacs.toString,
      "sortCodeIsPresentOnEISCD" -> barsResp.sortCodeIsPresentOnEISCD.toString,
      "sortCodeBankName" -> barsResp.sortCodeBankName,
      "sortCodeSupportsDirectDebit" -> barsResp.sortCodeSupportsDirectDebit.map(_.toString),
      "sortCodeSupportsDirectCredit" -> barsResp.sortCodeSupportsDirectCredit.map(_.toString),
      "iban" -> barsResp.iban
    )
  )

  private def hcTags(transactionName: String)(implicit request: Request[_]): Map[String, String] =
    hc.toAuditTags(transactionName, request.path) ++
      Map(hc.names.deviceID -> hc.deviceID.getOrElse("-"))

  private def asJsObject[A: Writes](maybeA: Option[A], fieldName: String): JsObject =
    maybeA.fold(Json.obj())(a => Json.obj(fieldName -> a))

}
