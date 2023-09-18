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

package util

import testsupport.UnitSpec

class CurrencyUtilSpec extends UnitSpec {

  "Currency.formatToCurrencyString converts a BigDecimal to a string in currency format (two or no decimal places)" - {

    "Two zero decimal places evaluates to string with no decimal places" in {
      val twoZeroDecimalPlaces = 100.00

      CurrencyUtil.formatToCurrencyString(twoZeroDecimalPlaces) shouldBe "100"
    }
    "One zero decimal place evaluates to string with no decimal places" in {
      val oneZeroDecimalPlace = 100.0

      CurrencyUtil.formatToCurrencyString(oneZeroDecimalPlace) shouldBe "100"
    }
    "One non-zero decimal place evaluates to string with two decimal places (second being zero)" in {
      val oneNonZeroDecimalPlace = 100.1

      CurrencyUtil.formatToCurrencyString(oneNonZeroDecimalPlace) shouldBe "100.10"
    }
  }

}
