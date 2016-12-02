/*
 * Copyright 2016 HM Revenue & Customs
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

import com.fasterxml.jackson.annotation.JsonIgnore
import play.api.libs.json.Json

object CalculatorAmountDue {
  implicit val format = Json.format[CalculatorAmountDue]
  def apply(amt: BigDecimal,
            dueByYear: Int,
            dueByMonth: String,
            dueByDay: Int): CalculatorAmountDue = new CalculatorAmountDue(amt, dueByYear, dueByMonth, dueByDay)

  def unapply(arg: CalculatorAmountDue): Option[(BigDecimal, Int, String, Int)] = Some((arg.amt, arg.dueByYear, arg.dueByMonth, arg.dueByDay))
}

class CalculatorAmountDue(val amt: BigDecimal,
                               val dueByYear: Int,
                               val dueByMonth: String,
                               val dueByDay: Int) extends Debit(originCode = None,
                                                            amount = Some(amt),
                                                            dueDate = LocalDate.of(dueByYear, DateTimeFormatter.ofPattern("MMMM").parse(dueByMonth).get(ChronoField.MONTH_OF_YEAR), dueByDay),
                                                            interest = None,
                                                            taxYearEnd = None) {
  def this(amount: BigDecimal, dueBy: LocalDate) {
    this(amount, dueBy.getYear, dueBy.getMonthValue, dueBy.getDayOfMonth)
  }

  @JsonIgnore
  def getDueBy = dueDate
}