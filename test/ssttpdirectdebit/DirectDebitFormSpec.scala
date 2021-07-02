/*
 * Copyright 2021 HM Revenue & Customs
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

package ssttpdirectdebit

import testsupport.UnitSpec

class DirectDebitFormSpec extends UnitSpec {

  case class TestCase(
      input:   String,
      isValid: Boolean,
      clue:    String
  )
  val testCases = List(
    TestCase("000000", true, "valid"),
    TestCase("00-00-00", true, "hyphens are allowed"),
    TestCase("0-0-0 0-0-0", true, "hyphens and spaces mishmash - still valid"),
    TestCase(" 00 - 00 - 00 ", true, "hyphens and spaces mishmash2 - still valid"),
    TestCase("00 00 00", true, "spaces are allowed"),
    TestCase("   00 00 00  ", true, "around spaces are allowed"),

    TestCase("00x00x00", false, "not valid character"),
    TestCase("123", false, "invalid length"),
    TestCase("123   ", false, "invalid length (spaces around are trimmed)"),
    TestCase("", false, "empty string is invalid")
  )

  "sort code validation" in {
    testCases.foreach{ tc =>
      DirectDebitForm.isValidSortCode(tc.input) mustBe tc.isValid withClue tc.clue
    }
  }

}
