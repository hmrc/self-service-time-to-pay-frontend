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

package ssttpcalculator.model

import config.AppConfig
import journey.PaymentToday
import org.scalatest.matchers.should.Matchers
import ssttpcalculator.CalculatorService
import testsupport.{DateSupport, ItSpec}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.{Clock, LocalDate, LocalDateTime, Month}
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

class TaxPaymentPlanSpec extends ItSpec with Matchers with DateSupport {

  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  private def date(month: Int, day: Int): LocalDate = LocalDate.of(_2020, month, day)

  private def may(day: Int): LocalDate = date(may, day)

  private def june(day: Int): LocalDate = date(june, day)

  private def july(day: Int): LocalDate = date(july, day)

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"${_2020}-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  def date(date: String): LocalDate = LocalDate.parse(date)

  "A TaxPaymentPlan" - {

    val liabilities = List(TaxLiability(BigDecimal(3559.20), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JULY, 31)))

    val upfrontPayment = BigDecimal(4000.00)

    val regularPaymentAmount = 500

    "when the plan has an initial payment and has not provide an initial payment date should" - {

      val plan = TaxPaymentPlan(
        liabilities,
        upfrontPayment,
        LocalDate.of(2021, Month.OCTOBER, 28)
      )(appConfig)
      "remove liabilities covered by the initial payment" in {
        plan.outstandingLiabilities.size shouldBe 2
      }
      "have remaining liability with a value equal to the value of the initial liabilities less the initial payment" in {
        plan.outstandingLiabilities.map(_.amount).sum shouldBe plan.remainingLiability
      }
      "have an actual start date equal to the start date" in {
        plan.actualStartDate shouldBe plan.planStartDate
      }
      "leave any due date occurring more than one week after the initial payment date unmodified" in {
        plan.outstandingLiabilities.map(_.dueDate) shouldBe List(LocalDate.of(2022, Month.JANUARY, 31), LocalDate.of(2022, Month.JULY, 31))
      }
    }

    "TaxPaymentPlan should" - {
      "include correct payments calendar information" - {
        "when the current date is the 1st" in {
          val clock = clockForMay(_1st)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(today.getDayOfMonth)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _1st
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_1st)
        }
      }

      "when the current date is the 28th" in {
        val clock = clockForMay(_28th)
        val today = LocalDate.now(clock)

        val taxPaymentPlan = TaxPaymentPlan(
          taxLiabilities             = liabilities,
          upfrontPayment             = upfrontPayment,
          planStartDate              = today,
          maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(today.getDayOfMonth)),
          regularPaymentAmount       = regularPaymentAmount,
          maybePaymentToday          = None
        )(appConfig)

        taxPaymentPlan.planStartDate shouldBe today
        taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
        taxPaymentPlan.regularPaymentsDay shouldBe _28th
        taxPaymentPlan.regularPaymentDates.head shouldBe june(_28th)
      }

      "when the current date is the 29th" in {
        val clock = clockForMay(_29th)
        val today = LocalDate.now(clock)

        val taxPaymentPlan = TaxPaymentPlan(
          taxLiabilities = liabilities,
          upfrontPayment = upfrontPayment,
          planStartDate  = today,
          Some(ArrangementDayOfMonth(today.getDayOfMonth)),
          regularPaymentAmount,
          None
        )(appConfig)

        taxPaymentPlan.planStartDate shouldBe today
        taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
        taxPaymentPlan.regularPaymentsDay shouldBe _1st
        taxPaymentPlan.regularPaymentDates.head shouldBe july(_1st)
      }
      "without an initial payment when" - {
        "the current date is Friday 1st May with upcoming bank holiday" in {
          val clock = clockForMay(_1st)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_12th)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _12th
          taxPaymentPlan.regularPaymentDates.head shouldBe may(_12th)
        }

        "the current date is Thursday 7th May with upcoming bank holiday" in {
          val clock = clockForMay(_7th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_15th)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _15th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_15th)

        }

        "the current date is bank holiday Friday 8th May" in {
          val clock = clockForMay(_8th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_15th)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _15th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_15th)
        }

        "the current date is Monday 11th May" in {
          val clock = clockForMay(_11th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_18th)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _18th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_18th)
        }

        "the current date is the Monday 25th May so the payment dates roll into the next month" in {
          val clock = clockForMay(_25th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_1st)),
            regularPaymentAmount,
            None
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe None
          taxPaymentPlan.regularPaymentsDay shouldBe _1st
          taxPaymentPlan.regularPaymentDates.head shouldBe july(_1st)
        }
      }
      "return a payment schedule request with an initial payment when" - {
        "the current date is Friday 1st May with upcoming bank holiday" in {
          val clock = clockForMay(_1st)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_12th)),
            regularPaymentAmount,
            Some(PaymentToday(true))

          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe Some(may(_12th))
          taxPaymentPlan.regularPaymentsDay shouldBe _12th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_12th)
        }

        "the current date is Thursday 7th May with upcoming bank holiday" in {
          val clock = clockForMay(_7th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_15th)),
            regularPaymentAmount,
            Some(PaymentToday(true))
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe Some(may(_18th))
          taxPaymentPlan.regularPaymentsDay shouldBe _15th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_15th)
        }

        "the current date is bank holiday Friday 8th May" in {
          val clock = clockForMay(_8th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_15th)),
            regularPaymentAmount,
            Some(PaymentToday(true))
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe Some(may(_19th))
          taxPaymentPlan.regularPaymentsDay shouldBe _15th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_15th)
        }

        "the current date is Monday 11th May" in {
          val clock = clockForMay(_11th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_18th)),
            regularPaymentAmount,
            Some(PaymentToday(true))
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe Some(may(_22nd))
          taxPaymentPlan.regularPaymentsDay shouldBe _18th
          taxPaymentPlan.regularPaymentDates.head shouldBe june(_18th)
        }

        "the current date is the Monday 25th May so the payment dates roll into the next month" in {
          val clock = clockForMay(_25th)
          val today = LocalDate.now(clock)

          val taxPaymentPlan = TaxPaymentPlan(
            taxLiabilities = liabilities,
            upfrontPayment = upfrontPayment,
            planStartDate  = today,
            Some(ArrangementDayOfMonth(_1st)),
            regularPaymentAmount,
            Some(PaymentToday(true))
          )(appConfig)

          taxPaymentPlan.planStartDate shouldBe today
          taxPaymentPlan.maybeUpfrontPaymentDate shouldBe Some(june(_5th))
          taxPaymentPlan.regularPaymentsDay shouldBe _1st
          taxPaymentPlan.regularPaymentDates.head shouldBe july(_1st)
        }
      }
    }
  }
}
