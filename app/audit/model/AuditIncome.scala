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

package audit.model

import play.api.libs.json.{Json, OFormat}
import ssttpaffordability.model.{Income, IncomeCategory}
import util.CurrencyUtil.formatToCurrencyStringWithTrailingZeros

case class AuditIncome(monthlyIncomeAfterTax: String, benefits: String, otherMonthlyIncome: String, totalIncome: String)

object AuditIncome {
  implicit val format: OFormat[AuditIncome] = Json.format[AuditIncome]

  def fromIncome(income: Option[Income]): AuditIncome = {
    income match {
      case Some(income) =>
        val monthlyIncome = formatToCurrencyStringWithTrailingZeros(income.amount(IncomeCategory.MonthlyIncome))
        val benefits = formatToCurrencyStringWithTrailingZeros(income.amount(IncomeCategory.Benefits))
        val otherIncome = formatToCurrencyStringWithTrailingZeros(income.amount(IncomeCategory.OtherIncome))
        val totalIncome = formatToCurrencyStringWithTrailingZeros(income.totalIncome)
        AuditIncome(monthlyIncome, benefits, otherIncome, totalIncome)
      case None =>
        throw new Exception("Income must be in the Journey at this point")
    }
  }
}

