/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{Clock, LocalDate, LocalDateTime}
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

import org.scalatest.{Matchers, WordSpec}
import ssttpcalculator.CalculatorService.createCalculatorInput
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput}

class CalculatorServiceSpec extends WordSpec with Matchers {
  private val year = 2020

  private val may = 5
  private val june = 6
  private val july = 7
  private val august = 8

  private val _1st = 1
  private val _2nd = 2
  private val _3rd = 3
  private val _4th = 4
  private val _15th = 15
  private val _20th = 20
  private val _21st = 21
  private val _22nd = 22
  private val _23rd = 23
  private val _27th = 27
  private val _28th = 28
  private val _29th = 29
  private val _30th = 30
  private val _31st = 31

  private def date(month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
  private def may(day: Int): LocalDate = date(may, day)
  private def june(day: Int): LocalDate = date(june, day)
  private def july(day: Int): LocalDate = date(july, day)
  private def august(day: Int): LocalDate = date(august, day)

  private val may21st = may(_21st)
  private val may22nd = may(_22nd)
  private val may27th = may(_27th)
  private val may28th = may(_28th)
  private val may31st = may(_31st)
  private val june1st = june(_1st)
  private val june3rd = june(_3rd)
  private val june4th = june(_4th)
  private val june20th = june(_20th)
  private val june21st = june(_21st)
  private val june22nd = june(_22nd)
  private val june27th = june(_27th)
  private val june28th = june(_28th)
  private val june30th = june(_30th)
  private val july1st = july(_1st)
  private val july2nd = july(_2nd)
  private val july3rd = july(_3rd)
  private val july4th = july(_4th)
  private val july20th = july(_20th)
  private val july21st = july(_21st)
  private val july27th = july(_27th)
  private val july31st = july(_31st)
  private val august2nd = august(_2nd)
  private val august3rd = august(_3rd)

  private val zeroDuration = 0
  private val oneMonthDuration = 1

  private val debt = 500
  private val debits = Seq(DebitInput(debt, july31st))
  private val noInitialPayment = BigDecimal(0)
  private val initialPayment = BigDecimal(1)

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"$year-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  "createCalculatorInput with zero duration and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may31st, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        createCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        createCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may31st, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        createCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may27th, firstPaymentDate = Some(may28th))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may31st, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(zeroDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july2nd, firstPaymentDate = Some(july3rd))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(zeroDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june3rd, firstPaymentDate = Some(june4th))
      }
    }
  }

  "createCalculatorInput with a duration of one month and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        createCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july31st, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        createCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        createCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(may28th))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(oneMonthDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = august2nd, firstPaymentDate = Some(july3rd))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(oneMonthDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july3rd, firstPaymentDate = Some(june4th))
      }
    }
  }

  "createCalculatorInput with zero duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may31st, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        createCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        createCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        createCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july2nd, firstPaymentDate = Some(july3rd))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june21st, firstPaymentDate = Some(june22nd))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(zeroDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july3rd, firstPaymentDate = Some(july4th))
      }
    }
  }

  "createCalculatorInput with a months duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june30th, firstPaymentDate = Some(june1st))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        createCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june21st, firstPaymentDate = Some(may22nd))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        createCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july31st, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        createCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july31st, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        createCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july27th, firstPaymentDate = Some(june28th))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        createCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july31st, firstPaymentDate = Some(july1st))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july20th, firstPaymentDate = Some(june21st))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = august2nd, firstPaymentDate = Some(july3rd))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        createCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july21st, firstPaymentDate = Some(june22nd))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        createCalculatorInput(oneMonthDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = august3rd, firstPaymentDate = Some(july4th))
      }
    }
  }
}
