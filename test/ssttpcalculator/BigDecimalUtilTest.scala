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

package ssttpcalculator

import testsupport.UnitSpec

class BigDecimalUtilTest extends UnitSpec {

  "test rounding" in {
    testCases.foreach { tc =>
      BigDecimalUtil.roundDownToNearestTen(tc.bigDecimal) shouldBe tc.roundedDown
      BigDecimalUtil.roundUpToNearestTen(tc.bigDecimal) shouldBe tc.roundedUp
    }
  }

  lazy val testCases: List[TestCase] = List(
    TestCase(bigDecimal  = "0.000000001", roundedUp = "10.0", roundedDown = "0.0"),
    TestCase(bigDecimal  = "15.000000001", roundedUp = "20.0", roundedDown = "10.0"),
    TestCase(bigDecimal  = "10.000000001", roundedUp = "20.0", roundedDown = "10.0"),
    TestCase(bigDecimal  = "100000000000010.000000001", roundedUp = "100000000000020.0", roundedDown = "100000000000010.0")
  )

  case class TestCase(
      bigDecimal:  BigDecimal,
      roundedUp:   BigDecimal,
      roundedDown: BigDecimal
  )

  implicit def toBigDecimal(s: String): BigDecimal = BigDecimal(s)
}
