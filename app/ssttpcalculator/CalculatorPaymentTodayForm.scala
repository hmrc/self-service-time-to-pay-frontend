/*
 * Copyright 2019 HM Revenue & Customs
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

case class CalculatorPaymentTodayForm(amount: BigDecimal)

object CalculatorPaymentTodayForm {
  def apply(str: String): CalculatorPaymentTodayForm = {
    str match {
      case s if s.isEmpty => CalculatorPaymentTodayForm(BigDecimal(0))
      case s              => CalculatorPaymentTodayForm(BigDecimal(s))
    }
  }

  implicit def bigDecimalToCalculatorPaymentToday(bd: BigDecimal): CalculatorPaymentTodayForm = {
    CalculatorPaymentTodayForm(bd)
  }

  implicit def calculatorPaymentTodayToBigDecimal(cpd: CalculatorPaymentTodayForm): BigDecimal = {
    cpd.amount.setScale(2, HALF_UP)
  }

  def unapply(arg: BigDecimal) = Option(arg.setScale(2, HALF_UP))
}
