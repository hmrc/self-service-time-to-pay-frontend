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

package pagespecs.legacycalculator

import langswitch.Languages.{English, Welsh}
import model.enumsforforms.{IsSoleSignatory, TypesOfBankAccount}
import ssttpcalculator.CalculatorType.Legacy
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.DirectDebitTd
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class ViewPaymentPlanPageLegacyCalculatorSpec extends ItSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
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
    howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed
    howMuchCanYouPayEachMonthPageLegacyCalculator.selectASpecificOption(PaymentPlanOption.Basic)
    howMuchCanYouPayEachMonthPageLegacyCalculator.clickContinue()

    checkYourPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed()
    checkYourPaymentPlanPageLegacyCalculator.clickContinue()

    aboutBankAccountPage.assertInitialPageIsDisplayed()
    aboutBankAccountPage.selectTypeOfAccountRadioButton(TypesOfBankAccount.Personal)
    aboutBankAccountPage.selectIsAccountHolderRadioButton(IsSoleSignatory.Yes)
    aboutBankAccountPage.clickContinue()

    directDebitPage.assertInitialPageIsDisplayed()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksIsSuccessful()
    directDebitPage.clickContinue()

    directDebitConfirmationPage.assertInitialPageIsDisplayed()
    directDebitConfirmationPage.clickContinue()
  }

  "language English" in {
    beginJourney()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.clickLink()
    viewPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed(English)
  }

  "language Welsh" in {
    beginJourney()
    termsAndConditionsPage.clickOnWelshLink()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.clickLink()
    viewPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed(Welsh)
  }

  "back link goes to previous page" in {
    beginJourney()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.clickLink()
    viewPaymentPlanPageLegacyCalculator.goBack()
    arrangementSummaryPage.assertInitialPageIsDisplayed
  }
}
