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
import ssttpcalculator.CalculatorType.PaymentOptimised
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class CheckYourPaymentPlanPageSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> PaymentOptimised.value
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
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    howMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    howMuchCanYouPayEachMonthPage.clickContinue()

    checkYourPaymentPlanPage.assertInitialPageIsDisplayed()
  }

  "language" in {
    beginJourney()

    checkYourPaymentPlanPage.clickOnWelshLink()
    checkYourPaymentPlanPage.assertInitialPageIsDisplayed(Welsh)

    checkYourPaymentPlanPage.clickOnEnglishLink()

    checkYourPaymentPlanPage.assertInitialPageIsDisplayed(English)
  }

  "change monthly instalments" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeMonthlyAmountLink()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed()
  }

  "change collection day" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeCollectionDayLink()
    selectDatePage.assertInitialPageIsDisplayed
  }

  "change upfront payment amount" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeUpfrontPaymentAnswerLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "change upfront answer" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeUpfrontPaymentAmountLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    checkYourPaymentPlanPage.clickContinue()
    aboutBankAccountPage.assertInitialPageIsDisplayed
  }

  "shows warning" in {
    beginJourney()
    checkYourPaymentPlanPage.assertInitialPageIsDisplayed
    checkYourPaymentPlanPage.clickChangeMonthlyAmountLink()
    howMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Higher)
    howMuchCanYouPayEachMonthPage.clickContinue()
    checkYourPaymentPlanPage.assertWarningIsDisplayed(English)
    checkYourPaymentPlanPage.clickOnWelshLink()
    checkYourPaymentPlanPage.assertWarningIsDisplayed(Welsh)
  }

  "back link goes to previous page" in {
    beginJourney()
    checkYourPaymentPlanPage.assertInitialPageIsDisplayed
    checkYourPaymentPlanPage.goBack()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
  }
}
