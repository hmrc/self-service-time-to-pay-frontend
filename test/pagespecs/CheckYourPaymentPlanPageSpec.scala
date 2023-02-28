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
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class CheckYourPaymentPlanPageSpec extends ItSpec {

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.assertPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.assertPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickOnTempButton()

    startAffordabilityPage.assertPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.assertPageIsDisplayed
    yourMonthlyIncomePage.enterMonthlyIncome(remainingIncomeAfterSpending.toString)
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertPageIsDisplayed
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed
    calculatorInstalmentsPage28thDay.selectASpecificOption("50")
    calculatorInstalmentsPage28thDay.clickContinue()

    checkYourPaymentPlanPage.assertPageIsDisplayed()
  }

  "language" in {
    beginJourney()

    checkYourPaymentPlanPage.clickOnWelshLink()
    checkYourPaymentPlanPage.assertPageIsDisplayed(Welsh)

    checkYourPaymentPlanPage.clickOnEnglishLink()

    checkYourPaymentPlanPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    checkYourPaymentPlanPage.backButtonHref shouldBe Some(s"${baseUrl.value}${calculatorInstalmentsPage28thDay.path}")
  }

  "change monthly instalments" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeMonthlyAmountLink()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
  }

  "change collection day" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeCollectionDayLink()
    selectDatePage.assertPageIsDisplayed
  }

  "change upfront payment amount" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeUpfrontPaymentAnswerLink()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }

  "change upfront answer" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeUpfrontPaymentAmountLink()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    checkYourPaymentPlanPage.clickContinue()
    aboutBankAccountPage.assertPageIsDisplayed
  }

  "shows warning" in {
    beginJourney()
    checkYourPaymentPlanPage.assertPageIsDisplayed
    checkYourPaymentPlanPage.clickChangeMonthlyAmountLink()
    calculatorInstalmentsPage28thDay.selectASpecificOption("60")
    calculatorInstalmentsPage28thDay.clickContinue()
    checkYourPaymentPlanPage.assertWarningIsDisplayed(English)
    checkYourPaymentPlanPage.clickOnWelshLink()
    checkYourPaymentPlanPage.assertWarningIsDisplayed(Welsh)
  }
}
