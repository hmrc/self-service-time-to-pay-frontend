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

package ssttpaffordability

import ssttpcalculator.MonthlyIncomeForm

import scala.math.BigDecimal.RoundingMode.HALF_UP

final case class MonthlySpendingForm(monthlySpending: BigDecimal)

object MonthlySpendingForm {
  def apply(str: String): MonthlySpendingForm = {
    str match {
      case s if s.isEmpty => MonthlySpendingForm(BigDecimal(0))
      case s              => MonthlySpendingForm(BigDecimal(s))
    }
  }

  implicit def digDecimalToMonthlySpending(bd: BigDecimal): MonthlyIncomeForm = {
    MonthlyIncomeForm(bd)
  }

  implicit def monthlySpendingToBigDecimal(mif: MonthlyIncomeForm): BigDecimal = {
    mif.monthlyIncome.setScale(2, HALF_UP)
  }

  def unapply(arg: BigDecimal): Option[BigDecimal] = Option(arg.setScale(2, HALF_UP))
}
