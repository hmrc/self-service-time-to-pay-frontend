/*
 * Copyright 2018 HM Revenue & Customs
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

import scala.math.BigDecimal.RoundingMode.HALF_UP

case class CalculatorPaymentToday(amount: BigDecimal)

object CalculatorPaymentToday {
  def apply(str: String): CalculatorPaymentToday = {
    str match {
      case s if s.isEmpty => CalculatorPaymentToday(BigDecimal(0))
      case s => CalculatorPaymentToday(BigDecimal(s))
    }
  }

  implicit def bigDecimalToCalculatorPaymentToday(bd: BigDecimal): CalculatorPaymentToday = {
    CalculatorPaymentToday(bd)
  }

  implicit def calculatorPaymentTodayToBigDecimal(cpd: CalculatorPaymentToday): BigDecimal = {
    cpd.amount.setScale(2, HALF_UP)
  }

  def unapply(arg: BigDecimal) = Option(arg.setScale(2, HALF_UP))
}
