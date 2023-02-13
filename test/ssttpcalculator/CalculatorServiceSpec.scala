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

import config.AppConfig
import org.scalatest.matchers.should.Matchers
import ssttpcalculator.model.{PaymentSchedule, TaxLiability, TaxPaymentPlan}
import testsupport.{DateSupport, ItSpec}

import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDate, LocalDateTime}

class CalculatorServiceSpec extends ItSpec with Matchers with DateSupport {
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
  private val debits = Seq(TaxLiability(debt, july(_31st)))
  private val noInitialPayment = BigDecimal(0)
  private val initialPayment = BigDecimal(debt - minimumBalanceAfterInitialPayment)
  private val initialPaymentTooLarge = BigDecimal(468.01)

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"${_2020}-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  "return a payment schedule request with no initial payment when the user tries to make a payment which would leave less than £32 balance when" - {
    "the current date is Friday 1st May with upcoming bank holiday" in {
      val clock = clockForMay(_1st)
      val currentDate = LocalDate.now(clock)
      val firstPaymentDate = Some(june(_11th))

      makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
      makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
    }

    "the current date is Thursday 7th May with upcoming bank holiday" in {
      val clock = clockForMay(_7th)
      val currentDate = LocalDate.now(clock)
      val firstPaymentDate = Some(june(_15th))

      makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
      makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
    }

    "the current date is bank holiday Friday 8th May" in {
      val clock = clockForMay(_8th)
      val currentDate = LocalDate.now(clock)
      val firstPaymentDate = Some(june(_15th))

      makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
      makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
    }

    "the current date is Monday 11th May" in {
      val clock = clockForMay(_11th)
      val currentDate = LocalDate.now(clock)
      val firstPaymentDate = Some(june(_18th))

      makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
      makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
    }

    "the current date is the Monday 25th May so the payment dates roll into the next month" in {
      val clock = clockForMay(_25th)
      val currentDate = may(_25th)
      val firstPaymentDate = july(_1st)

      makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = july(_25th), firstRegularPaymentDate = Some(firstPaymentDate))
      makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, endDate = august(_25th), firstRegularPaymentDate = Some(firstPaymentDate))
    }
  }

  "changeScheduleRequest with zero duration and no initial payment should" - {
    "return a payment schedule request when" - {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _1st, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_1st), endDate = may(_31st), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstRegularPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changePaymentPlan(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_29th), endDate = june(_30th), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changePaymentPlan(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_23rd), endDate = june(_30th), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in 10 days time" in {
        val clock = clockForMay(_21st)

        changePaymentPlan(zeroDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_21st), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in 10 days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(zeroDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_22nd), endDate = may(_31st), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than 10 days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(zeroDuration, _21st, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than 10 days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _3rd, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_2nd), firstRegularPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is 10 days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(zeroDuration, _22nd, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_15th), endDate = june(_21st), firstRegularPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is 10 days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _4th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_3rd), firstRegularPaymentDate = Some(july(_4th)))
      }
    }
  }

  "changeScheduleRequest with a duration of one month and no initial payment should" - {
    "return a payment schedule request when" - {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _1st, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_1st), endDate = june(_30th), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstRegularPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changePaymentPlan(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_29th), endDate = july(_31st), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than 10 days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_22nd), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than 10 days time" in {
        val clock = clockForMay(_23rd)

        changePaymentPlan(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_23rd), endDate = july(_31st), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in 10 days time" in {
        val clock = clockForMay(_21st)

        changePaymentPlan(oneMonthDuration, _28th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_21st), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in 10 days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(oneMonthDuration, _29th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_22nd), endDate = june(_30th), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is less than 10 days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(oneMonthDuration, _21st, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_15th), endDate = july(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than 10 days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _3rd, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = august(_2nd), firstRegularPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is 10 days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(oneMonthDuration, _22nd, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_15th), endDate = july(_21st), firstRegularPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is 10 days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _4th, noInitialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, noInitialPayment, startDate = may(_28th), endDate = august(_3rd), firstRegularPaymentDate = Some(july(_4th)))
      }
    }
  }

  "changeScheduleRequest with zero duration and an initial payment should" - {
    "return a payment schedule request when" - {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _1st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = may(_31st), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = june(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = may(_21st), firstRegularPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changePaymentPlan(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_29th), endDate = june(_30th), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changePaymentPlan(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_23rd), endDate = june(_30th), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changePaymentPlan(zeroDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_21st), endDate = june(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(zeroDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_22nd), endDate = june(_30th), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(zeroDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_15th), endDate = june(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _3rd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = july(_2nd), firstRegularPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(zeroDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_15th), endDate = june(_21st), firstRegularPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(zeroDuration, _4th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = july(_3rd), firstRegularPaymentDate = Some(july(_4th)))
      }
    }
  }

  "changeScheduleRequest with a months duration and an initial payment should" - {
    "return a payment schedule request when" - {
      "the required day of the month and the current date are the 1st" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _1st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = june(_30th), firstRegularPaymentDate = Some(june(_1st)))
      }

      "the required day of the month is 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = july(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is more than 20 days after the current date" in {
        val clock = clockForMay(_1st)

        changePaymentPlan(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_1st), endDate = june(_21st), firstRegularPaymentDate = Some(may(_22nd)))
      }

      "the required day of the month and the current date are the 28th" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month and the current date are the 29th" in {
        val clock = clockForMay(_29th)

        changePaymentPlan(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_29th), endDate = july(_31st), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in less than seven days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in less than seven days time" in {
        val clock = clockForMay(_23rd)

        changePaymentPlan(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_23rd), endDate = july(_31st), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is the 28th which is in seven days time" in {
        val clock = clockForMay(_21st)

        changePaymentPlan(oneMonthDuration, _28th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_21st), endDate = july(_27th), firstRegularPaymentDate = Some(june(_28th)))
      }

      "the required day of the month is the 29th which is in seven days time" in {
        val clock = clockForMay(_22nd)

        changePaymentPlan(oneMonthDuration, _29th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_22nd), endDate = july(_31st), firstRegularPaymentDate = Some(july(_1st)))
      }

      "the required day of the month is less than seven days from the current date and in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(oneMonthDuration, _21st, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_15th), endDate = july(_20th), firstRegularPaymentDate = Some(june(_21st)))
      }

      "the required day of the month is less than seven days from the current date and in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _3rd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = august(_2nd), firstRegularPaymentDate = Some(july(_3rd)))
      }

      "the required day of the month is seven days or more from the current date in the same month" in {
        val clock = clockForMay(_15th)

        changePaymentPlan(oneMonthDuration, _22nd, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_15th), endDate = july(_21st), firstRegularPaymentDate = Some(june(_22nd)))
      }

      "the required day of the month is seven days or more from the current date in the next month" in {
        val clock = clockForMay(_28th)

        changePaymentPlan(oneMonthDuration, _4th, initialPayment, debits)(clock) shouldBe TaxPaymentPlan(
          debits, initialPayment, startDate = may(_28th), endDate = august(_3rd), firstRegularPaymentDate = Some(july(_4th)))
      }
    }
  }

  "availablePaymentSchedules with an endDate should" - {
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    val LastPaymentDelayDays = 7

    "return a payment schedule with endDate when" - {
      "matching last payment date plush seven days" in {
        val startDate = LocalDate.now
        val endDate = startDate.plusMonths(3)

        val taxPaymentPlan = TaxPaymentPlan(
          taxLiabilities             = Seq(TaxLiability(1000, startDate)),
          upfrontPayment             = 0,
          planStartDate              = startDate,
          maybeArrangementDayOfMonth = None,
          regularPaymentAmount       = 500,
          maybePaymentToday          = None
        )(appConfig)

        val result: PaymentSchedule = calculatorService.schedule(taxPaymentPlan).get

        result.endDate shouldBe result.instalments.last.paymentDate.plusDays(LastPaymentDelayDays)
      }

      "ignoring public holidays,  last payment date plush seven days" in {
        val startDate = LocalDate.of(2022, 6, 1)
        val endDate = startDate.plusMonths(6)

        val taxPaymentPlan = TaxPaymentPlan(
          taxLiabilities             = Seq(TaxLiability(1000, startDate)),
          upfrontPayment             = 0,
          planStartDate              = startDate,
          maybeArrangementDayOfMonth = None,
          regularPaymentAmount       = 200,
          maybePaymentToday          = None
        )(appConfig)

        val result: PaymentSchedule = calculatorService.schedule(taxPaymentPlan).get

        result.endDate shouldBe result.instalments.last.paymentDate.plusDays(LastPaymentDelayDays)
      }
    }
  }
}
