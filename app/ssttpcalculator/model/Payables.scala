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

case class Payables(liabilities: Seq[Payable]) {
  def balanceToPay: BigDecimal = liabilities.map(_.amount).sum
}

object Payables {
  def latePayments(payment: Payment, payables: Payables): List[LatePayment] = {
    payables.liabilities.foldLeft((payment, List.empty[LatePayment])) {
      case ((p, l), InterestLiability(_)) => (p, l)
      case ((p, l), lt) if lt.amount <= 0 || !lt.hasInterestCharge(payment) => (p, l)
      case ((p, l), TaxLiability(a, d)) if a >= p.amount => (p.copy(amount = 0), LatePayment(d, p) :: l)
      case ((p, l), TaxLiability(a, d)) => (p.copy(amount = p.amount - a), LatePayment(d, p.copy(amount = a)) :: l)
    }._2
  }

}
