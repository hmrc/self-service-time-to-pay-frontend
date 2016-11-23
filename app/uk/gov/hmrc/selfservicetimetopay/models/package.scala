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

import scala.math.BigDecimal
import scala.math.BigDecimal.RoundingMode
import java.time.LocalDate

import play.api.http.Status._
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import uk.gov.hmrc.play.http.{HttpErrorFunctions, HttpReads, HttpResponse}
import uk.gov.hmrc.selfservicetimetopay.models._

package object modelsFormat {
  implicit val localDateFormatter = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      json.validate[String].map(LocalDate.parse)

    override def writes(o: LocalDate): JsValue = Json.toJson(o.toString)
  }

  //Front end formatters
  implicit val calculatorPaymentTodayFormatter = Json.format[CalculatorPaymentToday]
  implicit val eligibilityDebtTypeFormatter = Json.format[EligibilityDebtType]
  implicit val eligibilityExistingTTPFormatter = Json.format[EligibilityExistingTTP]

  //Calculator formatters
  implicit val calculatorLiabilityFormatter = Json.format[CalculatorDebit]
  implicit val calculatorAmountDueFormatter = Json.format[CalculatorAmountDue]
  implicit val calculatorAmountsDueFormatter = Json.format[CalculatorAmountsDue]
  implicit val calculatorDurationFormatter = Json.format[CalculatorDuration]
  implicit val calculatorPaymentScheduleInstalmentFormatter = Json.format[CalculatorPaymentScheduleInstalment]
  implicit val calculatorPaymentScheduleFormatter = Json.format[CalculatorPaymentSchedule]
  implicit val calculatorInputFormatter = Json.format[CalculatorInput]

  //Tax payer formatters
  implicit val addressFormatter = Json.format[Address]
  implicit val interestFormatter = Json.format[Interest]
  implicit val communicationPreferencesFormatter = Json.format[CommunicationPreferences]
  implicit val debitFormatter = Json.format[Debit]
  implicit val returnsFormatter = Json.format[Return]
  implicit val selfAssessmentFormatter = Json.format[SelfAssessment]
  implicit val taxPayerFormatter = Json.format[TaxPayer]

  //Arrangement formatters
  implicit val arrangementDayOfMonthFormatter = Json.format[ArrangementDayOfMonth]
  implicit val arrangementDirectDebitFormatter = Json.format[ArrangementDirectDebit]
  implicit val arrangementFormatter = Json.format[TTPArrangement]

  //Direct debit formatters
  implicit val directDebitInstructionFormatter = Json.format[DirectDebitInstruction]
  implicit val directDebitBanksFormatter = Json.format[DirectDebitBank]
  implicit val directDebitPaymentPlanFormatter = Json.format[DirectDebitPaymentPlan]
  implicit val directDebitInstructionPaymentPlanFormatter = Json.format[DirectDebitInstructionPaymentPlan]
  implicit val knownFactFormatter = Json.format[KnownFact]
  implicit val paymentPlanFormatter = Json.format[PaymentPlan]
  implicit val paymentPlanRequestFormatter = Json.format[PaymentPlanRequest]
  implicit val bankDetailsFormatter = Json.format[BankDetails]
  implicit val bankAccountFormatter = Json.format[BankAccount]

  //Eligibility formatters
  implicit val eligibilityStatusFormatter = Json.format[EligibilityStatus]
}

package object controllerVariables {
  def generatePaymentSchedules(amountDue:BigDecimal, payment:Option[BigDecimal]):Seq[CalculatorPaymentSchedule] = {
    var schedules = Seq[CalculatorPaymentSchedule]()
    for(i  <- 2 to 11) {
      val interest = BigDecimal("10") * BigDecimal(i)
      var payments = Seq[CalculatorPaymentScheduleInstalment]()
      for (p <- 1 to i) {
        payments = payments :+ CalculatorPaymentScheduleInstalment(LocalDate.now.plusMonths(p),
          ((amountDue - payment.getOrElse(BigDecimal("0")) + interest)/BigDecimal(i)).setScale(2, RoundingMode.HALF_UP))
      }
      schedules = schedules :+ CalculatorPaymentSchedule(Some(LocalDate.now), Some(LocalDate.now.plusMonths(i)),
        payment.getOrElse(BigDecimal("0")),
        amountDue, amountDue - payment.getOrElse(BigDecimal("0")),
        interest, amountDue + interest, payments)
    }
    schedules
  }


  implicit val formAmountsDue = CalculatorAmountsDue(Seq(
    CalculatorAmountDue(BigDecimal("200.00"), 2010, "January", 20),
    CalculatorAmountDue(BigDecimal("100.00"), 2014, "December", 1)
  ))
}
