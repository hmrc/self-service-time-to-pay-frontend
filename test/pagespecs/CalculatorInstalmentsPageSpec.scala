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
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, netIncomeLargeEnoughForSingleDefaultPlan, netIncomeLargeEnoughForTwoDefaultPlans, netIncomeTooSmallForPlan}

class CalculatorInstalmentsPageSpec extends ItSpec {

  def beginNewJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
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
  }

  "goes to kick out page " +
    "if 50% of remaining income after spending cannot cover amount remaining to pay including interest in 24 months of less" in {
      beginNewJourney(netIncomeTooSmallForPlan)
      weCannotAgreeYourPaymentPlanPage.assertPagePathCorrect
    }

  "display default options" - {
    "if 50% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% default option" in {
        beginNewJourney(netIncomeLargeEnoughForSingleDefaultPlan)

        calculatorInstalmentsPage28thDay.optionIsDisplayed("4,914.40")
        calculatorInstalmentsPage28thDay.optionIsNotDisplayed("6,250")
        calculatorInstalmentsPage28thDay.optionIsNotDisplayed("7,500")
        calculatorInstalmentsPage28thDay.optionIsNotDisplayed("10,000")
      }
    "if 60% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% and 60% default options" in {
        beginNewJourney(netIncomeLargeEnoughForTwoDefaultPlans)

        calculatorInstalmentsPage28thDay.optionIsDisplayed("4,750")
        calculatorInstalmentsPage28thDay.optionIsDisplayed("4,914.40", Some("1"), Some("14.40"))
        calculatorInstalmentsPage28thDay.optionIsNotDisplayed("5,700")
        calculatorInstalmentsPage28thDay.optionIsNotDisplayed("7,600")
      }
    "displays three default options otherwise" in {
      beginNewJourney()
      calculatorInstalmentsPage28thDay.assertPageIsDisplayed
    }
  }
  "displays custom amount option" - {
    "in English" in {
      beginNewJourney()
      calculatorInstalmentsPage28thDay.customAmountOptionIsDisplayed
    }
    "in Welsh" in {
      beginNewJourney()
      calculatorInstalmentsPage28thDay.clickOnWelshLink()
      calculatorInstalmentsPage28thDay.customAmountOptionIsDisplayed(Welsh)
    }
  }
  "custom amount entry" - {
    "displays page with customer option at top when custom amount entered and continue pressed" in {
      beginNewJourney()

      calculatorInstalmentsPage28thDay.assertPageIsDisplayed

      val customAmount = 280
      val planMonths = 18
      val planInterest = 124.26

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount(customAmount.toString)
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.optionIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))
    }
    "less than minimum displays error message" in {
      beginNewJourney()

      val customAmountBelowMinimum = 200

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount(customAmountBelowMinimum.toString)
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.assertBelowMinimumErrorIsDisplayed
    }
    "more than maximum displays error message" in {
      beginNewJourney()

      val customAmountBelowMinimum = 7000

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount(customAmountBelowMinimum.toString)
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.assertAboveMaximumErrorIsDisplayed
    }
    "not filled in displays error message" in {
      beginNewJourney()

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount()
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.assertNoInputErrorIsDisplayed
    }
    "filled with non-numeric displays error message" in {
      beginNewJourney()

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount("non-numeric")
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.assertNonNumericErrorIsDisplayed
    }
    "filled with negative amount displays error message" in {
      beginNewJourney()

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount("-1")
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.assertNegativeAmountErrorIsDisplayed
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

  "select an option and continue" - {
    "basic case" in {
      beginNewJourney()
      calculatorInstalmentsPage28thDay.selectAnOption()
      calculatorInstalmentsPage28thDay.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }
    "case with large number of decimal places of plan selection amounts" in {
      beginNewJourney(netIncomeLargeEnoughForSingleDefaultPlan)

      calculatorInstalmentsPage28thDay.selectAnOption()
      calculatorInstalmentsPage28thDay.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }

  }

  "returning to the page" - {
    "selecting a default option, continue, then back, returns to the schedule selection page" in {
      beginNewJourney()
      calculatorInstalmentsPage28thDay.selectAnOption()
      calculatorInstalmentsPage28thDay.clickContinue()
      checkYourPaymentPlanPage.clickOnBackButton()

      calculatorInstalmentsPage28thDay.assertPageIsDisplayed
    }
    "selecting an option, continue, back to change income or spending, resets previous plan selection - doesn't display previous selection" in {
      beginNewJourney()

      val customAmount = 280
      val planMonths = 18
      val planInterest = 124.26

      calculatorInstalmentsPage28thDay.selectCustomAmountOption()
      calculatorInstalmentsPage28thDay.enterCustomAmount(customAmount.toString)
      calculatorInstalmentsPage28thDay.clickContinue()

      calculatorInstalmentsPage28thDay.optionIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))

      calculatorInstalmentsPage28thDay.selectASpecificOption("0")
      calculatorInstalmentsPage28thDay.clickContinue()

      checkYourPaymentPlanPage.clickOnBackButton()
      calculatorInstalmentsPage28thDay.clickOnBackButton()

      howMuchYouCouldAffordPage.clickOnAddChangeIncome()
      yourMonthlyIncomePage.enterMonthlyIncome("501")
      yourMonthlyIncomePage.clickContinue()

      howMuchYouCouldAffordPage.clickContinue()

      calculatorInstalmentsPage28thDay.optionIsNotDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))

    }
  }
}
