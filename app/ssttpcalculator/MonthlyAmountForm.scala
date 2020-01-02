/*
 * Copyright 2020 HM Revenue & Customs
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

package ssttpcalculator

import scala.math.BigDecimal.RoundingMode.HALF_UP

case class MonthlyAmountForm(amount: BigDecimal)

object MonthlyAmountForm {
  def apply(str: String): MonthlyAmountForm = {
    str match {
      case s if s.isEmpty => MonthlyAmountForm(BigDecimal(0))
      case s              => MonthlyAmountForm(BigDecimal(s))
    }
  }

  implicit def bigDecimalToMonthlyAmount(bd: BigDecimal): MonthlyAmountForm = {
    MonthlyAmountForm(bd)
  }

  implicit def monthlyAmountToBigDecimal(maf: MonthlyAmountForm): BigDecimal = {
    maf.amount.setScale(2, HALF_UP)
  }

  def unapply(arg: BigDecimal) = Option(arg.setScale(2, HALF_UP))
}

