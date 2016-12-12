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
    CalculatorInput.initial.copy(initialPayment = BigDecimal.valueOf(300)))

  val calculatorAmountDue: Debit = Debit(amount = BigDecimal(123.45), dueDate = LocalDate.now())
  val ttpSubmissionNLI: TTPSubmission = TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDue)),
    schedule = Some(calculatorPaymentSchedule))

  val ttpSubmissionNLINoSchedule: TTPSubmission = TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDue)))
  val ttpSubmissionNLIEmpty: TTPSubmission = TTPSubmission()

  val calculatorAmountDueOver10k: Debit = Debit(amount = BigDecimal(11293.22), dueDate = LocalDate.now())
  val ttpSubmissionNLIOver10k: TTPSubmission = TTPSubmission(eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)),
    eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
    calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDueOver10k)))

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
        None,
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
      "192.22",
      LocalDate.now(),
      "722.22",
      LocalDate.now(),
      LocalDate.now(),
      "scheduledPaymentFrequency",
      "162.11",
      LocalDate.now(),
      "282.11"),
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
    "accountName" -> "John Smith",
    "sortCode" -> "121212",
    "accountNumber" -> "12345678"
  )

  val invalidBankDetailsForm = Seq(
    "accountName" -> "Jane Doe",
    "sortCode" -> "123123",
    "accountNumber" -> "12345678"
  )

  val inValidDirectDebitForm = Seq(
    "accountName" -> "John Smith",
    "sortCode" -> "100",
    "accountNumber" -> ""
  )

  val bankDetails = BankDetails("123456", "12345678", None, None, None, None)

  val directDebitBank = DirectDebitBank("", Seq.empty)

  val authorisedUser = Authority("", Accounts(sa = Some(SaAccount("", SaUtr("1234567890")))), None, None, Strong, L200, None, None, None)
  val authorisedUserNoSA = Authority("", Accounts(), None, None, Strong, L200, None, None, None)

  val sessionProvider = new SessionProvider() {}
}
