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

package testsupport.testdata

import journey.{Journey, JourneyId, PaymentToday, PaymentTodayAmount}
import journey.Statuses.InProgress
import model.enumsforforms.TypesOfBankAccount.Personal
import model.enumsforforms.TypesOfBankAccount
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, PlanSelection, SelectedPlan, TypeOfAccountDetails}

import java.time.LocalDateTime

object TestJourney {
  def createJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id                             = journeyId,
      status                          = InProgress,
      createdOn                       = LocalDateTime.now(),
      maybeTypeOfAccountDetails       = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails                = Some(BankDetails(Some(Personal), "111111", "12345678", "Darth Vader", None)),
     maybeTaxpayer                   = Some(TdAll.taxpayer),
      maybePaymentToday               = Some(PaymentToday(true)),
      maybePaymentTodayAmount         = Some(PaymentTodayAmount(200)),
      maybeIncome                     = Some(Income(IncomeBudgetLine(MonthlyIncome, 2000))),
      maybeSpending                   = Some(Spending(Expenses(HousingExp, 500))),
      maybePlanSelection              = Some(PlanSelection(Left(SelectedPlan(470)))),
      maybePaymentDayOfMonth          = Some(PaymentDayOfMonth(3)),
      maybeEligibilityStatus          = Some(EligibilityStatus(Seq.empty)),
      maybeDateFirstPaymentCanBeTaken = Some(TdAll.dateFirstPaymentCanBeTaken)
    )
  }

  def createJourneyNddsRejects(journeyId: JourneyId): Journey = {
    Journey(
      _id                             = journeyId,
      status                          = InProgress,
      createdOn                       = LocalDateTime.now(),
      maybeTypeOfAccountDetails       = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails                = Some(BankDetails(Some(Personal), "111111", "12345678", "Darth Vader", None)),
      maybeTaxpayer                   = Some(TdAll.taxpayerNddsRejects),
      maybePaymentToday               = Some(PaymentToday(false)),
      maybePaymentTodayAmount         = None,
      maybeIncome                     = Some(Income(IncomeBudgetLine(MonthlyIncome, 2000))),
      maybeSpending                   = Some(Spending(Expenses(HousingExp, 1700))),
      maybePlanSelection              = Some(PlanSelection(Left(SelectedPlan(150)))),
      maybePaymentDayOfMonth          = Some(PaymentDayOfMonth(20)),
      maybeEligibilityStatus          = Some(EligibilityStatus(Seq.empty)),
      maybeDateFirstPaymentCanBeTaken = Some(TdAll.dateFirstPaymentCanBeTaken)
    )
  }
}
