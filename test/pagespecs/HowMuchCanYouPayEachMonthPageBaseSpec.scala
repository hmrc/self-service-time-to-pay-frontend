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
import pagespecs.pages.HowMuchCanYouPayEachMonthPage
import ssttpcalculator.CalculatorType.PaymentOptimised
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.DisplayDefaultPlanOptionsTd
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, netIncomeLargeEnoughForSingleDefaultPlan, netIncomeLargeEnoughForTwoDefaultPlans, netIncomeTooSmallForPlan}

class HowMuchCanYouPayEachMonthPageSpec extends HowMuchCanYouPayEachMonthPageBaseSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> PaymentOptimised.value
  )

  lazy val pageUnderTest: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPage

  val displayOnePlan: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = netIncomeLargeEnoughForSingleDefaultPlan,
    optionsDisplayed             = Seq("4,914.40"),
    optionsNotDisplayed          = Seq("6,250", "7,500", "10,000")
  )
  val displayTwoPlans: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = netIncomeLargeEnoughForTwoDefaultPlans,
    optionsDisplayed             = Seq("4,750", "4,914.40"),
    optionsNotDisplayed          = Seq("5,700", "7,600")
  )
  val displayThreePlans: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = defaultRemainingIncomeAfterSpending,
    optionsDisplayed             = Seq("500", "600", "800"),
    optionsNotDisplayed          = Seq.empty
  )

  val customAmountInput = 700
  val customAmountPlanMonthsOutput = 8
  val customAmountPlanInterestOutput = 54.35
}

trait HowMuchCanYouPayEachMonthPageBaseSpec extends ItSpec {

  val pageUnderTest: HowMuchCanYouPayEachMonthPage

  val customAmountInput: BigDecimal
  val customAmountPlanMonthsOutput: Int
  val customAmountPlanInterestOutput: BigDecimal

  val displayOnePlan: DisplayDefaultPlanOptionsTd
  val displayTwoPlans: DisplayDefaultPlanOptionsTd
  val displayThreePlans: DisplayDefaultPlanOptionsTd

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

  s"display default options" - {
    "if 50% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% default option" in {
        beginJourney(displayOnePlan.remainingIncomeAfterSpending)

        displayOnePlan.optionsDisplayed.foreach(pageUnderTest.optionIsDisplayed(_))
        displayOnePlan.optionsNotDisplayed.foreach(pageUnderTest.optionIsNotDisplayed(_))
      }
    "if 60% of remaining income after spending covers amount remaining to pay including interest in one month " +
      "displays only 50% and 60% default options" in {
        beginJourney(displayTwoPlans.remainingIncomeAfterSpending)

        displayTwoPlans.optionsDisplayed.foreach(pageUnderTest.optionIsDisplayed(_))
        displayTwoPlans.optionsNotDisplayed.foreach(pageUnderTest.optionIsNotDisplayed(_))
      }
    "displays three default options otherwise" in {
      beginJourney(displayThreePlans.remainingIncomeAfterSpending)

      displayThreePlans.optionsDisplayed.foreach(pageUnderTest.optionIsDisplayed(_))
      displayThreePlans.optionsNotDisplayed.foreach(pageUnderTest.optionIsNotDisplayed(_))
    }
  }

  "displays custom amount option" - {
    "in English" in {
      beginJourney()
      pageUnderTest.customAmountOptionIsDisplayed
    }
    "in Welsh" in {
      beginJourney()
      pageUnderTest.clickOnWelshLink()
      pageUnderTest.customAmountOptionIsDisplayed(Welsh)
    }
  }

  "displays error message if press continue without selecting an option" - {
    "in English" in {
      beginJourney()
      pageUnderTest.clickContinue()
      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
      pageUnderTest.assertNoOptionSelectedErrorIsDisplayed
      pageUnderTest.assertInitialPageContentIsDisplayed
    }
    "in Welsh" in {
      beginJourney()
      pageUnderTest.clickOnWelshLink()
      pageUnderTest.clickContinue()
      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix(Welsh)
      pageUnderTest.assertNoOptionSelectedErrorIsDisplayed(Welsh)
      pageUnderTest.assertInitialPageContentIsDisplayed(Welsh)
    }
  }

  "custom amount entry" - {
    "displays page with custom option at top when custom amount entered and continue pressed" - {
      "in English" in {
        beginJourney()

        pageUnderTest.assertInitialPageIsDisplayed

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountInput.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertPageWithCustomAmountIsDisplayed(customAmountInput.toString, Some(customAmountPlanMonthsOutput.toString), Some(customAmountPlanInterestOutput.toString))
      }
      "in Welsh" in {
        beginJourney()
        pageUnderTest.clickOnWelshLink()

        pageUnderTest.assertInitialPageIsDisplayed(Welsh)

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountInput.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertPageWithCustomAmountIsDisplayed(customAmountInput.toString, Some(customAmountPlanMonthsOutput.toString), Some(customAmountPlanInterestOutput.toString))(Welsh)
      }
    }
    "displays error message and options including custom option if press continue after custom option displayed without selecting an option" in {
      beginJourney()

      pageUnderTest.assertInitialPageIsDisplayed

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount(customAmountInput.toString)
      pageUnderTest.clickContinue()

      pageUnderTest.clickContinue()
      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
      pageUnderTest.assertNoOptionSelectedErrorIsDisplayed
      pageUnderTest.assertPageWithCustomAmountContentIsDisplayed(customAmountInput.toString, Some(customAmountPlanMonthsOutput.toString), Some(customAmountPlanInterestOutput.toString))
    }
    "less than minimum displays error message" - {
      "in English" in {
        beginJourney()

        val customAmountBelowMinimum = 200

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountBelowMinimum.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
        pageUnderTest.assertBelowMinimumErrorIsDisplayed
      }
      "in Welsh" in {
        beginJourney()
        pageUnderTest.clickOnWelshLink()

        val customAmountBelowMinimum = 200

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountBelowMinimum.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix(Welsh)
        pageUnderTest.assertBelowMinimumErrorIsDisplayed(Welsh)
      }
    }
    "more than maximum displays error message" - {
      "in English" in {
        beginJourney()

        val customAmountBelowMinimum = 7000

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountBelowMinimum.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
        pageUnderTest.assertAboveMaximumErrorIsDisplayed
      }
      "in Welsh" in {
        beginJourney()
        pageUnderTest.clickOnWelshLink()

        val customAmountBelowMinimum = 7000

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmountBelowMinimum.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix(Welsh)
        pageUnderTest.assertAboveMaximumErrorIsDisplayed(Welsh)
      }
    }
    "not filled in displays error message" in {
      beginJourney()

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount()
      pageUnderTest.clickContinue()

      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
      pageUnderTest.assertNoInputErrorIsDisplayed
    }
    "filled with non-numeric displays error message" in {
      beginJourney()

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount("non-numeric")
      pageUnderTest.clickContinue()

      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
      pageUnderTest.assertNonNumericErrorIsDisplayed
    }
    "filled with negative amount displays error message" in {
      beginJourney()

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount("-1")
      pageUnderTest.clickContinue()

      pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
      pageUnderTest.assertNegativeAmountErrorIsDisplayed
    }
    "filled with more than two decimal places" - {
      "in English" in {
        beginJourney()

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount("280.111")
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
        pageUnderTest.assertDecimalPlacesErrorIsDisplayed
      }
      "in Welsh" in {
        beginJourney()

        pageUnderTest.clickOnWelshLink()
        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount("280.111")
        pageUnderTest.clickContinue()

        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix(Welsh)
        pageUnderTest.assertDecimalPlacesErrorIsDisplayed(Welsh)
      }
    }
  }

  "language" in {
    beginJourney()

    pageUnderTest.clickOnWelshLink()
    pageUnderTest.assertInitialPageIsDisplayed(Welsh)

    pageUnderTest.clickOnEnglishLink()
    pageUnderTest.assertInitialPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    pageUnderTest.backButtonHref shouldBe Some(s"${baseUrl.value}${howMuchYouCouldAffordPage.path}")
  }

  "select an option and continue" - {
    "basic case" in {
      beginJourney()
      pageUnderTest.selectAnOption()
      pageUnderTest.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }
    "case with large number of decimal places of plan selection amounts" in {
      beginJourney(netIncomeLargeEnoughForSingleDefaultPlan)

      pageUnderTest.selectAnOption()
      pageUnderTest.clickContinue()
      checkYourPaymentPlanPage.expectedHeadingContent(English)
    }

  }

  "returning to the page" - {
    "selecting a default option, continue, then back, returns to the schedule selection page" in {
      beginJourney()
      pageUnderTest.selectAnOption()
      pageUnderTest.clickContinue()
      checkYourPaymentPlanPage.clickOnBackButton()

      pageUnderTest.assertInitialPageIsDisplayed
    }
    "selecting a custom option, continue, back to change income or spending, resets previous plan selection - doesn't display previous selection" in {
      beginJourney()

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount(customAmountInput.toString)
      pageUnderTest.clickContinue()

      pageUnderTest.optionIsDisplayed(customAmountInput.toString, Some(customAmountPlanMonthsOutput.toString), Some(customAmountPlanInterestOutput.toString))

      pageUnderTest.selectASpecificOption(PaymentPlanOption.Custom)
      pageUnderTest.clickContinue()

      checkYourPaymentPlanPage.clickOnBackButton()
      pageUnderTest.clickOnBackButton()

      howMuchYouCouldAffordPage.clickOnAddChangeIncome()
      yourMonthlyIncomePage.enterMonthlyIncome("501")
      yourMonthlyIncomePage.clickContinue()

      howMuchYouCouldAffordPage.clickContinue()

      pageUnderTest.optionIsNotDisplayed(customAmountInput.toString, Some(customAmountPlanMonthsOutput.toString), Some(customAmountPlanInterestOutput.toString))
    }
  }
}
