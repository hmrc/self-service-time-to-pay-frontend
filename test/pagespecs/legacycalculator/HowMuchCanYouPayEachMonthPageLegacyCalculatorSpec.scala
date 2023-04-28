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

import pagespecs.HowMuchCanYouPayEachMonthPageBaseSpec
import pagespecs.pages.HowMuchCanYouPayEachMonthPage
import ssttpcalculator.CalculatorType.Legacy
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.testdata.DisplayDefaultPlanOptionsTd
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class HowMuchCanYouPayEachMonthPageLegacyCalculatorSpec extends HowMuchCanYouPayEachMonthPageBaseSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  lazy val pageUnderTest: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPageLegacyCalculator

  val displayOnePlan: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = 4900,
    optionsDisplayed             = Seq("2,450"),
    optionsNotDisplayed          = Seq("4,900", "1,633.33", "1,633.34")
  )
  val displayTwoPlans: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = 3267,
    optionsDisplayed             = Seq("2,450", "1,633.33"),
    optionsNotDisplayed          = Seq("4,900", "1,225")
  )
  val displayThreePlans: DisplayDefaultPlanOptionsTd = DisplayDefaultPlanOptionsTd(
    remainingIncomeAfterSpending = defaultRemainingIncomeAfterSpending,
    optionsDisplayed             = Seq("490", "544.44", "612.50"),
    optionsNotDisplayed          = Seq("700")
  )

  val customAmountInput = 700
  val customAmountPlanMonthsOutput = 7
  val customAmountPlanInterestOutput = 54.35

  s"${overrideConfig("calculatorType")} - custom amount entry" - {
    "displays page with custom option at top when custom amount entered and continue pressed" - {
      "custom plan closest to custom amount input if exact plan not available" - {
        "plan instalment amount below custom amount input" in {
          beginJourney()

          pageUnderTest.assertInitialPageIsDisplayed

          pageUnderTest.selectCustomAmountOption()
          pageUnderTest.enterCustomAmount((customAmountInput + 10).toString)
          pageUnderTest.clickContinue()

          pageUnderTest.assertPageWithCustomAmountIsDisplayed(
            customAmountInput.toString,
            Some(customAmountPlanMonthsOutput.toString),
            Some(customAmountPlanInterestOutput.toString)
          )
        }
        "plan instalment amount above custom amount input" in {
          beginJourney()

          pageUnderTest.assertInitialPageIsDisplayed

          pageUnderTest.selectCustomAmountOption()
          pageUnderTest.enterCustomAmount((customAmountInput - 10).toString)
          pageUnderTest.clickContinue()

          pageUnderTest.assertPageWithCustomAmountIsDisplayed(
            customAmountInput.toString,
            Some(customAmountPlanMonthsOutput.toString),
            Some(customAmountPlanInterestOutput.toString)
          )
        }
      }
    }
  }
}
