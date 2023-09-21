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
import ssttpaffordability.model.{Expense, Spending}
import util.CurrencyUtil.formatToCurrencyStringWithTrailingZeros

case class AuditSpending(
    housing:              String,
    pensionContributions: String,
    councilTax:           String,
    utilities:            String,
    debtRepayments:       String,
    travel:               String,
    childcareCosts:       String,
    insurance:            String,
    groceries:            String,
    health:               String,
    totalOutgoings:       String
)

object AuditSpending {
  implicit val format: OFormat[AuditSpending] = Json.format[AuditSpending]

  def fromSpending(spending: Option[Spending]): AuditSpending = {
    spending match {
      case Some(spending) =>
        val housing = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.HousingExp))
        val pensionContributions = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.PensionContributionsExp))
        val councilTax = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.CouncilTaxExp))
        val utilities = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.UtilitiesExp))
        val debtRepayments = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.DebtRepaymentsExp))
        val travel = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.TravelExp))
        val childcareCosts = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.ChildcareExp))
        val insurance = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.InsuranceExp))
        val groceries = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.GroceriesExp))
        val health = formatToCurrencyStringWithTrailingZeros(spending.amount(Expense.HealthExp))
        val totalOutgoings = formatToCurrencyStringWithTrailingZeros(spending.totalSpending)
        AuditSpending(
          housing,
          pensionContributions,
          councilTax,
          utilities,
          debtRepayments,
          travel,
          childcareCosts,
          insurance,
          groceries,
          health,
          totalOutgoings
        )
      case None =>
        throw new Exception("Spending must be in the Journey at this point")
    }
  }
}

