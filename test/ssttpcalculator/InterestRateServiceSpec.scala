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
import uk.gov.hmrc.http.SessionKeys

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, LocalDateTime, LocalTime, Month, ZoneId, ZoneOffset}

class InterestRateServiceSpec extends AnyWordSpec with Matchers {
  val clock = new ClockProvider
//  val clock = Clock.fixed(LocalDate.parse("2020-02-05").atStartOfDay(ZoneId.of("Europe/London")).toInstant, ZoneId.of("Europe/London"))
  val irc = new InterestRateService(clock)


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
    "passed SA details and an implicit request" should {
      " lists all interest rates for the period" +
        " from the due date of the oldest debit in the SA details" +
        " to today" in {
        import testsupport.testdata.TdAll
        import play.api.test.FakeRequest

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        irc.applicableInterestRates(TdAll.taxpayer.selfAssessment)(
          FakeRequest().withSession("ssttp.frozenDateTime" ->  LocalDateTime.parse("2020-11-16T02:27:39", formatter).toInstant(ZoneOffset.UTC).toString)
        ) shouldBe Seq()

      }
    }
  }

}
