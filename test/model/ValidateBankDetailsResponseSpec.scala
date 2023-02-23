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

package model

import bars.model.BarsAssessmentType._
import bars.model.{BarsAssessmentType, ValidateBankDetailsResponse}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import testsupport.RichMatchers.convertToClueful

class ValidateBankDetailsResponseSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  val testCases = {
      implicit def liftStringToSome(str: String): Some[String] = Some(str) // Just to make the table easier to read

    Table[Boolean, BarsAssessmentType, BarsAssessmentType](
      ("isValid", "accountNumberIsWellFormatted", "sortCodeIsPresentOnEISCD"),
      (true, Yes, Yes),
      (true, Indeterminate, Yes),
      (false, No, Yes),
      (false, Indeterminate, No),
      (false, No, No),
      (false, Yes, No)
    )
  }

  private val mkResponseFromTestData = (
    _: Boolean,
    accountNumberIsWellFormatted: BarsAssessmentType,
    sortCodeIsPresentOnEISCD: BarsAssessmentType,
  ) => {
    ValidateBankDetailsResponse(
      accountNumberIsWellFormatted             = accountNumberIsWellFormatted,
      sortCodeIsPresentOnEISCD                 = sortCodeIsPresentOnEISCD,
      nonStandardAccountDetailsRequiredForBacs = No,
      sortCodeSupportsDirectDebit              = None,
      sortCodeSupportsDirectCredit             = None,
      iban                                     = None,
      sortCodeBankName                         = None
    )
  }

  "isValid" - {
    "should return the correct value" in {
      forAll(testCases) {
        case testData @ (expectedIsValid: Boolean, _: BarsAssessmentType, _: BarsAssessmentType) =>
          val response = mkResponseFromTestData.tupled(testData)

          response.isValid shouldBe expectedIsValid withClue s" - isValid was expected to to return $expectedIsValid for this test case"
      }
    }

    "test cases are exhaustive and cover all combinations" in {
      val allResponses: Seq[ValidateBankDetailsResponse] = for {
        accountNumberWithSortCodeIsValid <- Seq(Yes, No, Indeterminate)
        sortCodeIsPresentOnEISCD <- Seq(Yes, No)
      } yield {
        val unused = true

        mkResponseFromTestData(unused, accountNumberWithSortCodeIsValid, sortCodeIsPresentOnEISCD)
      }

      val testDataResponses = testCases.map{
        case testData @ (_: Boolean, _: BarsAssessmentType, _: BarsAssessmentType) =>
          mkResponseFromTestData.tupled(testData)
      }

      allResponses.toSet shouldBe testDataResponses.toSet withClue " - the test data does not cover all possible combinations of responses"
    }
  }

}
