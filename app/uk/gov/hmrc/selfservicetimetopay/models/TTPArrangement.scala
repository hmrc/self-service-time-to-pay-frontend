/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import timetopaycalculator.cor.model.PaymentSchedule
import timetopaytaxpayer.cor.model.TaxpayerDetails

final case class TTPArrangement(paymentPlanReference: String,
                                directDebitReference: String,
                                taxpayerDetails:             TaxpayerDetails,
                                schedule:             PaymentSchedule) {

  def obfuscate = TTPArrangement(
    paymentPlanReference = paymentPlanReference,
    directDebitReference = directDebitReference,
    taxpayerDetails             = taxpayerDetails .obfuscate,
    schedule             = schedule
  )
}

object TTPArrangement {
  implicit val formats: OFormat[TTPArrangement] = Json.format[TTPArrangement]
}
