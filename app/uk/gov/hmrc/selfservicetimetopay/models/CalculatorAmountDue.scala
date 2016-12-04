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

case class CalculatorAmountDue(amount: BigDecimal, dueByYear: Int, dueByMonth: Int, dueByDay: Int) {

  def this(amount: BigDecimal, dueBy: LocalDate) {
    this(amount, dueBy.getYear, dueBy.getMonthValue, dueBy.getDayOfMonth)
  }

  def getDueBy: LocalDate = {
    LocalDate.of(dueByYear, dueByMonth, dueByDay)
  }
}
