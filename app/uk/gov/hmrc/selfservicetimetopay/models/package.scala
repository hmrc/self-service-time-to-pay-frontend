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

import play.api.libs.json._

import uk.gov.hmrc.selfservicetimetopay.models._

import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode

package object modelsFormat {
  implicit val localDateFormatter = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      json.validate[String].map(LocalDate.parse)

    override def writes(o: LocalDate): JsValue = Json.toJson(o.toString)
  }

  implicit def eitherReads[A, B](implicit A: Reads[A], B: Reads[B]): Reads[Either[A, B]] =
    Reads[Either[A, B]] { json =>
      A.reads(json) match {
        case JsSuccess(value, path) => JsSuccess(Left(value), path)
        case JsError(e1) => B.reads(json) match {
          case JsSuccess(value, path) => JsSuccess(Right(value), path)
          case JsError(e2) => JsError(JsError.merge(e1, e2))
        }
      }
    }

  implicit def eitherWrites[A, B](implicit A: Writes[A], B: Writes[B]): Writes[Either[A, B]] =
    Writes[Either[A, B]] {
      case Left(obj) => A.writes(obj)
      case Right(obj) => B.writes(obj)
    }

  //Front end formatters
  implicit val eligibilityTypeOfTaxFormatter: Format[EligibilityTypeOfTax] = Json.format[EligibilityTypeOfTax]
  implicit val eligibilityExistingTTPFormatter: Format[EligibilityExistingTTP] = Json.format[EligibilityExistingTTP]

  //Tax payer formatters
  implicit val addressFormatter: Format[Address] = Json.format[Address]
  implicit val interestFormatter: Format[Interest] = Json.format[Interest]
  implicit val communicationPreferencesFormatter: Format[CommunicationPreferences] = Json.format[CommunicationPreferences]
  implicit val debitFormatter: Format[Debit] = Json.format[Debit]
  implicit val returnsFormatter: Format[Return] = Json.format[Return]
  implicit val selfAssessmentFormatter: Format[SelfAssessment] = Json.format[SelfAssessment]
  implicit val taxPayerFormatter: Format[Taxpayer] = Json.format[Taxpayer]

  //Calculator formatters
  implicit val calculatorAmountsDueFormatter: Format[CalculatorAmountsDue] = Json.format[CalculatorAmountsDue]
  implicit val calculatorDurationFormatter: Format[CalculatorDuration] = Json.format[CalculatorDuration]
  implicit val calculatorPaymentScheduleInstalmentFormatter: Format[CalculatorPaymentScheduleInstalment] = Json.format[CalculatorPaymentScheduleInstalment]
  implicit val calculatorPaymentScheduleFormatter: Format[CalculatorPaymentSchedule] = Json.format[CalculatorPaymentSchedule]
  implicit val calculatorInputFormatter: Format[CalculatorInput] = Json.format[CalculatorInput]

  //Arrangement formatters
  implicit val arrangementDayOfMonthFormatter: Format[ArrangementDayOfMonth] = Json.format[ArrangementDayOfMonth]
  implicit val arrangementDirectDebitFormatter: Format[ArrangementDirectDebit] = Json.format[ArrangementDirectDebit]
  implicit val arrangementFormatter: Format[TTPArrangement] = Json.format[TTPArrangement]

  //Direct debit formatters
  implicit val directDebitInstructionFormatter: Format[DirectDebitInstruction] = Json.format[DirectDebitInstruction]
  implicit val directDebitBanksFormatter: Format[DirectDebitBank] = Json.format[DirectDebitBank]
  implicit val directDebitPaymentPlanFormatter: Format[DirectDebitPaymentPlan] = Json.format[DirectDebitPaymentPlan]
  implicit val directDebitInstructionPaymentPlanFormatter: Format[DirectDebitInstructionPaymentPlan] = Json.format[DirectDebitInstructionPaymentPlan]
  implicit val knownFactFormatter: Format[KnownFact] = Json.format[KnownFact]
  implicit val paymentPlanFormatter: Format[PaymentPlan] = Json.format[PaymentPlan]
  implicit val paymentPlanRequestFormatter: Format[PaymentPlanRequest] = Json.format[PaymentPlanRequest]
  implicit val bankDetailsFormatter: Format[BankDetails] = Json.format[BankDetails]
  implicit val bankAccountResponseFormatter = Format(eitherReads[BankDetails, DirectDebitBank], eitherWrites[BankDetails, DirectDebitBank])

  //Eligibility formatters
  implicit val eligibilityStatusFormatter: Format[EligibilityStatus] = Json.format[EligibilityStatus]
  implicit val eligibilityRequestFormatter: Format[EligibilityRequest] = Json.format[EligibilityRequest]

  //Submission formatter
  implicit val submissionFormatter: Format[TTPSubmission] = Json.format[TTPSubmission]
}

package object controllerVariables {
  def generatePaymentSchedules(amountDue: BigDecimal, payment: Option[BigDecimal]): Seq[CalculatorPaymentSchedule] = {
    var schedules = Seq[CalculatorPaymentSchedule]()
    for (i <- 2 to 11) {
      val interest = BigDecimal("10") * BigDecimal(i)
      var payments = Seq[CalculatorPaymentScheduleInstalment]()
      for (p <- 1 to i) {
        payments = payments :+ CalculatorPaymentScheduleInstalment(LocalDate.now.plusMonths(p),
          ((amountDue - payment.getOrElse(BigDecimal("0")) + interest) / BigDecimal(i)).setScale(2, RoundingMode.HALF_UP))
      }
      schedules = schedules :+ CalculatorPaymentSchedule(Some(LocalDate.now), Some(LocalDate.now.plusMonths(i)),
        payment.getOrElse(BigDecimal("0")),
        amountDue, amountDue - payment.getOrElse(BigDecimal("0")),
        interest, amountDue + interest, payments)
    }
    schedules
  }

  implicit val fakeAmountsDue = CalculatorAmountsDue(Seq(
    Debit(None, BigDecimal("4000.00"), LocalDate.of(2015, 1, 30), None, None),
    Debit(None, BigDecimal("2000.00"), LocalDate.of(2015, 1, 30), None, None)
  ))

  implicit val fakeDebits = Seq(
    Debit(Some("ASST"), BigDecimal("4000.00"), LocalDate.of(2015, 1, 30), None, None),
    Debit(Some("IN1"), BigDecimal("2000.00"), LocalDate.of(2015, 1, 30), None, None),
    Debit(Some("IN2"), BigDecimal("2000.00"), LocalDate.of(2015, 6, 30), None, None),
    Debit(None, BigDecimal("20.00"), LocalDate.of(2015, 2, 14), None, None)
  )

  implicit val arrangementDirectDebit = ArrangementDirectDebit("My Account", 1, 2, 33, 123456789, Some(true))

}
