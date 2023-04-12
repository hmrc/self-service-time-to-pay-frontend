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
import testsupport.ItSpec
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, netIncomeLargeEnoughForSingleDefaultPlan, netIncomeLargeEnoughForTwoDefaultPlans, netIncomeTooSmallForPlan}

class HowMuchCanYouPayEachMonthPageLegacyCalculatorSpec extends ItSpec with LegacyCalculatorPages {

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
  }

  "goes to kick out page " +
    "if 50% of remaining income after spending cannot cover amount remaining to pay including interest in 24 months of less" in {
      beginJourney(netIncomeTooSmallForPlan)
      weCannotAgreeYourPaymentPlanPage.assertPagePathCorrect
    }

  "display default options" - {
    "if a two-month plan is the closest monthly amount less than 50% of remaining income after spending, show only this option" in {
      beginJourney(4900)

      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("2,450")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("4,900")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("1,633.33")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("1,633.34")
      //        howMuchCanYouPayEachMonthPageLegacyCalculator.assertContentDoesNotContainOrSeparator
    }
    "if a three-month plan is the closest monthly amount less than 50% of remaining income after spending, " +
      "show three-month and two-month plans only" in {
        beginJourney(3267)

        howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("1,633.33")
        howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("2,450")
        howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("4,900")
        howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("1,225")
        //        howMuchCanYouPayEachMonthPageLegacyCalculator.assertContentDoesNotContainOrSeparator

      }
    "displays three default options otherwise" in {
      beginJourney()
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("490")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("544.44")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsDisplayed("612.50")
      howMuchCanYouPayEachMonthPageLegacyCalculator.optionIsNotDisplayed("700")
    }
  }
  "does not display a custom amount option" - {
    "in English" in {
      beginJourney()
      howMuchCanYouPayEachMonthPageLegacyCalculator.customAmountOptionNotDisplayed
    }
    "in Welsh" in {
      beginJourney()
      howMuchCanYouPayEachMonthPageLegacyCalculator.clickOnWelshLink()
      howMuchCanYouPayEachMonthPageLegacyCalculator.customAmountOptionNotDisplayed(Welsh)
    }
  }

  "language" in {
    beginJourney()

    howMuchCanYouPayEachMonthPageLegacyCalculator.clickOnWelshLink()
    howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed(Welsh)

    howMuchCanYouPayEachMonthPageLegacyCalculator.clickOnEnglishLink()
    howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed(English)
  }

  "select an option and continue" - {
    "basic case" in {
      beginJourney()
      howMuchCanYouPayEachMonthPageLegacyCalculator.selectAnOption()
      howMuchCanYouPayEachMonthPageLegacyCalculator.clickContinue()
      checkYourPaymentPlanPageLegacyCalculator.expectedHeadingContent(English)
    }
    "case with large number of decimal places of plan selection amounts" in {
      beginJourney(netIncomeLargeEnoughForSingleDefaultPlan)

      howMuchCanYouPayEachMonthPageLegacyCalculator.selectAnOption()
      howMuchCanYouPayEachMonthPageLegacyCalculator.clickContinue()
      checkYourPaymentPlanPageLegacyCalculator.expectedHeadingContent(English)
    }

  }

  "returning to the page" - {
    "selecting a default option, continue, then back, returns to the schedule selection page" in {
      beginJourney(1000)
      howMuchCanYouPayEachMonthPageLegacyCalculator.selectAnOption()
      howMuchCanYouPayEachMonthPageLegacyCalculator.clickContinue()
      checkYourPaymentPlanPageLegacyCalculator.goBack()

      howMuchCanYouPayEachMonthPageLegacyCalculator.assertInitialPageIsDisplayed
    }
  }
}
