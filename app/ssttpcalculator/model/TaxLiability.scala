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

package ssttpcalculator.model

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}

final case class TaxLiability(
    amount:  BigDecimal,
    dueDate: LocalDate
) {
  def amortize(payment: BigDecimal): (TaxLiability, BigDecimal) = payment match {
    case p if p <= 0      => (this, payment)
    case p if p >= amount => (this.copy(amount = 0), p - amount)
    case p if p < amount  => (this.copy(amount - p), 0)

  }
}

object TaxLiability {
  implicit val format: OFormat[TaxLiability] = Json.format[TaxLiability]
}
