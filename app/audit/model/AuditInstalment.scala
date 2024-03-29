/*
 * Copyright 2023 HM Revenue & Customs
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

package audit.model

import play.api.libs.json.{Json, OFormat, Writes}

import java.time.LocalDate

case class AuditInstalment(
    amount:           String,
    instalmentNumber: Int,
    paymentDate:      LocalDate
)

object AuditInstalment {

  val writes: Writes[AuditInstalment] = (o: AuditInstalment) => Json.obj(
    "amount" -> o.amount,
    "instalment" -> o.instalmentNumber,
    "paymentDate" -> o.paymentDate
  )

  implicit val format: OFormat[AuditInstalment] = Json.format[AuditInstalment]
}

