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
import ssttpcalculator.model.InterestRate
import times.ClockProvider

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Month}


class InterestRateServiceSpec extends AnyWordSpec with Matchers {
  val irc = new InterestRateService(new ClockProvider)

  "the interest rate calculator" when {
    "the interest rates are up to date" should {
      "provide the latest rate" in {
        val ir = irc.rateOn(LocalDate.of(2022, Month.APRIL, 6))
        ir.rate shouldBe BigDecimal("3.25")
      }
      "provide the previous rate" in {
        val ir = irc.rateOn(LocalDate.of(2022, Month.FEBRUARY, 21))
        ir.rate shouldBe BigDecimal("3.00")
      }
    }
  }

  "applicableInterestRates" when {
    import testsupport.testdata.TdAll
    import play.api.test.FakeRequest

    "passed SA details and an implicit request" should {
      "lists all interest rates for the period" +
        " from the due date of the oldest debit in the SA details" +
        " to today" in {
        irc.applicableInterestRates(TdAll.taxpayer.selfAssessment)(
          FakeRequest().withSession("ssttp.frozenDateTime" ->  "2020-02-05T00:00:00.880")
        ) shouldBe Seq(
          InterestRate(LocalDate.parse("2019-11-25"), LocalDate.parse("2019-12-31"), 3.25),
          InterestRate(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-02-05"), 3.25)
        )

      }
    }
    "passed SA details with all charges not yet due (today's date is before earliest due date)" should {
      "list a single interest rate for the period only covering today (from today, to today)" in {
        irc.applicableInterestRates(TdAll.taxpayer.selfAssessment)(
          FakeRequest().withSession("ssttp.frozenDateTime" -> "2019-11-24T00:00:00.880")
        ) shouldBe Seq(
          InterestRate(LocalDate.parse("2019-11-24"), LocalDate.parse("2019-11-24"), 3.25),
        )

      }
    }
  }

}
