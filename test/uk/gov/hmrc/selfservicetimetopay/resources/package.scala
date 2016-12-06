/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay

import java.time.LocalDate

import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel.L200
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength.Strong
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, SaAccount}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.util.SessionProvider

import scala.io.Source

package object resources {
  val getBanksResponseJSON = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/GetBanksResponse.json")
  val createPaymentPlanResponseJSON = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/GetDirectDebitInstructionPaymentPlanResponse.json")
  val submitArrangementResponse: TTPArrangement = validateAndReturn[TTPArrangement]("test/uk/gov/hmrc/selfservicetimetopay/resources/GetArrangementResponse.json")
  val submitDebitsRequest = validateAndReturn[CalculatorInput]("test/uk/gov/hmrc/selfservicetimetopay/resources/SubmitLiabilitiesRequest.json")
  val submitLiabilitiesResponseJSON = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/SubmitLiabilitiesResponse.json")
  val getBankResponseJSON = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/GetBank.json")
  val createPaymentRequestJSON = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/CreatePaymentPlanRequest.json")
  val checkEligibilityTrueRequest = validateAndReturn[EligibilityRequest]("test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityTrueRequest.json")
  val checkEligibilityTrueResponse = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityTrueResponse.json")
  val checkEligibilityFalseRequest = validateAndReturn[EligibilityRequest]("test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityFalseRequest.json")
  val checkEligibilityFalseResponse = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityFalseResponse.json")

  def validateAndReturn[T](filename: String)(implicit rds: Reads[T]): T = {
    Json.parse(Source.fromFile(filename).mkString).validate[T].fold(
      error => throw new RuntimeException(s"$error"),
      success => success
    )
  }

  val debit: Debit = Debit(Some("originCode"), BigDecimal(121.2), LocalDate.now(), Some(Interest(LocalDate.now(), BigDecimal(0))), Some(LocalDate.now()))
  val selfAssessment: Option[SelfAssessment] = Some(SelfAssessment(Some("utr"), None, List(debit), None))
  val taxPayer: Taxpayer = Taxpayer(Some("Bob"), List(), selfAssessment)
  val calculatorPaymentScheduleInstalment = CalculatorPaymentScheduleInstalment(LocalDate.now(), BigDecimal(1234.22))

  val calculatorPaymentSchedule: CalculatorPaymentSchedule = CalculatorPaymentSchedule(
    Some(LocalDate.parse("2001-01-01")),
    Some(LocalDate.parse("2001-01-01")),
    BigDecimal(1024.12),
    BigDecimal(20123.76),
    BigDecimal(1024.12),
    BigDecimal(102.67),
    BigDecimal(20123.76),
    Seq(calculatorPaymentScheduleInstalment,
      calculatorPaymentScheduleInstalment)
  )
  val ttpSubmission: TTPSubmission = TTPSubmission(Some(calculatorPaymentSchedule),
    Some(BankDetails("012131", "1234567890", None, None, None, Some("0987654321"))),
    None,//DirectDebitBank
    Some(taxPayer),
    Some(EligibilityTypeOfTax(hasSelfAssessmentDebt = true)),
    Some(EligibilityExistingTTP(Some(false))),
    Some(ArrangementDayOfMonth(1)),
    CalculatorInput.initial.copy(initialPayment = BigDecimal.valueOf(300)))

  val calculatorAmountDue: Debit = Debit(amount = BigDecimal(123.45), dueDate = LocalDate.now())
  val ttpSubmissionNLI: TTPSubmission = TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDue)))

  val calculatorAmountDueOver10k: Debit = Debit(amount = BigDecimal(11293.22), dueDate = LocalDate.now())
  val ttpSubmissionNLIOver10k: TTPSubmission = TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDueOver10k)))

  val eligibilityStatusOk: EligibilityStatus = EligibilityStatus(true, Seq.empty)
  val eligibilityStatusDebtTooHigh: EligibilityStatus = EligibilityStatus(false, Seq("TotalDebtIsTooHigh"))

  val directDebitInstructionPaymentPlan: DirectDebitInstructionPaymentPlan = {
    DirectDebitInstructionPaymentPlan(LocalDate.now().toString, "1234567890", List(
      DirectDebitInstruction(
        None,
        None,
        Some("XXXX"),
        None,
        Some(true),
        Some("XXXX"))
    ), List(
      DirectDebitPaymentPlan("XXX")
    ))
  }

  val paymentPlanRequest: PaymentPlanRequest = PaymentPlanRequest(
    "Requesting service",
    "2017-01-01",
    List(),
    DirectDebitInstruction(
      None,
      None,
      Some("XXXX"),
      None,
      Some(true),
      Some("XXXX")),
    PaymentPlan(
      "ppType",
      "paymentRef",
      "hodService",
      "GBP",
      BigDecimal(192.22),
      LocalDate.now(),
      BigDecimal(722.22),
      LocalDate.now(),
      LocalDate.now(),
      "scheduledPaymentFrequency",
      BigDecimal(162.11),
      LocalDate.now(),
      BigDecimal(282.11)),
    printFlag = true)

  val ttpArrangement: TTPArrangement = TTPArrangement(
    "paymentPlanReference",
    "directDebitReference",
    Taxpayer(
      Some("Bob"),
      List(),
      Some(SelfAssessment(
        Some("utr"),
        None,
        List(),
        None))),
    CalculatorPaymentSchedule(
      Some(LocalDate.parse("2001-01-01")),
      Some(LocalDate.parse("2001-01-01")),
      BigDecimal(1024.12),
      BigDecimal(20123.76),
      BigDecimal(1024.12),
      BigDecimal(102.67),
      BigDecimal(20123.76),
      Seq(CalculatorPaymentScheduleInstalment(
        LocalDate.now(),
        BigDecimal(1234.22))
      )
    )
  )

  val validDirectDebitForm = Seq(
    "accountHolderName" -> "John Smith",
    "sortCode1" -> "12",
    "sortCode2" -> "34",
    "sortCode3" -> "56",
    "accountNumber" -> "12345678",
    "confirmed" -> "true"
  )

  val invalidBankDetailsForm = Seq(
    "accountHolderName" -> "John Smith",
    "sortCode1" -> "65",
    "sortCode2" -> "43",
    "sortCode3" -> "21",
    "accountNumber" -> "87654321",
    "confirmed" -> "true"
  )

  val inValidDirectDebitForm = Seq(
    "accountHolderName" -> "John Smith",
    "sortCode1" -> "100",
    "sortCode2" -> "100",
    "sortCode3" -> "100",
    "accountNumber" -> "12345678",
    "confirmed" -> "true"
  )

  val bankDetails = BankDetails("123456", "12345678", None, None, None, None)

  val directDebitBank = DirectDebitBank("", Seq.empty)

  val authorisedUser = Authority("", Accounts(sa = Some(SaAccount("", SaUtr("1234567890")))), None, None, Strong, L200, None, None, None)
  val authorisedUserNoSA = Authority("", Accounts(), None, None, Strong, L200, None, None, None)

  val sessionProvider = new SessionProvider() {}
}
