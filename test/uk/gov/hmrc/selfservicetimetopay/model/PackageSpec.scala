/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.model

import java.time.LocalDate

import play.api.libs.json._
import testsupport.UnitSpec
import testsupport.testdata.TdAll._
import testsupport.testdata.CalculatorTd._
import testsupport.testdata.ArrangementTd._
import testsupport.testdata.DirectDebitTd._
import testsupport.testdata.SessionInfoTd._
import token.{TTPSessionId, Token, TokenData}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class PackageSpec extends UnitSpec {

  "Frontend Formatters" in {

    val localDate = LocalDate.of(2019, 9, 12)
    val localDateString = "2019-09-12"

    Json.toJson(localDate) shouldBe JsString(localDateString) withClue "serialize"
    JsString(localDateString).as[LocalDate] shouldBe localDate withClue "deserialize"

    Json.toJson(eligibilityTypeOfTax) shouldBe eligibilityTypeOfTaxJson withClue "serialize"
    eligibilityTypeOfTaxJson.as[EligibilityTypeOfTax] shouldBe eligibilityTypeOfTax withClue "deserialize"
  }

  "TaxPayer Formatters" in {
    Json.toJson(address) shouldBe addressJson withClue "serialize"
    addressJson.as[Address] shouldBe address withClue "deserialize"

    Json.toJson(interest) shouldBe interestJson withClue "serialize"
    interestJson.as[Interest] shouldBe interest withClue "deserialize"

    Json.toJson(communicationPreferences) shouldBe communicationPreferencesJson withClue "serialize"
    communicationPreferencesJson.as[CommunicationPreferences] shouldBe communicationPreferences withClue "deserialize"

    Json.toJson(debit1) shouldBe debit1Json withClue "serialize"
    debit1Json.as[Debit] shouldBe debit1 withClue "deserialize"

    Json.toJson(return1) shouldBe return1Json withClue "serialize"
    return1Json.as[Return] shouldBe return1 withClue "deserialize"

    Json.toJson(selfAssessment) shouldBe selfAssessmentJson withClue "serialize"
    selfAssessmentJson.as[SelfAssessment] shouldBe selfAssessment withClue "deserialize"

    Json.toJson(taxpayer) shouldBe taxpayerJson withClue "serialize"
    taxpayerJson.as[Taxpayer] shouldBe taxpayer withClue "deserialize"
  }

  "Calculator Formatters" in
    {
      Json.toJson(calculatorAmountsDue) shouldBe calculatorAmountsDueJson withClue "serialize"
      calculatorAmountsDueJson.as[CalculatorAmountsDue] shouldBe calculatorAmountsDue withClue "deserialize"

      Json.toJson(calculatorDuration) shouldBe calculatorDurationJson withClue "serialize"
      calculatorAmountsDueJson.as[CalculatorAmountsDue] shouldBe calculatorAmountsDue withClue "deserialize"

      Json.toJson(calculatorPaymentScheduleInstalment) shouldBe calculatorPaymentScheduleInstalmentJson withClue "serialize"
      calculatorPaymentScheduleInstalmentJson.as[CalculatorPaymentScheduleInstalment] shouldBe (calculatorPaymentScheduleInstalment) withClue "deserialize"

      Json.toJson(calculatorPaymentSchedule) shouldBe calculatorPaymentScheduleJson withClue "serialize"
      calculatorPaymentScheduleJson.as[CalculatorPaymentSchedule] shouldBe calculatorPaymentSchedule withClue "deserialize"

      Json.toJson(calculatorInput) shouldBe calculatorInputJson withClue "serialize"
      calculatorInputJson.as[CalculatorInput] shouldBe calculatorInput withClue "deserialize"
    }

  "Arrangement Formatters" in {
    Json.toJson(arrangementDayOfMonth) shouldBe arrangementDayOfMonthJson withClue "serialize"
    arrangementDayOfMonthJson.as[ArrangementDayOfMonth] shouldBe arrangementDayOfMonth withClue "deserialize"

    Json.toJson(arrangementDirectDebit) shouldBe arrangementDirectDebitJson withClue "serialize"
    arrangementDirectDebitJson.as[ArrangementDirectDebit] shouldBe arrangementDirectDebit withClue "deserialize"

    Json.toJson(tTPArrangement) shouldBe tTPArrangementJson withClue "serialize"
    tTPArrangementJson.as[TTPArrangement] shouldBe tTPArrangement withClue "deserialize"
  }

  "DirectDebit Formatters" in {
    Json.toJson(directDebitInstruction) shouldBe directDebitInstructionJson withClue "serialize"
    directDebitInstructionJson.as[DirectDebitInstruction] shouldBe directDebitInstruction withClue "deserialize"

    Json.toJson(directDebitBank) shouldBe directDebitBankJson withClue "serialize"
    directDebitBankJson.as[DirectDebitBank] shouldBe directDebitBank withClue "deserialize"

    Json.toJson(directDebitPaymentPlan) shouldBe directDebitPaymentPlanJson withClue "serialize"
    directDebitPaymentPlanJson.as[DirectDebitPaymentPlan] shouldBe directDebitPaymentPlan withClue "deserialize"

    Json.toJson(directDebitInstructionPaymentPlan) shouldBe directDebitInstructionPaymentPlanJson withClue "serialize"
    directDebitInstructionPaymentPlanJson.as[DirectDebitInstructionPaymentPlan] shouldBe directDebitInstructionPaymentPlan withClue "deserialize"

    Json.toJson(knownFact) shouldBe knownFactJson withClue "serialize"
    knownFactJson.as[KnownFact] shouldBe knownFact withClue "deserialize"

    Json.toJson(paymentPlan) shouldBe paymentPlanJson withClue "serialize"
    paymentPlanJson.as[PaymentPlan] shouldBe paymentPlan withClue "deserialize"

    Json.toJson(paymentPlanRequest) shouldBe paymentPlanRequestJson withClue "serialize"
    paymentPlanRequestJson.as[PaymentPlanRequest] shouldBe paymentPlanRequest withClue "deserialize"

    Json.toJson(bankDetails) shouldBe bankDetailsJson withClue "serialize"
    bankDetailsJson.as[BankDetails] shouldBe bankDetails withClue "deserialize"

    val rightDirectDebitBank: Either[BankDetails, DirectDebitBank] = Right(directDebitBank)
    val leftBankDetails: Either[BankDetails, DirectDebitBank] = Left(bankDetails)

    Json.toJson(rightDirectDebitBank) shouldBe directDebitBankJson
    Json.toJson(leftBankDetails) shouldBe bankDetailsJson
  }

  "Eligibility Formatters" in {
    parseFromString("NoDebt") shouldBe Some(NoDebt)
    parseFromString("debtisinsignificant  ") shouldBe Some(DebtIsInsignificant)
    parseFromString("olddEbtistoOhigh  ") shouldBe Some(OldDebtIsTooHigh)
    parseFromString("totaldebtistoohigh") shouldBe Some(TotalDebtIsTooHigh)
    parseFromString("  ttpisleSSthentwomonths") shouldBe Some(TotalDebtIsTooHigh)
    parseFromString("   ISNOTONIA   ") shouldBe Some(IsNotOnIa)
    parseFromString("returnneedssubmitting") shouldBe Some(ReturnNeedsSubmitting)
    parseFromString("ISNOTONIA returnneedssubmitting") shouldBe Some(ReturnNeedsSubmitting)
    parseFromString("blah blah blah ") shouldBe None
  }

  "EligibilityReasons Formatters" in {
    val noDebtJsonString = JsString("NoDebt")

    Json.toJson(NoDebt) shouldBe noDebtJsonString withClue "serialize"
    formatEligibilityReasons.reads(noDebtJsonString) shouldBe JsSuccess(NoDebt) withClue "deserialize"

    formatEligibilityReasons.reads(JsNumber(404)) shouldBe JsError(s"Failed to parse 404 as Reason")
  }

  "Not Logged in journey Formatters" in {
    Json.toJson(notLoggedInJourneyInfo) shouldBe notLoggedInJourneyInfoJson withClue "serialize"
    notLoggedInJourneyInfoJson.as[NotLoggedInJourneyInfo] shouldBe notLoggedInJourneyInfo withClue "deserialize"

    Json.toJson(eligibilityStatus) shouldBe eligibilityStatusJson withClue "serialize"
    eligibilityStatusJson.as[EligibilityStatus] shouldBe eligibilityStatus withClue "deserialize"

    Json.toJson(eligibilityRequest) shouldBe eligibilityRequestJson withClue "serialize"
    eligibilityRequestJson.as[EligibilityRequest] shouldBe eligibilityRequest withClue "deserialize"

  }

  "Submission Formatters" in {
    Json.toJson(tTPSubmission) shouldBe tTPSubmissionJson withClue "serialize"
    tTPSubmissionJson.as[TTPSubmission] shouldBe tTPSubmission withClue "deserialize"

    Json.toJson(tTPSessionId) shouldBe tTPSessionIdJson withClue "serialize"
    tTPSessionIdJson.as[TTPSessionId] shouldBe tTPSessionId withClue "deserialize"

    Json.toJson(tokenTd) shouldBe tokenTdJson withClue "serialize"
    tokenTdJson.as[Token] shouldBe tokenTd withClue "deserialize"

    Json.toJson(tokenData) shouldBe tokenDataJson withClue "serialize"
    tokenDataJson.as[TokenData] shouldBe tokenData withClue "deserialize"
  }

}
