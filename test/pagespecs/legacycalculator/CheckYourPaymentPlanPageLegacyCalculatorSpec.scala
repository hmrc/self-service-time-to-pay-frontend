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
import ssttpcalculator.CalculatorType.Legacy
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class CheckYourPaymentPlanPageLegacyCalculatorSpec extends ItSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

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
  }

  "language" in {
    beginJourney()

    checkYourPaymentPlanPageLegacyCalculator.clickOnWelshLink()
    checkYourPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed(Welsh)

    checkYourPaymentPlanPage.clickOnEnglishLink()

    checkYourPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed(English)
  }

  "change monthly instalments" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.clickChangeMonthlyAmountLink()
    howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed()
  }

  "change collection day" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.clickChangeCollectionDayLink()
    selectDatePage.assertInitialPageIsDisplayed
  }

  "change upfront payment amount" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.clickChangeUpfrontPaymentAnswerLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "change upfront answer" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.clickChangeUpfrontPaymentAmountLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.clickContinue()
    aboutBankAccountPage.assertInitialPageIsDisplayed
  }

  "shows warning" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed
    checkYourPaymentPlanPageLegacyCalculator.clickChangeMonthlyAmountLink()
    howMuchCanYouPayEachMonthPageLegacyCalculator.selectASpecificOption(PaymentPlanOption.Higher)
    howMuchCanYouPayEachMonthPageLegacyCalculator.clickContinue()
    checkYourPaymentPlanPageLegacyCalculator.assertWarningIsDisplayed(English)
    checkYourPaymentPlanPageLegacyCalculator.clickOnWelshLink()
    checkYourPaymentPlanPageLegacyCalculator.assertWarningIsDisplayed(Welsh)
  }

  "back link goes to previous page" in {
    beginJourney()
    checkYourPaymentPlanPageLegacyCalculator.assertInitialPageIsDisplayed
    checkYourPaymentPlanPageLegacyCalculator.clickOnBackButton()
    howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed
  }
}
