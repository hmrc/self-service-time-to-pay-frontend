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

package pagespecs

import langswitch.Languages.{English, Welsh}
import model.enumsforforms.{IsSoleSignatory, TypesOfBankAccount}
import ssttpcalculator.CalculatorType.PaymentOptimised
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending
import testsupport.testdata.DirectDebitTd

class ArrangementSummaryPageSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> PaymentOptimised.value
  )

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    TaxpayerStub.getTaxpayer()
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement

    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertInitialPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertInitialPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.assertInitialPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    startAffordabilityPage.assertInitialPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertInitialPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.assertInitialPageIsDisplayed
    yourMonthlyIncomePage.enterMonthlyIncome(remainingIncomeAfterSpending.toString)
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertInitialPageIsDisplayed
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    howMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    howMuchCanYouPayEachMonthPage.clickContinue()

    checkYourPaymentPlanPage.assertInitialPageIsDisplayed()
    checkYourPaymentPlanPage.clickContinue()

    aboutBankAccountPage.assertInitialPageIsDisplayed()
    aboutBankAccountPage.selectTypeOfAccountRadioButton(TypesOfBankAccount.Personal)
    aboutBankAccountPage.selectIsAccountHolderRadioButton(IsSoleSignatory.Yes)
    aboutBankAccountPage.clickContinue()

    directDebitPage.assertInitialPageIsDisplayed()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()

    directDebitConfirmationPage.assertInitialPageIsDisplayed()
    directDebitConfirmationPage.clickContinue()
    termsAndConditionsPage.assertInitialPageIsDisplayed()
  }

  "redirect user to summary page if user returns to calculator" in {
    beginJourney()
    termsAndConditionsPage.clickContinue()
    taxLiabilitiesPage.open()
    arrangementSummaryPage.assertInitialPageIsDisplayed()
  }

  "language English" in {
    beginJourney()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.assertInitialPageIsDisplayed(English)
  }

  "language Welsh" in {
    beginJourney()
    termsAndConditionsPage.clickOnWelshLink()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.assertInitialPageIsDisplayed(Welsh)
  }
}
