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

package uk.gov.hmrc.selfservicetimetopay

import java.time.LocalDate

import cats.arrow.Strong
import journey.{Journey, JourneyId}
import play.api.libs.json.{JsValue, Json, Reads}
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.model._
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.Future
import scala.io.Source

package object resources {
  val taxPayerJson = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/taxPayer.json")
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
  val checkEligibilityFalseResponseNotSubmitted = validateAndReturn[JsValue]("test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityFalseResponseNotSubmmited.json")
  def validateAndReturn[T](filename: String)(implicit rds: Reads[T]): T = {
    Json.parse(Source.fromFile(filename).mkString).validate[T].fold(
      error => throw new RuntimeException(s"$error"),
      success => success
    )
  }

  val communicationPreferences = new CommunicationPreferences(false, false, false, false)
  val taxReturn = new Return(
    LocalDate.of(2020: Int, 4: Int, 5: Int),
    Some(LocalDate.of(2019: Int, 1: Int, 25: Int)),
    Some(LocalDate.of(2019: Int, 9: Int, 25: Int)),
    Some(LocalDate.of(2019: Int, 3: Int, 9: Int)))
  val retruns: Seq[Return] = List(taxReturn)

  val debit: Debit = Debit("originCode", 121.2: Double, LocalDate.now(), Some(Interest(Some(LocalDate.now()), 0: Double)), LocalDate.now().minusYears(1))
  val selfAssessment: SelfAssessmentDetails = SelfAssessmentDetails(SaUtr("12345678"), communicationPreferences, List(debit), retruns)
  val taxPayer: Taxpayer = Taxpayer("Bob", List(), selfAssessment)
  val calculatorPaymentScheduleInstalment = Instalment(LocalDate.now(), BigDecimal(1234.22), BigDecimal(0))

  val calculatorPaymentSchedule: PaymentSchedule = PaymentSchedule(
    LocalDate.parse("2001-01-01"),
    LocalDate.parse("2001-01-01"),
    BigDecimal(1024.12),
    BigDecimal(20123.76),
    BigDecimal(1024.12),
    BigDecimal(102.67),
    BigDecimal(20123.76),
    Seq(calculatorPaymentScheduleInstalment,
        calculatorPaymentScheduleInstalment)
  )
  val calculatorPaymentScheduleLessThenOnePayment: PaymentSchedule = PaymentSchedule(
    LocalDate.parse("2001-01-01"),
    LocalDate.parse("2001-01-01"),
    BigDecimal(1024.12),
    BigDecimal(20123.76),
    BigDecimal(1024.12),
    BigDecimal(102.67),
    BigDecimal(20123.76),
    Seq(calculatorPaymentScheduleInstalment,
        calculatorPaymentScheduleInstalment)
  )
  val journeyId = JourneyId("12345A")
  val eventualSchedules: Future[Seq[PaymentSchedule]] = Future.successful(Seq(calculatorPaymentSchedule))
  val calculatorPaymentScheduleMap = Map(2 -> calculatorPaymentSchedule, 3 -> calculatorPaymentSchedule,
    4 -> calculatorPaymentSchedule, 5 -> calculatorPaymentScheduleLessThenOnePayment)

  val calculatorInput = new CalculatorInput(Seq(DebitInput(250, LocalDate.of(2019, 9, 25))), 0: BigDecimal, LocalDate.of(2019, 8, 12), LocalDate.of(2020, 4, 25), Some(LocalDate.of(2019, 9, 25)))

  val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                       Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                       Some(taxPayer),
                                       Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))

  val ttpSubmissionNoAmounts: Journey = Journey(journeyId)

  val calculatorAmountDue: Debit = new Debit(originCode = "IN2", amount = 123.45: Double, dueDate = LocalDate.now(), interest = None, taxYearEnd = LocalDate.of(2020, 4, 5))
  val debitInput = new DebitInput(amount = 123.45: Double, LocalDate.now())
  val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))

  val ttpSubmissionNLINoSchedule: Journey = new Journey(journeyId, maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))
  val ttpSubmissionNLIEmpty: Journey = Journey(journeyId)

  val calculatorAmountDueOver10k: Debit = new Debit(originCode = "IN2", amount = 11293.22: Double, dueDate = LocalDate.now(), interest = None, taxYearEnd = LocalDate.of(2020, 4, 5))
  val ttpSubmissionNLIOver10k: Journey = Journey(_id                 = journeyId, maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))

  val eligibilityStatusOk: EligibilityStatus = EligibilityStatus(true, Seq.empty)
  val eligibilityStatusDebtTooHigh: EligibilityStatus = EligibilityStatus(false, Seq(TotalDebtIsTooHigh))

  val eligibilityTypeOfTaxOk: Option[EligibilityTypeOfTax] = Some(EligibilityTypeOfTax(true, false))

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
      Some("192.22"),
      Some(LocalDate.now()),
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
      "Bob",
      List(),
      SelfAssessmentDetails(
        SaUtr("12345A"),
        communicationPreferences,
        List(),
        retruns)),
    PaymentSchedule(
      LocalDate.parse("2001-01-01"),
      LocalDate.parse("2001-01-01"),
      BigDecimal(1024.12),
      BigDecimal(20123.76),
      BigDecimal(1024.12),
      BigDecimal(102.67),
      BigDecimal(20123.76),
      Seq(Instalment(
        LocalDate.now(),
        BigDecimal(1234.22),
        BigDecimal(0)
      )
      )
    )
  )

  val validDirectDebitForm = Seq(
    "accountName" -> "John Smith",
    "sortCode" -> "121212",
    "accountNumber" -> "12345678",
    "singleAccountHolder" -> "true"
  )

  val invalidBankDetailsForm = Seq(
    "accountName" -> "Jane Doe",
    "sortCode1" -> "12",
    "sortCode2" -> "34",
    "sortCode3" -> "56",
    "accountNumber" -> "12345678",
    "singleAccountHolder" -> "true"
  )

  val inValidDirectDebitForm = Seq(
    "accountName" -> "John Smith",
    "sortCode" -> "100",
    "accountNumber" -> "",
    "singleAccountHolder" -> "true"
  )

  val bankDetails = BankDetails(Some("123456"), Some("12345678"), Some("bank-name"), None, Some("Cersei Lannister"), None)
  val ttpSubmissionWithBankDetails = ttpSubmission.copy(bankDetails = Some(bankDetails))
  val directDebitBank = DirectDebitBank("", Seq.empty)

}
