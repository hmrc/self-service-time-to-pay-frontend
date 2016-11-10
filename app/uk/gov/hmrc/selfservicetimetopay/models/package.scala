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

import play.api.libs.json._
import uk.gov.hmrc.selfservicetimetopay.models._

package object modelsFormat {
  implicit val localDateFormatter = new Format[LocalDate] {
    override def reads(json: JsValue): JsResult[LocalDate] =
      json.validate[String].map(LocalDate.parse)

    override def writes(o: LocalDate): JsValue = Json.toJson(o.toString)
  }

  implicit val directDebitInstructionFormatter = Json.format[DirectDebitInstruction]
  implicit val directDebitBanksFormatter = Json.format[DirectDebitBank]
  implicit val directDebitPaymentPlanFormatter = Json.format[DirectDebitPaymentPlan]
  implicit val directDebitInstructionPaymentPlanFormatter = Json.format[DirectDebitInstructionPaymentPlan]

  implicit val calculatorLiabilityFormatter = Json.format[CalculatorLiability]
  implicit val calculatorAmountDueFormatter = Json.format[CalculatorAmountDue]
  implicit val calculatorAmountsDueFormatter = Json.format[CalculatorAmountsDue]
  implicit val calculatorDurationFormatter = Json.format[CalculatorDuration]
  implicit val calculatorPaymentScheduleInstalmentFormatter = Json.format[CalculatorPaymentScheduleInstalment]
  implicit val calculatorPaymentScheduleFormatter = Json.format[CalculatorPaymentSchedule]
  implicit val calculatorPaymentTodayFormatter = Json.format[CalculatorPaymentToday]
  implicit val calculatorInputFormatter = Json.format[CalculatorInput]

  implicit val arrangementDayOfMonthFormatter = Json.format[ArrangementDayOfMonth]
  implicit val arrangementDirectDebitFormatter = Json.format[ArrangementDirectDebit]

  implicit val eligibilityDebtTypeFormatter = Json.format[EligibilityDebtType]
  implicit val eligibilityExistingTTPFormatter = Json.format[EligibilityExistingTTP]

  implicit val interestFormatter = Json.format[Interest]

  implicit val tTPDebitFormatter = Json.format[Debit]
  implicit val addressFormatter = Json.format[Address]

  implicit val tTPCommunicationPreferencesFormatter = Json.format[CommunicationPreferences]
  implicit val tTPSelfAssessmentFormatter = Json.format[SelfAssessment]
  implicit val taxPayerFormatter = Json.format[TaxPayer]
  implicit val tTPArrangementFormatter = Json.format[TTPArrangement]

  implicit val bankDetailsFormatterter = Json.format[BankDetails]
}

package object controllerVariables {
  implicit val paymentSchedules = Seq(
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("250.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("250.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("250.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("100.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("100.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("100.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("100.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("50.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("50.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("50.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("50.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 5, 1), new BigDecimal("50.00"))
      )
    )
  )

  implicit val formAmountsDue = CalculatorAmountsDue(Seq(
    CalculatorAmountDue(new BigDecimal("200.00"), 2010, "January", 20),
    CalculatorAmountDue(new BigDecimal("100.00"), 2014, "December", 1)
  ))
}
