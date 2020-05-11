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
import ssttpcalculator.CalculatorService._
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

  private val zeroDuration = 0
  private val oneMonthDuration = 1

  private val debt = 500
  private val debits = Seq(DebitInput(debt, july(_31st)))
  private val noInitialPayment = BigDecimal(0)
  private val initialPayment = BigDecimal(1)

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"$year-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  "calculatorInput with zero duration and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        val expectedResult = CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))

        calculatorInput(zeroDuration, _1st, noInitialPayment, debits)(clock) shouldBe expectedResult
        payTodayRequest(debits)(clock) shouldBe expectedResult
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        val expectedResult = CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe expectedResult
        payTodayRequest(debits)(clock) shouldBe expectedResult
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        val expectedResult = CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe expectedResult
        payTodayRequest(debits)(clock) shouldBe expectedResult
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "calculatorInput with a duration of one month and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = LocalDate.now(clock), endDate = july(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "calculatorInput with zero duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }

  "calculatorInput with a months duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = july(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = LocalDate.now(clock), endDate = august(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }
}
