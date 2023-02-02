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

package ssttpcalculator.model

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}

import scala.math.BigDecimal

abstrate trait Payable {
  def amount: BigDecimal
}

final case class InterestLiability(segments: Seq[Interest]) {
  def amount: BigDecimal = segments.map(_.amountAccrued).sum
}

final case class TaxLiability(
    amount:  BigDecimal,
    dueDate: LocalDate
) extends Payable {
  def hasInterestCharge(payment: Payment): Boolean = hasInterestCharge(payment.date)

  def hasInterestCharge(paymentDate: LocalDate): Boolean = dueDate.isBefore(paymentDate)
}

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

  def latePayments(payment: Payment)(ls: Seq[TaxLiability]) = ls.foldLeft((payment, List.empty[LatePayment])){
    case ((p, l), lt) if p.amount <= 0 || !lt.hasInterestCharge(payment) => (p, l)
    case ((p, l), lt) if lt.amount >= p.amount => (p.copy(amount = 0), LatePayment(lt.dueDate, p) :: l)
    case ((p, l), lt) => (p.copy(amount = p.amount - lt.amount), LatePayment(lt.dueDate, p.copy(amount = lt.amount)) :: l)
  }._2
}
