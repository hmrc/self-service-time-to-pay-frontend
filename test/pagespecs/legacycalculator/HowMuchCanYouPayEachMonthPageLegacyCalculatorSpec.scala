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
import pagespecs.HowMuchCanYouPayEachMonthPageBaseSpec
import pagespecs.pages.HowMuchCanYouPayEachMonthPage
import ssttpcalculator.CalculatorType.Legacy
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, netIncomeLargeEnoughForSingleDefaultPlan, netIncomeLargeEnoughForTwoDefaultPlans, netIncomeTooSmallForPlan}

class HowMuchCanYouPayEachMonthPageLegacyCalculatorSpec extends HowMuchCanYouPayEachMonthPageBaseSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  lazy val pageUnderTest: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPageLegacyCalculator


  s"${overrideConfig("calculatorType")} - display default options" - {
    "if a two-month plan is the closest monthly amount less than 50% of remaining income after spending, show only this option" in {
      beginJourney(4900)

      pageUnderTest.optionIsDisplayed("2,450")
      pageUnderTest.optionIsNotDisplayed("4,900")
      pageUnderTest.optionIsNotDisplayed("1,633.33")
      pageUnderTest.optionIsNotDisplayed("1,633.34")
    }
    "if a three-month plan is the closest monthly amount less than 50% of remaining income after spending, " +
      "show three-month and two-month plans only" in {
        beginJourney(3267)

        pageUnderTest.optionIsDisplayed("1,633.33")
        pageUnderTest.optionIsDisplayed("2,450")
        pageUnderTest.optionIsNotDisplayed("4,900")
        pageUnderTest.optionIsNotDisplayed("1,225")
      }
    "displays three default options otherwise" in {
      beginJourney()
      pageUnderTest.optionIsDisplayed("490")
      pageUnderTest.optionIsDisplayed("544.44")
      pageUnderTest.optionIsDisplayed("612.50")
      pageUnderTest.optionIsNotDisplayed("700")
    }
  }

  s"${overrideConfig("calculatorType")} - custom amount entry" - {
    "displays page with custom option at top when custom amount entered and continue pressed" - {
      "custom plan matching custom amount input exactly if available" in {
        beginJourney()

        pageUnderTest.assertInitialPageIsDisplayed

        val customAmount = 700
        val planMonths = 7
        val planInterest = 54.35

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmount.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.assertPageWithCustomAmountIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))
      }
      "custom plan closest to custom amount input if exact plan not available" - {
        "plan instalment amount below custom amount input" in {
          beginJourney()

          pageUnderTest.assertInitialPageIsDisplayed

          val customAmount = 710
          val planMonths = 7
          val planInterest = 54.35

          pageUnderTest.selectCustomAmountOption()
          pageUnderTest.enterCustomAmount(customAmount.toString)
          pageUnderTest.clickContinue()

          pageUnderTest.assertPageWithCustomAmountIsDisplayed(
            (customAmount - 10).toString,
            Some(planMonths.toString),
            Some(planInterest.toString)
          )
        }
        "plan instalment amount above custom amount input" in {
          beginJourney()

          pageUnderTest.assertInitialPageIsDisplayed

          val customAmount = 690
          val planMonths = 7
          val planInterest = 54.35

          pageUnderTest.selectCustomAmountOption()
          pageUnderTest.enterCustomAmount(customAmount.toString)
          pageUnderTest.clickContinue()

          pageUnderTest.assertPageWithCustomAmountIsDisplayed(
            (customAmount + 10).toString,
            Some(planMonths.toString),
            Some(planInterest.toString)
          )
        }

      }
      "displays error message and options including custom option if press continue after custom option displayed without selecting an option" in {
        beginJourney()

        pageUnderTest.assertInitialPageIsDisplayed

        val customAmount = 700
        val planMonths = 7
        val planInterest = 54.35

        pageUnderTest.selectCustomAmountOption()
        pageUnderTest.enterCustomAmount(customAmount.toString)
        pageUnderTest.clickContinue()

        pageUnderTest.clickContinue()
        pageUnderTest.assertExpectedHeadingContentWithErrorPrefix
        pageUnderTest.assertNoOptionSelectedErrorIsDisplayed
        pageUnderTest.assertPageWithCustomAmountContentIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))
      }
    }
  }
  s"${overrideConfig("calculatorType")} - returning to the page" - {
    "selecting a custom option, continue, back to change income or spending, resets previous plan selection - doesn't display previous selection" in {
      beginJourney()

      val customAmount = 700
      val planMonths = 7
      val planInterest = 54.35

      pageUnderTest.selectCustomAmountOption()
      pageUnderTest.enterCustomAmount(customAmount.toString)
      pageUnderTest.clickContinue()

      pageUnderTest.optionIsDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))

      pageUnderTest.selectASpecificOption(PaymentPlanOption.Custom)
      pageUnderTest.clickContinue()

      checkYourPaymentPlanPage.clickOnBackButton()
      pageUnderTest.clickOnBackButton()

      howMuchYouCouldAffordPage.clickOnAddChangeIncome()
      yourMonthlyIncomePage.enterMonthlyIncome("501")
      yourMonthlyIncomePage.clickContinue()

      howMuchYouCouldAffordPage.clickContinue()

      pageUnderTest.optionIsNotDisplayed(customAmount.toString, Some(planMonths.toString), Some(planInterest.toString))
    }
  }

}
