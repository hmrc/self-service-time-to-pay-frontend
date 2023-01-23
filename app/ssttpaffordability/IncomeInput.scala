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

import ssttpaffordability.AffordabilityForm.parseStringToBigDecimal

final case class IncomeInput(
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

object IncomeInput {
  def apply(
      monthlyIncomeStr: String,
      benefitsStr:      String,
      otherIncomeStr:   String
  ): IncomeInput = {
    IncomeInput(
      parseStringToBigDecimal(monthlyIncomeStr),
      parseStringToBigDecimal(benefitsStr),
      parseStringToBigDecimal(otherIncomeStr)
    )
  }

}
