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
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, netIncomeLargeEnoughForSingleDefaultPlan, netIncomeLargeEnoughForTwoDefaultPlans, netIncomeTooSmallForPlan}

class HowMuchCanYouPayEachMonthPageLegacyCalculatorSpec extends ItSpec {

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
  }

  "goes to kick out page " +
    "if 50% of remaining income after spending cannot cover amount remaining to pay including interest in 24 months of less" in {
      beginJourney(netIncomeTooSmallForPlan)
      weCannotAgreeYourPaymentPlanPage.assertPagePathCorrect
    }

  "display default options" - {
    "if 50% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% default option" in {
        beginJourney(netIncomeLargeEnoughForSingleDefaultPlan)

        howMuchCanYouPayEachMonthPage.optionIsDisplayed("4,914.40")
        howMuchCanYouPayEachMonthPage.optionIsNotDisplayed("6,250")
        howMuchCanYouPayEachMonthPage.optionIsNotDisplayed("7,500")
        howMuchCanYouPayEachMonthPage.optionIsNotDisplayed("10,000")
      }
    "if 60% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% and 60% default options" in {
        beginJourney(netIncomeLargeEnoughForTwoDefaultPlans)

        howMuchCanYouPayEachMonthPage.optionIsDisplayed("4,750")
        howMuchCanYouPayEachMonthPage.optionIsDisplayed("4,914.40", Some("1"), Some("14.40"))
        howMuchCanYouPayEachMonthPage.optionIsNotDisplayed("5,700")
        howMuchCanYouPayEachMonthPage.optionIsNotDisplayed("7,600")
      }
    "displays three default options otherwise" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    }
  }
  "does not display a custom amount option" - {
    "in English" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.customAmountOptionNotDisplayed
    }
    "in Welsh" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.clickOnWelshLink()
      howMuchCanYouPayEachMonthPage.customAmountOptionNotDisplayed(Welsh)
    }
  }

  "language" in {
    beginJourney()

    howMuchCanYouPayEachMonthPage.clickOnWelshLink()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed(Welsh)

    howMuchCanYouPayEachMonthPage.clickOnEnglishLink()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed(English)
  }

  "select an option and continue" - {
    "basic case" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.selectAnOption()
      howMuchCanYouPayEachMonthPage.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }
    "case with large number of decimal places of plan selection amounts" in {
      beginJourney(netIncomeLargeEnoughForSingleDefaultPlan)

      howMuchCanYouPayEachMonthPage.selectAnOption()
      howMuchCanYouPayEachMonthPage.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }

  }

  "returning to the page" - {
    "selecting a default option, continue, then back, returns to the schedule selection page" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.selectAnOption()
      howMuchCanYouPayEachMonthPage.clickContinue()
      checkYourPaymentPlanPage.goBack()

      howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    }
  }
}
