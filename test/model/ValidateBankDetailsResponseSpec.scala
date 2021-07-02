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

package model

import bars.model.ValidateBankDetailsResponse
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import testsupport.RichMatchers.convertToClueful

class ValidateBankDetailsResponseSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {
  private val yes = "yes"
  private val no = "no"
  private val indeterminate = "indeterminate"
  private val missing = None

  val testCases = {
      implicit def liftStringToSome(str: String): Some[String] = Some(str) // Just to make the table easier to read
    Table[Boolean, Option[String], Option[String], Option[String], String](
      ("isValid", "directDebitsDisallowed", "directDebitInstructionsDisallowed", "supportsBACS", "accountNumberWithSortCodeIsValid"),
      (true, yes, no, yes, yes),
      (true, yes, no, yes, no),
      (true, yes, no, yes, indeterminate),
      (true, yes, no, no, yes),
      (true, yes, no, no, no),
      (true, yes, no, no, indeterminate),
      (true, yes, no, missing, yes),
      (true, yes, no, missing, no),
      (true, yes, no, missing, indeterminate),
      (true, no, yes, yes, yes),
      (true, no, yes, yes, no),
      (true, no, yes, yes, indeterminate),
      (true, no, yes, no, yes),
      (true, no, yes, no, no),
      (true, no, yes, no, indeterminate),
      (true, no, yes, missing, yes),
      (true, no, yes, missing, no),
      (true, no, yes, missing, indeterminate),
      (true, no, no, yes, yes),
      (true, no, no, yes, no),
      (true, no, no, yes, indeterminate),
      (true, no, no, no, yes),
      (true, no, no, no, no),
      (true, no, no, no, indeterminate),
      (true, no, no, missing, yes),
      (true, no, no, missing, no),
      (true, no, no, missing, indeterminate),
      (true, no, missing, yes, yes),
      (true, no, missing, yes, no),
      (true, no, missing, yes, indeterminate),
      (true, no, missing, no, yes),
      (true, no, missing, no, no),
      (true, no, missing, no, indeterminate),
      (true, no, missing, missing, yes),
      (true, no, missing, missing, no),
      (true, no, missing, missing, indeterminate),
      (true, missing, no, yes, yes),
      (true, missing, no, yes, no),
      (true, missing, no, yes, indeterminate),
      (true, missing, no, no, yes),
      (true, missing, no, no, no),
      (true, missing, no, no, indeterminate),
      (true, missing, no, missing, yes),
      (true, missing, no, missing, no),
      (true, missing, no, missing, indeterminate),
      (true, missing, missing, yes, yes),
      (true, missing, missing, yes, no),
      (true, missing, missing, yes, indeterminate),
      (true, missing, missing, missing, yes),
      (false, yes, yes, yes, yes),
      (false, yes, yes, yes, no),
      (false, yes, yes, yes, indeterminate),
      (false, yes, yes, no, yes),
      (false, yes, yes, no, no),
      (false, yes, yes, no, indeterminate),
      (false, yes, yes, missing, yes),
      (false, yes, yes, missing, no),
      (false, yes, yes, missing, indeterminate),
      (false, yes, missing, yes, yes),
      (false, yes, missing, yes, no),
      (false, yes, missing, yes, indeterminate),
      (false, yes, missing, no, yes),
      (false, yes, missing, no, no),
      (false, yes, missing, no, indeterminate),
      (false, yes, missing, missing, yes),
      (false, yes, missing, missing, no),
      (false, yes, missing, missing, indeterminate),
      (false, missing, yes, yes, yes),
      (false, missing, yes, yes, no),
      (false, missing, yes, yes, indeterminate),
      (false, missing, yes, no, yes),
      (false, missing, yes, no, no),
      (false, missing, yes, no, indeterminate),
      (false, missing, yes, missing, yes),
      (false, missing, yes, missing, no),
      (false, missing, yes, missing, indeterminate),
      (false, missing, missing, no, yes),
      (false, missing, missing, no, no),
      (false, missing, missing, no, indeterminate),
      (false, missing, missing, missing, no),
      (false, missing, missing, missing, indeterminate)
    )
  }

  private val mkResponseFromTestData = (
    _: Boolean,
    directDebitsDisallowed: Option[String],
    directDebitInstructionsDisallowed: Option[String],
    supportsBACS: Option[String],
    accountNumberWithSortCodeIsValid: String
  ) => {
    ValidateBankDetailsResponse(
      accountNumberWithSortCodeIsValid         = accountNumberWithSortCodeIsValid,
      nonStandardAccountDetailsRequiredForBacs = "",
      sortCodeIsPresentOnEISCD                 = "",
      supportsBACS                             = supportsBACS,
      ddiVoucherFlag                           = None,
      directDebitsDisallowed                   = directDebitsDisallowed,
      directDebitInstructionsDisallowed        = directDebitInstructionsDisallowed,
      iban                                     = None,
      sortCodeBankName                         = None
    )
  }

  "isInvalid" - {
    "should return the correct value" in {
      forAll(testCases) {
        case testData @ (expectedIsValid: Boolean, _: Option[String], _: Option[String], _: Option[String], _: String) =>
          val response = mkResponseFromTestData.tupled(testData)

          response.isValid shouldBe expectedIsValid withClue s" - isValid was expected to to return $expectedIsValid for this test case"
      }
    }

    "test cases are exhaustive and cover all combinations" in {
      val allResponses: Seq[ValidateBankDetailsResponse] = for {
        directDebitsDisallowed <- Seq(Some(yes), Some(no), missing)
        directDebitInstructionsDisallowed <- Seq(Some(yes), Some(no), missing)
        supportsBACS <- Seq(Some(yes), Some(no), missing)
        accountNumberWithSortCodeIsValid <- Seq(yes, no, indeterminate)
      } yield {
        val unused = true

        mkResponseFromTestData(unused, directDebitsDisallowed, directDebitInstructionsDisallowed, supportsBACS, accountNumberWithSortCodeIsValid)
      }

      val testDataResponses = testCases.map{
        case testData @ (_: Boolean, _: Option[String], _: Option[String], _: Option[String], _: String) =>
          mkResponseFromTestData.tupled(testData)
      }

      allResponses.toSet shouldBe testDataResponses.toSet withClue " - the test data does not cover all possible combinations of responses"
    }
  }

}
