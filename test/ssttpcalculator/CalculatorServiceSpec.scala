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

  private val zeroDuration = 0
  private val oneMonthDuration = 1
  private val twoMonthDuration = 2

  private val debt = 500
  private val minimumBalanceAfterInitialPayment = 32
  private val debits = Seq(TaxLiability(debt, july(_31st)))
  private val noInitialPayment = BigDecimal(0)
  private val initialPayment = BigDecimal(debt - minimumBalanceAfterInitialPayment)
  private val initialPaymentTooLarge = BigDecimal(468.01)
  private val regularPaymentAmount = 500

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

      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
    }

    "the current date is Thursday 7th May with upcoming bank holiday" in {
      val clock = clockForMay(_7th)
      val currentDate = LocalDate.now(clock)

      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
    }

    "the current date is bank holiday Friday 8th May" in {
      val clock = clockForMay(_8th)
      val currentDate = LocalDate.now(clock)

      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
    }

    "the current date is Monday 11th May" in {
      val clock = clockForMay(_11th)
      val currentDate = LocalDate.now(clock)

      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
    }

    "the current date is the Monday 25th May so the payment dates roll into the next month" in {
      val clock = clockForMay(_25th)
      val currentDate = may(_25th)

      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
      TaxPaymentPlan.safeNew(debits, initialPaymentTooLarge, regularPaymentAmount)(clock, appConfig) shouldBe TaxPaymentPlan(
        debits, noInitialPayment, currentDate, regularPaymentAmount)(appConfig)
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
