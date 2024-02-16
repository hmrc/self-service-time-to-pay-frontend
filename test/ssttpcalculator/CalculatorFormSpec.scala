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

package ssttpcalculator

import testsupport.UnitSpec
import uk.gov.hmrc.selfservicetimetopay.models.{CustomPlanRequest, PlanChoice}

class CalculatorFormSpec extends UnitSpec {

  "CalculatorForm.apply generates a plan selection data structure" - {
    "if the radio selection is the custom amount option" +
      " then it generates the plan selection with a custom plan request" - {
      }
    "Using the max custom amount not rounded if selected:" +
      " if the custom amount input is equal to the max custom amount (rounded to two decimal places" +
      " then the custom amount in the request is the max custom amount not rounded" - {
        "where maxCustomAmount rounds DOWN to 2 dc to WHOLE NUMBER (£XX)" in {

          val maxCustomAmount = 1000.004

          val radioSelection = "customAmountOption"
          val customAmountInput = Some("1000")

          val result = CalculatorForm.apply(maxCustomAmount)(radioSelection, customAmountInput)

          result.selection shouldBe PlanChoice(Right(CustomPlanRequest(1000.004)))
        }
        "where maxCustomAmount rounds down to 2 dp with NUMBER WITH 2 DP (£XX.XX)" in {

          val maxCustomAmount = 1000.504

          val radioSelection = "customAmountOption"
          val customAmountInput = Some("1000.50")

          val result = CalculatorForm.apply(maxCustomAmount)(radioSelection, customAmountInput)

          result.selection shouldBe PlanChoice(Right(CustomPlanRequest(1000.504)))
        }
        "where maxCustomAmount rounds UP to 2 dc to WHOLE NUMBER (£XX)" in {

          val maxCustomAmount = 1000.995

          val radioSelection = "customAmountOption"
          val customAmountInput = Some("1001")

          val result = CalculatorForm.apply(maxCustomAmount)(radioSelection, customAmountInput)

          result.selection shouldBe PlanChoice(Right(CustomPlanRequest(1000.995)))
        }
        "where maxCustomAmount rounds UP to 2 dc to NUMBER WITH 2 DP (£XX.XX)" in {

          val maxCustomAmount = 1000.005

          val radioSelection = "customAmountOption"
          val customAmountInput = Some("1000.01")

          val result = CalculatorForm.apply(maxCustomAmount)(radioSelection, customAmountInput)

          result.selection shouldBe PlanChoice(Right(CustomPlanRequest(1000.005)))
        }

      }
    "Otherwise: " +
      " if the custom amount input is NOT equal to the max custom amount (rounded)" +
      " then the custom amount in the request is the custom amount input" in {

        val maxCustomAmount = 1000.004

        val radioSelection = "customAmountOption"
        val customAmountInput = Some("1001")

        val result = CalculatorForm.apply(maxCustomAmount)(radioSelection, customAmountInput)

        result.selection shouldBe PlanChoice(Right(CustomPlanRequest(1001)))

      }
  }
  "if the radio selection is one of the plans" +
    " then it generates the plan selection with a selected plan" - {
      "Using the max custom amount not rounded if selected:" +
        " if the radio selection amount (rounded to 2 DP - just in case) is equal to the max custom amount (rounded to 2 DP)" +
        " then the selected plan amount is the max custom amount not rounded" in {

        }
      "Otherwise: " +
        " if the radio selection amount (rounded) is NOT equal to the max custom amount (rounded)" +
        " then the radio selection value is used in the plan selection" in {

        }
    }
}
