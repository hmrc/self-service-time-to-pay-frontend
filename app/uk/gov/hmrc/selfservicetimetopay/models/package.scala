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

import play.api.libs.json.{Format, JsResult, JsValue, Json}
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorPaymentScheduleInstalment, _}

package object modelsFormat {
  implicit val localDateFormat = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      json.validate[String].map(LocalDate.parse)

    override def writes(o: LocalDate): JsValue = Json.toJson(o.toString)
  }

  implicit val directDebitInstructionFormat = Json.format[DirectDebitInstruction]
  implicit val directDebitBanksFormat = Json.format[DirectDebitBank]
  implicit val directDebitPaymentPlanFormat = Json.format[DirectDebitPaymentPlan]
  implicit val directDebitInstructionPaymentPlanFormat = Json.format[DirectDebitInstructionPaymentPlan]

  implicit val calculatorLiabilityFormat = Json.format[CalculatorLiability]
  implicit val calculatorAmountDueFormat = Json.format[CalculatorAmountDue]
  implicit val calculatorAmountsDueFormat = Json.format[CalculatorAmountsDue]
  implicit val calculatorDurationFormat = Json.format[CalculatorDuration]
  implicit val calculatorPaymentScheduleInstalmentFormat = Json.format[CalculatorPaymentScheduleInstalment]
  implicit val calculatorPaymentScheduleFormat = Json.format[CalculatorPaymentSchedule]
  implicit val calculatorPaymentTodayFormat = Json.format[CalculatorPaymentToday]
  implicit val calculatorInputFormat = Json.format[CalculatorInput]

  implicit val arrangementDayOfMonthFormat = Json.format[ArrangementDayOfMonth]
  implicit val arrangementDirectDebitFormat = Json.format[ArrangementDirectDebit]

  implicit val eligibilityDebtTypeFormat = Json.format[EligibilityDebtType]
  implicit val eligibilityExistingTTPFormat = Json.format[EligibilityExistingTTP]

  implicit val tTPDebitFormat = Json.format[Debit]
  implicit val tTPAddressFormat = Json.format[Address]
  implicit val tTPCommunicationPreferencesFormat = Json.format[CommunicationPreferences]
  implicit val tTPSelfAssessmentFormat = Json.format[SelfAssessment]
  implicit val tTPTaxPayerFormat = Json.format[TTPTaxPayer]
  implicit val tTPArrangementFormat = Json.format[TTPArrangement]
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
      schedules = schedules :+ CalculatorPaymentSchedule(payment.getOrElse(BigDecimal("0")),
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
