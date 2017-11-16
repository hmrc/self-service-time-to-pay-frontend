/*
 * Copyright 2017 HM Revenue & Customs
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

case class CalculatorInput(debits:Seq[Debit] = Seq.empty,
                           initialPayment:BigDecimal = BigDecimal(0),
                           startDate:LocalDate,
                           endDate:LocalDate,
                           firstPaymentDate:Option[LocalDate] = None,
                           paymentFrequency:String = "MONTHLY")

object CalculatorInput {
  val initial = CalculatorDef(2)
}

object CalculatorDef {
  def apply(relativeEndDate: Int): CalculatorInput = CalculatorInput(startDate = LocalDate.now(),
    endDate = LocalDate.now().plusMonths(relativeEndDate).minusDays(1))

  def apply(startDate: LocalDate, relativeEndDate: Int): CalculatorInput = CalculatorInput(startDate = startDate,
    endDate = startDate.plusMonths(relativeEndDate).minusDays(1))
}
