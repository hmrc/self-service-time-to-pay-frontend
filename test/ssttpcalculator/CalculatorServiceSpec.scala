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

import scala.math.BigDecimal

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
  private val _5th = 5
  private val _7th = 7
  private val _8th = 8
  private val _11th = 11
  private val _15th = 15
  private val _18th = 18
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
  private val twoMonthDuration = 2

  private val debt = 500
  private val minimumBalanceAfterInitialPayment = 32
  private val debits = Seq(DebitInput(debt, july(_31st)))
  private val noInitialPayment = BigDecimal(0)
  private val initialPayment = BigDecimal(debt - minimumBalanceAfterInitialPayment)
  private val initialPaymentTooLarge = BigDecimal(468.01)

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"$year-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  "payTodayRequest" should {
    "return a payment schedule request" when {
      "the current date is the 1st" in {
        val clock = clockForMay(_1st)

        payTodayRequest(debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }
    }

    "the current date is the 28th" in {
      val clock = clockForMay(_28th)

      payTodayRequest(debits)(clock) shouldBe CalculatorInput(
        debits, noInitialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
    }

    "the current date is the 29th" in {
      val clock = clockForMay(_29th)

      payTodayRequest(debits)(clock) shouldBe CalculatorInput(
        debits, noInitialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
    }
  }

  "instalmentsScheduleRequest" should {
    "return a payment schedule request without an initial payment" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_11th))

        instalmentsScheduleRequest(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_1st), firstPaymentDate)
        instalmentsScheduleRequest(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_15th))

        instalmentsScheduleRequest(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_7th), firstPaymentDate)
        instalmentsScheduleRequest(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_15th))

        instalmentsScheduleRequest(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_8th), firstPaymentDate)
        instalmentsScheduleRequest(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_18th))

        instalmentsScheduleRequest(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_11th), firstPaymentDate)
        instalmentsScheduleRequest(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
      }

      "the current date is the Friday 29th May" in {
        val clock = clockForMay(_29th)
        val currentDate = may(_29th)
        val firstPaymentDate = june(_5th)

        instalmentsScheduleRequest(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_29th), firstPaymentDate = Some(firstPaymentDate))
        instalmentsScheduleRequest(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_29th), firstPaymentDate = Some(firstPaymentDate))
      }
    }

    "return a payment schedule request with an initial payment" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_11th))

        instalmentsScheduleRequest(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        instalmentsScheduleRequest(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        instalmentsScheduleRequest(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_18th))

        instalmentsScheduleRequest(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
      }

      "the current date is the Friday 29th May" in {
        val clock = clockForMay(_29th)
        val currentDate = may(_29th)
        val firstPaymentDate = july(_5th)

        instalmentsScheduleRequest(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_29th), firstPaymentDate = Some(firstPaymentDate))
        instalmentsScheduleRequest(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_29th), firstPaymentDate = Some(firstPaymentDate))
      }
    }

    "return a payment schedule request with no initial payment when the user tries to make a payment which would leave less than £32 balance" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_11th))

        instalmentsScheduleRequest(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        instalmentsScheduleRequest(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        instalmentsScheduleRequest(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_18th))

        instalmentsScheduleRequest(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
        instalmentsScheduleRequest(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
      }

      "the current date is the Friday 29th May" in {
        val clock = clockForMay(_29th)
        val currentDate = may(_29th)
        val firstPaymentDate = july(_5th)

        instalmentsScheduleRequest(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_29th), firstPaymentDate = Some(firstPaymentDate))
        instalmentsScheduleRequest(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_29th), firstPaymentDate = Some(firstPaymentDate))
      }
    }
  }

  "calculatorInput with zero duration and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_23rd), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_21st), endDate = may(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = june(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "calculatorInput with a duration of one month and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_29th), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_23rd), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_21st), endDate = june(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "calculatorInput with zero duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_23rd), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_21st), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = june(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(zeroDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }

  "calculatorInput with a months duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_29th), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_23rd), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        calculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_21st), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        calculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        calculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = july(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        calculatorInput(oneMonthDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = august(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }
}
