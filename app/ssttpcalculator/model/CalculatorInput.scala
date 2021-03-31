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

import play.api.libs.json.{Json, JsonValidationError, OFormat, OWrites}

case class CalculatorInput(
    debits:           Seq[DebitInput],
    initialPayment:   BigDecimal,
    startDate:        LocalDate,
    endDate:          LocalDate,
    firstPaymentDate: Option[LocalDate] = None
) {
  //TODO remove this
  override def toString: String = s"CalculatorInput(debits=$debits, initialPayment=$initialPayment, startDate=$startDate, endDate=$endDate, firstPaymentDate=$firstPaymentDate)"
}

object CalculatorInput {

  private val reads = Json.reads[CalculatorInput]
    .filter(JsonValidationError("'debits' was empty, it should have at least one debit."))(_.debits.size > 0)
    .filter(JsonValidationError("The 'initialPayment' can't be less than 0"))(_.initialPayment >= 0)

  private val writes: OWrites[CalculatorInput] = Json.writes[CalculatorInput]

  implicit val format: OFormat[CalculatorInput] = OFormat(reads, writes)

}
