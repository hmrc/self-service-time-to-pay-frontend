/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

//Direct-debit - input to createPaymentPlan
final case class PaymentPlanRequest(requestingService:      String,
                                    submissionDateTime:     String,
                                    knownFact:              List[KnownFact],
                                    directDebitInstruction: DirectDebitInstruction,
                                    paymentPlan:            PaymentPlan,
                                    printFlag:              Boolean) {

  def obfuscate: PaymentPlanRequest = copy(
    knownFact              = knownFact.map(_.obfuscate),
    directDebitInstruction = directDebitInstruction.obfuscate,
    paymentPlan            = paymentPlan.obfuscate
  )
}

object PaymentPlanRequest {
  implicit val paymentPlanRequestFormatter: Format[PaymentPlanRequest] = Json.format[PaymentPlanRequest]
}

final case class KnownFact(
    service: String,
    value:   String
) {

  def obfuscate: KnownFact = copy(
    value = "***" //utr
  )
}

object KnownFact {
  implicit val format: Format[KnownFact] = Json.format[KnownFact]
}

final case class PaymentPlan(
    ppType:                    String,
    paymentReference:          String,
    hodService:                String,
    paymentCurrency:           String,
    initialPaymentAmount:      Option[String],
    initialPaymentStartDate:   Option[LocalDate],
    scheduledPaymentAmount:    String,
    scheduledPaymentStartDate: LocalDate,
    scheduledPaymentEndDate:   LocalDate,
    scheduledPaymentFrequency: String,
    balancingPaymentAmount:    String,
    balancingPaymentDate:      LocalDate,
    totalLiability:            String
) {

  def obfuscate: PaymentPlan = copy(
    paymentReference = "***" //utr
  )
}

object PaymentPlan {
  implicit val format: Format[PaymentPlan] = Json.format[PaymentPlan]
}
