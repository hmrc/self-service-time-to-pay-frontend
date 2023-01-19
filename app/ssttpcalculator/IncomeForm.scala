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

package ssttpcalculator

import scala.math.BigDecimal.RoundingMode.HALF_UP

final case class IncomeForm(
    monthlyIncome: BigDecimal,
    benefits:      BigDecimal,
    otherIncome:   BigDecimal
) {
  def hasPositiveTotal: Boolean = totalIncome > 0

  private val allIncome: Seq[BigDecimal] = Seq(
    monthlyIncome,
    benefits,
    otherIncome
  )

  private val totalIncome: BigDecimal = allIncome.sum
}

object IncomeForm {
  def apply(
      monthlyIncomeStr: String,
      benefitsStr:      String,
      otherIncomeStr:   String
  ): IncomeForm = {
    val monthlyIncomeBigDecimal = monthlyIncomeStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val benefitsBigDecimal = benefitsStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    val otherIncomeBigDecimal = otherIncomeStr match {
      case s if s.isEmpty => BigDecimal(0)
      case s              => BigDecimal(s)
    }
    IncomeForm(monthlyIncomeBigDecimal, benefitsBigDecimal, otherIncomeBigDecimal)
  }

  implicit def bigDecimalsToIncome(
      monthlyIncome: BigDecimal,
      benefits:      BigDecimal,
      otherIncome:   BigDecimal): IncomeForm = {
    IncomeForm(monthlyIncome, benefits, otherIncome)
  }

  implicit def incomeToBigDecimal(inf: IncomeForm): (BigDecimal, BigDecimal, BigDecimal) = {
    (
      inf.monthlyIncome.setScale(2, HALF_UP),
      inf.benefits.setScale(2, HALF_UP),
      inf.otherIncome.setScale(2, HALF_UP)
    )
  }

  def unapply(arg: BigDecimal): Option[BigDecimal] = Option(arg.setScale(2, HALF_UP))
}
