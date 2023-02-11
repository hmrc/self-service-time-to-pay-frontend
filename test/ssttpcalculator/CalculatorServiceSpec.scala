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
import journey.PaymentToday
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ssttpcalculator.model.{PaymentSchedule, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import testsupport.{DateSupport, ItSpec}
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

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

  val paymentDatesService: PaymentDatesService = fakeApplication().injector.instanceOf[PaymentDatesService]
  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  "paymentDatesService.paymentsCalendar should" - {
    "return a payments calendar" - {
      "when the current date is the 1st" in {
        val clock = clockForMay(_1st)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(today.getDayOfMonth)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _1st
        paymentsCalendar.regularPaymentDates.head shouldBe june(_1st)
      }
    }

    "when the current date is the 28th" in {
      val clock = clockForMay(_28th)
      val today = LocalDate.now(clock)

      val paymentsCalendar = paymentDatesService.paymentsCalendar(
        maybePaymentToday          = None,
        maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(today.getDayOfMonth)),
        dateToday                  = today
      )(appConfig)

      paymentsCalendar.planStartDate shouldBe today
      paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
      paymentsCalendar.regularPaymentsDay shouldBe _28th
      paymentsCalendar.regularPaymentDates.head shouldBe june(_28th)
    }

    "when the current date is the 29th" in {
      val clock = clockForMay(_29th)
      val today = LocalDate.now(clock)

      val paymentsCalendar = paymentDatesService.paymentsCalendar(
        maybePaymentToday          = None,
        maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(today.getDayOfMonth)),
        dateToday                  = today
      )(appConfig)

      paymentsCalendar.planStartDate shouldBe today
      paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
      paymentsCalendar.regularPaymentsDay shouldBe _1st
      paymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
    }
    "without an initial payment when" - {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_12th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _12th
        paymentsCalendar.regularPaymentDates.head shouldBe may(_12th)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_15th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _15th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_15th)

      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_15th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _15th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_18th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _18th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_18th)
      }

      "the current date is the Monday 25th May so the payment dates roll into the next month" in {
        val clock = clockForMay(_25th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = None,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_1st)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe None
        paymentsCalendar.regularPaymentsDay shouldBe _1st
        paymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
      }
    }
    "return a payment schedule request with an initial payment when" - {
      "the current date is Friday 1st May with upcoming bank holiday" in {
        val clock = clockForMay(_1st)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = Some(PaymentToday(true)),
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_12th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_12th))
        paymentsCalendar.regularPaymentsDay shouldBe _12th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_12th)
      }

      "the current date is Thursday 7th May with upcoming bank holiday" in {
        val clock = clockForMay(_7th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = Some(PaymentToday(true)),
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_15th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_18th))
        paymentsCalendar.regularPaymentsDay shouldBe _15th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
      }

      "the current date is bank holiday Friday 8th May" in {
        val clock = clockForMay(_8th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = Some(PaymentToday(true)),
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_15th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_19th))
        paymentsCalendar.regularPaymentsDay shouldBe _15th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
      }

      "the current date is Monday 11th May" in {
        val clock = clockForMay(_11th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = Some(PaymentToday(true)),
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_18th)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_22nd))
        paymentsCalendar.regularPaymentsDay shouldBe _18th
        paymentsCalendar.regularPaymentDates.head shouldBe june(_18th)
      }

      "the current date is the Monday 25th May so the payment dates roll into the next month" in {
        val clock = clockForMay(_25th)
        val today = LocalDate.now(clock)

        val paymentsCalendar = paymentDatesService.paymentsCalendar(
          maybePaymentToday          = Some(PaymentToday(true)),
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_1st)),
          dateToday                  = today
        )(appConfig)

        paymentsCalendar.planStartDate shouldBe today
        paymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(june(_5th))
        paymentsCalendar.regularPaymentsDay shouldBe _1st
        paymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
      }
    }
  }
}
