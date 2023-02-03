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

  def payOff(paymentAmount: BigDecimal): Payables = {
    val result = liabilities.foldLeft((paymentAmount, Seq.empty[Payable])) {
      case ((payment, newSeqBuilder), liability) if payment <= 0 =>
        (payment, newSeqBuilder :+ liability)

      case ((payment, newSeqBuilder), liability) if payment >= liability.amount =>
        (payment - liability.amount, newSeqBuilder)

      case ((payment, newSeqBuilder), TaxLiability(amount, dueDate)) =>
        (0, newSeqBuilder :+ TaxLiability(amount - payment, dueDate))

      case ((payment, newSeqBuilder), InterestLiability(amount)) =>
        (0, newSeqBuilder :+ InterestLiability(amount - payment))
    }
    Payables(liabilities = result._2)
  }
}
