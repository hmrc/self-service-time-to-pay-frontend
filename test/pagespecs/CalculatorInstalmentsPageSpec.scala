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
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, remainingIncomeAfterSpendingTooSmallForPlan}

class CalculatorInstalmentsPageSpec extends ItSpec {

  // TODO OPS-8650: remove this entirely
  def beginOldJourney(): Unit = {
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
    selectDatePage.clickContinue()

    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
  }

  def beginNewJourney(remainingIncomeAfterSpending: Int = defaultRemainingIncomeAfterSpending): Unit = {
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
    yourMonthlySpendingPage.enterHousing("500")
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
  }

  "goes to kick out page " +
    "if 50% of remaining income after spending cannot cover amount remaining to pay including interest in 24 months of less" in {
      beginNewJourney(remainingIncomeAfterSpendingTooSmallForPlan)
      weCannotAgreeYourPaymentPlanPage.assertPagePathCorrect
    }

  "display default options" - {
    "if 50% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% default option" in {
      }
    "if 60% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% and 60% default options" in {
      }
    "displays three options otherwise" in {
      beginNewJourney()

      calculatorInstalmentsPage28thDay.assertPageIsDisplayed
    }
  }

  "language" in {
    beginNewJourney()

    calculatorInstalmentsPage28thDay.clickOnWelshLink()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed(Welsh)

    calculatorInstalmentsPage28thDay.clickOnEnglishLink()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginNewJourney()
    calculatorInstalmentsPage28thDay.backButtonHref shouldBe Some(s"${baseUrl.value}${howMuchYouCouldAffordPage.path}")
  }

  "select an option and continue" in {
    beginNewJourney()
    calculatorInstalmentsPage28thDay.selectAnOption()
    calculatorInstalmentsPage28thDay.clickContinue()
    instalmentSummaryPage.assertPageIsDisplayed()
  }
}
