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

case class AuditSpending(
    housing:              BigDecimal,
    pensionContributions: BigDecimal,
    councilTax:           BigDecimal,
    utilities:            BigDecimal,
    debtRepayments:       BigDecimal,
    travel:               BigDecimal,
    childcareCosts:       BigDecimal,
    insurance:            BigDecimal,
    groceries:            BigDecimal,
    health:               BigDecimal,
    totalOutgoings:       BigDecimal
)

object AuditSpending {
  implicit val format: OFormat[AuditSpending] = Json.format[AuditSpending]

  def fromSpending(spending: Option[Spending]): AuditSpending = {
    spending match {
      case Some(spending) =>
        val housing = spending.amount(Expense.HousingExp)
        val pensionContributions = spending.amount(Expense.PensionContributionsExp)
        val councilTax = spending.amount(Expense.CouncilTaxExp)
        val utilities = spending.amount(Expense.UtilitiesExp)
        val debtRepayments = spending.amount(Expense.DebtRepaymentsExp)
        val travel = spending.amount(Expense.TravelExp)
        val childcareCosts = spending.amount(Expense.ChildcareExp)
        val insurance = spending.amount(Expense.InsuranceExp)
        val groceries = spending.amount(Expense.GroceriesExp)
        val health = spending.amount(Expense.HealthExp)
        val totalOutgoings = spending.totalSpending
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

