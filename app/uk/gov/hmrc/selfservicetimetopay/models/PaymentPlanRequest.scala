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

package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

//Direct-debit - input to createPaymentPlan
case class PaymentPlanRequest(requestingService: String,
                              submissionDateTime: String,
                              knownFact: List[KnownFact],
                              directDebitInstruction: DirectDebitInstruction,
                              paymentPlan: PaymentPlan,
                              printFlag: Boolean)

case class KnownFact(service: String, value: String)

case class PaymentPlan(ppType: String,
                       paymentReference: String,
                       hodService: String,
                       paymentCurrency: String,
                       initialPaymentAmount: Option[String],
                       initialPaymentStartDate: Option[LocalDate],
                       scheduledPaymentAmount: String,
                       scheduledPaymentStartDate: LocalDate,
                       scheduledPaymentEndDate: LocalDate,
                       scheduledPaymentFrequency: String,
                       balancingPaymentAmount: String,
                       balancingPaymentDate: LocalDate,
                       totalLiability: String)
