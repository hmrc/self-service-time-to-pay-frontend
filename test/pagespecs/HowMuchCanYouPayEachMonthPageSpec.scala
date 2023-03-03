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

class HowMuchCanYouPayEachMonthPageSpec extends ItSpec {

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
  "displays custom amount option" - {
    "in English" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.customAmountOptionIsDisplayed
    }
    "in Welsh" in {
      beginJourney()
      howMuchCanYouPayEachMonthPage.clickOnWelshLink()
      howMuchCanYouPayEachMonthPage.customAmountOptionIsDisplayed(Welsh)
    }
  }
  "custom amount entry" - {
    "displays page with custom option at top when custom amount entered and continue pressed" in {
      beginJourney()

      howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed

      val customAmount = 280
      val planMonths = 18
      val planInterest = 124.26

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount(customAmount.toString)
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertPageWithCustomAmountIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))
    }
    "less than minimum displays error message" in {
      beginJourney()

      val customAmountBelowMinimum = 200

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount(customAmountBelowMinimum.toString)
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertBelowMinimumErrorIsDisplayed
    }
    "more than maximum displays error message" in {
      beginJourney()

      val customAmountBelowMinimum = 7000

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount(customAmountBelowMinimum.toString)
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertAboveMaximumErrorIsDisplayed
    }
    "not filled in displays error message" in {
      beginJourney()

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount()
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertNoInputErrorIsDisplayed
    }
    "filled with non-numeric displays error message" in {
      beginJourney()

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount("non-numeric")
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertNonNumericErrorIsDisplayed
    }
    "filled with negative amount displays error message" in {
      beginJourney()

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount("-1")
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.assertNegativeAmountErrorIsDisplayed
    }
    "filled with more than two decimal places" - {
      "in English" in {
        beginJourney()

        howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
        howMuchCanYouPayEachMonthPage.enterCustomAmount("280.111")
        howMuchCanYouPayEachMonthPage.clickContinue()

        howMuchCanYouPayEachMonthPage.assertDecimalPlacesErrorIsDisplayed
      }
      "in Welsh" in {
        beginJourney()

        howMuchCanYouPayEachMonthPage.clickOnWelshLink()
        howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
        howMuchCanYouPayEachMonthPage.enterCustomAmount("280.111")
        howMuchCanYouPayEachMonthPage.clickContinue()

        howMuchCanYouPayEachMonthPage.assertDecimalPlacesErrorIsDisplayed(Welsh)
      }

    }
  }

  "language" in {
    beginJourney()

    howMuchCanYouPayEachMonthPage.clickOnWelshLink()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed(Welsh)

    howMuchCanYouPayEachMonthPage.clickOnEnglishLink()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    howMuchCanYouPayEachMonthPage.backButtonHref shouldBe Some(s"${baseUrl.value}${howMuchYouCouldAffordPage.path}")
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
      checkYourPaymentPlanPage.clickOnBackButton()

      howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    }
    "selecting an option, continue, back to change income or spending, resets previous plan selection - doesn't display previous selection" in {
      beginJourney()

      val customAmount = 280
      val planMonths = 18
      val planInterest = 124.26

      howMuchCanYouPayEachMonthPage.selectCustomAmountOption()
      howMuchCanYouPayEachMonthPage.enterCustomAmount(customAmount.toString)
      howMuchCanYouPayEachMonthPage.clickContinue()

      howMuchCanYouPayEachMonthPage.optionIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))

      howMuchCanYouPayEachMonthPage.selectASpecificOption("0")
      howMuchCanYouPayEachMonthPage.clickContinue()

      checkYourPaymentPlanPage.clickOnBackButton()
      howMuchCanYouPayEachMonthPage.clickOnBackButton()

      howMuchYouCouldAffordPage.clickOnAddChangeIncome()
      yourMonthlyIncomePage.enterMonthlyIncome("501")
      yourMonthlyIncomePage.clickContinue()

      howMuchYouCouldAffordPage.clickContinue()

      howMuchCanYouPayEachMonthPage.optionIsNotDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))

    }
  }
}
