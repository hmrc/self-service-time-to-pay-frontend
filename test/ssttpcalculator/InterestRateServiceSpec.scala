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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import times.ClockProvider

import java.time.{LocalDate, Month}

class InterestRateServiceSpec extends AnyWordSpec with Matchers {
  val clock = new ClockProvider

  "the interest rate calculator" when {
    "the interest rates are up to date" should {
      "provide the latest rate" in {
        val irc = new InterestRateService(clock)
        val ir = irc.rateOn(LocalDate.of(2022, Month.APRIL, 6))
        ir.rate shouldBe BigDecimal("3.25")
      }
      "provide the previous rate" in {
        val irc = new InterestRateService(clock)
        val ir = irc.rateOn(LocalDate.of(2022, Month.FEBRUARY, 21))
        ir.rate shouldBe BigDecimal("3.00")
      }
    }
  }

}
