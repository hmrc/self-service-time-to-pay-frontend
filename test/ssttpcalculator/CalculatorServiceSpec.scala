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

import java.time.{Clock, LocalDate, LocalDateTime}
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

import org.scalatest.{Matchers, WordSpec}
import ssttpcalculator.CalculatorService._
import testsupport.{DateSupport, ItSpec}
import ssttpcalculator.model.{CalculatorInput, DebitInput}

import scala.math.BigDecimal

class CalculatorServiceSpec extends WordSpec with Matchers with DateSupport {
  private def date(month: Int, day: Int): LocalDate = LocalDate.of(_2020, month, day)
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
    val currentDateTime = LocalDateTime.parse(s"${_2020}-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  "payTodayRequest" should {
    "return a payment schedule request" when {
      "the current date is the 1st" in {
        val clock = clockForMay(_1st)

        makeCalculatorInputForPayToday(debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }
    }

    "the current date is the 28th" in {
      val clock = clockForMay(_28th)

      makeCalculatorInputForPayToday(debits)(clock) shouldBe CalculatorInput(
        debits, noInitialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
    }

    "the current date is the 29th" in {
      val clock = clockForMay(_29th)

      makeCalculatorInputForPayToday(debits)(clock) shouldBe CalculatorInput(
        debits, noInitialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
    }
  }

  "paymentScheduleRequest" should {
    "return a payment schedule request without an initial payment" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_11th))

        makeCalculatorInput(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_1st), firstPaymentDate)
        makeCalculatorInput(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_15th))

        makeCalculatorInput(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_7th), firstPaymentDate)
        makeCalculatorInput(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_15th))

        makeCalculatorInput(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_8th), firstPaymentDate)
        makeCalculatorInput(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(may(_18th))

        makeCalculatorInput(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_11th), firstPaymentDate)
        makeCalculatorInput(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
      }

      "the current date is the Monday 25th May so the payment dates roll into the next month" in {
        val clock = clockForMay(_25th)
        val currentDate = may(_25th)
        val firstPaymentDate = june(_1st)

        makeCalculatorInput(debits, noInitialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = june(_25th), firstPaymentDate = Some(firstPaymentDate))
        makeCalculatorInput(debits, noInitialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_25th), firstPaymentDate = Some(firstPaymentDate))
      }
    }

    "return a payment schedule request with an initial payment" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_11th))

        makeCalculatorInput(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
        makeCalculatorInput(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        makeCalculatorInput(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
        makeCalculatorInput(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        makeCalculatorInput(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
        makeCalculatorInput(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_18th))

        makeCalculatorInput(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
        makeCalculatorInput(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
      }

      "the current date is the Monday 25th May so the payment dates roll into the next month" in {
        val clock = clockForMay(_25th)
        val currentDate = may(_25th)
        val firstPaymentDate = july(_1st)

        makeCalculatorInput(debits, initialPayment, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = july(_25th), firstPaymentDate = Some(firstPaymentDate))
        makeCalculatorInput(debits, initialPayment, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, initialPayment, currentDate, endDate = august(_25th), firstPaymentDate = Some(firstPaymentDate))
      }
    }

    "return a payment schedule request with no initial payment when the user tries to make a payment which would leave less than £32 balance" when {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_11th))

        makeCalculatorInput(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
        makeCalculatorInput(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        makeCalculatorInput(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
        makeCalculatorInput(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_15th))

        makeCalculatorInput(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
        makeCalculatorInput(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val currentDate = LocalDate.now(clock)
        val firstPaymentDate = Some(june(_18th))

        makeCalculatorInput(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
        makeCalculatorInput(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
      }

      "the current date is the Monday 25th May so the payment dates roll into the next month" in {
        val clock = clockForMay(_25th)
        val currentDate = may(_25th)
        val firstPaymentDate = july(_1st)

        makeCalculatorInput(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = july(_25th), firstPaymentDate = Some(firstPaymentDate))
        makeCalculatorInput(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, currentDate, endDate = august(_25th), firstPaymentDate = Some(firstPaymentDate))
      }
    }
  }

  "changeScheduleRequest with zero duration and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changeCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changeCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_23rd), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changeCalculatorInput(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_21st), endDate = may(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(zeroDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(zeroDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = june(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "changeScheduleRequest with a duration of one month and no initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _1st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_1st), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changeCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_29th), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changeCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_23rd), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changeCalculatorInput(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_21st), endDate = june(_27th), firstPaymentDate = Some(may(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(oneMonthDuration, _21st, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _3rd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(oneMonthDuration, _22nd, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _4th, noInitialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_3rd), firstPaymentDate = Some(june(_4th)))
      }
    }
  }

  "changeScheduleRequest with zero duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_31st), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changeCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_29th), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changeCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_23rd), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changeCalculatorInput(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_21st), endDate = june(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_30th), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = june(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = june(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(zeroDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }

  "changeScheduleRequest with a months duration and an initial payment" should {
    "return a payment schedule request" when {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _1st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_30th), firstPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changeCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changeCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_29th), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changeCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_23rd), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changeCalculatorInput(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_21st), endDate = july(_27th), firstPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changeCalculatorInput(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_31st), firstPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = july(_20th), firstPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _3rd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = august(_2nd), firstPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changeCalculatorInput(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_15th), endDate = july(_21st), firstPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changeCalculatorInput(oneMonthDuration, _4th, initialPayment, debits)(clock) shouldBe CalculatorInput(
          debits, initialPayment, startDate = may(_28th), endDate = august(_3rd), firstPaymentDate = Some(july(_4th)))
      }
    }
  }
}
