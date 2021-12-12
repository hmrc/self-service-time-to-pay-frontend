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

import scala.math.BigDecimal

final case class TaxLiability(
    amount:  BigDecimal,
    dueDate: LocalDate
)

object TaxLiability {
  implicit val format: OFormat[TaxLiability] = Json.format[TaxLiability]

  def amortizedLiabilities(liabilities: Seq[TaxLiability], payment: BigDecimal): Seq[TaxLiability] = {
    val result = liabilities.foldLeft((payment, Seq.empty[TaxLiability])){
      case ((p, s), lt) if p <= 0         => (p, s :+ lt)

      case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)

      case ((p, s), lt)                   => (0, s :+ lt.copy(amount = lt.amount - p))
    }
    result._2
  }

  def latePayments(date: LocalDate)(ls: Seq[TaxLiability], repayment: BigDecimal) = ls.foldLeft((repayment, List.empty[LatePayment])){
    case ((p, l), lt) if p <= 0 || !lt.dueDate.isBefore(date) => (p, l)
    case ((p, l), lt) if lt.amount >= p                       => (0, LatePayment(lt.dueDate, date, p) :: l)
    case ((p, l), lt)                                         => (p - lt.amount, LatePayment(lt.dueDate, date, lt.amount) :: l)
  }._2
}
