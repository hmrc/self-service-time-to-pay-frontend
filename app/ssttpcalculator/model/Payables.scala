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

case class Payables(liabilities: Seq[Payable]) {
  def balance: BigDecimal = liabilities.map(_.amount).sum

  // TODO [OPS-9610] ensure payables are ordered by their due dates, earliest first, otherwise this method won't work
  def inDate(date: LocalDate): Boolean = liabilities.headOption match {
    case Some(TaxLiability(_, dueDate)) => dueDate.isAfter(date)
    case Some(LatePaymentInterest(_))   => true
    case None                           => true
  }
}

object Payables {
  def latePayments(paymentAmount: BigDecimal, paymentDate: LocalDate, payables: Payables): List[LatePayment] = {
    payables.liabilities.foldLeft((paymentAmount, List.empty[LatePayment])) {
      case ((p, l), LatePaymentInterest(_))                                     => (p, l)
      case ((p, l), lt) if lt.amount <= 0 || !lt.hasInterestCharge(paymentDate) => (p, l)
      case ((p, l), TaxLiability(a, d)) if a >= p                               => (0, LatePayment(d, Payment(paymentDate, p)) :: l)
      case ((p, l), TaxLiability(a, d))                                         => (p - a, LatePayment(d, Payment(paymentDate, a)) :: l)
    }._2
  }

}
