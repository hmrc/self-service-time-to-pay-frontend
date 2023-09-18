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

package ssttpaffordability

import testsupport.UnitSpec

class IncomeInputSpec extends UnitSpec {
  "IncomeInput.unapply evaluates to strings in currency format (two or no decimal places)" - {
    val twoZeroDecimalPlaces = 100.00
    val oneZeroDecimalPlace = 100.0
    val oneNonZeroDecimalPlace = 100.1

    val testCase = IncomeInput(
      twoZeroDecimalPlaces,
      oneZeroDecimalPlace,
      oneNonZeroDecimalPlace
    )

    "Two zero decimal places evaluates to string with no decimal places" in {
      IncomeInput.unapply(testCase).get._1 shouldBe "100"
    }
    "One zero decimal place evaluates to string with no decimal places" in {
      IncomeInput.unapply(testCase).get._2 shouldBe "100"
    }
    "One non-zero decimal place evaluates to string with two decimal places (second being zero)" in {
      IncomeInput.unapply(testCase).get._3 shouldBe "100.10"
    }
  }

}
