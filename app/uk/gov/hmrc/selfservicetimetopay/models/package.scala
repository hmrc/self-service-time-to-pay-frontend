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

import java.math.BigDecimal
import java.time.LocalDate

import play.api.libs.json.{Format, JsResult, JsValue, Json}
import uk.gov.hmrc.selfservicetimetopay.models._

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
  implicit val paymentSchedules = Seq(
    CalculatorPaymentSchedule(LocalDate.of(2016, 1, 1), LocalDate.of(2016, 9, 1),
      new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(LocalDate.of(2015, 9, 1), LocalDate.of(2016, 3, 1),
      new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(LocalDate.of(2016, 4, 1), LocalDate.of(2016, 7, 1),
      new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(LocalDate.of(2016, 7, 1), LocalDate.of(2016, 12, 1),
      new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 5, 1), new BigDecimal("300.00"))
      )
    )
  )

  implicit val formAmountsDue = CalculatorAmountsDue(Seq(
    CalculatorAmountDue(new BigDecimal("200.00"), 2010, "January", 20),
    CalculatorAmountDue(new BigDecimal("100.00"), 2014, "December", 1)
  ))
}