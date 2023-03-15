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
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableFor9
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.prop.Tables._
import ssttpcalculator.PaymentPlansService
import testsupport.{DateSupport, ItSpec}
import uk.gov.hmrc.selfservicetimetopay.models.PaymentDayOfMonth

import java.time.{Clock, LocalDate, LocalDateTime, Month}
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

class PaymentsCalendarOldSpec extends ItSpec with Matchers with DateSupport {

  val paymentPlansService: PaymentPlansService = fakeApplication().injector.instanceOf[PaymentPlansService]

  val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  private def clockForMay(dayInMay: Int) = {
    val formattedDay = dayInMay.formatted("%02d")
    val currentDateTime = LocalDateTime.parse(s"${_2020}-05-${formattedDay}T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  def date(date: String): LocalDate = LocalDate.parse(date)

  def testPaymentsCalendar(testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate]): Unit = {

    forAll(testCases) { (id, caseDescription,
      inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay,
      expectedMaybeUpfrontPaymentDate, expectedPlanStartDate, expectedRegularPaymentsDay, expectedFirstRegularPaymentDay) =>
      s"$id. $caseDescription" in {

        val taxPaymentPlan = PaymentsCalendar.generate(inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay)(config)

        taxPaymentPlan.maybeUpfrontPaymentDate shouldBe expectedMaybeUpfrontPaymentDate
        taxPaymentPlan.planStartDate shouldBe expectedPlanStartDate
        taxPaymentPlan.paymentDayOfMonth shouldBe expectedRegularPaymentsDay
        taxPaymentPlan.regularPaymentDates.head shouldBe expectedFirstRegularPaymentDay
      }
    }
  }

  val upfrontPaymentAmount = BigDecimal(4000.00)

  "PaymentPlansService.schedule" - {

    val liabilities = List(TaxLiability(BigDecimal(3559.20), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JULY, 31)))

    val upfrontPaymentAmount = BigDecimal(4000.00)

    val regularPaymentAmount = 500

    val clock = clockForMay(_1st)
    val today = LocalDate.now(clock)

    val paymentsCalendar = PaymentsCalendar.generate(upfrontPaymentAmount, today)(config)

    "when passed an upfront payment amount that is more than 0 and where the remaining liabilities are more than £32 returns a schedule with" - {

      val schedule = paymentPlansService.schedule(liabilities, regularPaymentAmount, paymentsCalendar, upfrontPaymentAmount).get

      "a start date equal to the payment's calendar plan start date" in {
        schedule.startDate shouldEqual paymentsCalendar.planStartDate
      }
      "an end date that is seven days after the last payment" in {
        schedule.endDate shouldEqual schedule.instalments.last.paymentDate.plusDays(7)
      }
      "an upfront payment of that amount" in {
        schedule.initialPayment shouldEqual upfrontPaymentAmount
      }
      "a principal to pay equal to the total of the liabilities" in {
        schedule.amountToPay shouldEqual liabilities.map(_.amount).sum
      }
      "instalments covering remaining liabilities after upfront payment is taken away" in {
        schedule.instalmentBalance shouldEqual liabilities.map(_.amount).sum - upfrontPaymentAmount
      }
      "total payable equal to the principal to pay plus the total interest charged" in {
        schedule.totalPayable shouldEqual schedule.amountToPay + schedule.totalInterestCharged
      }
      "at least one instalment" in {
        schedule.instalments.length > 0 shouldBe true
      }
    }

    "PaymentsCalendar should" - {
      "1. include correct payments calendar information" - {

        val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "when the current date is the 1st",
            0, date("2020-05-01"), Some(PaymentDayOfMonth(1)),
            None, date("2020-05-01"), 1, date("2020-06-01")),

          (".2", "when the current date is the 28th",
            0, date("2020-05-28"), Some(PaymentDayOfMonth(28)),
            None, date("2020-05-28"), 28, date("2020-06-28")),

          (".3", "when the current date is the 29th",
            0, date("2020-05-29"), Some(PaymentDayOfMonth(29)),
            None, date("2020-05-29"), 1, date("2020-07-01")),
        )

        testPaymentsCalendar(testCases)
      }

      "2. without an initial payment when" - {

        val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "the current date is Friday 1st May with upcoming bank holiday",
            0, date("2020-05-01"), Some(PaymentDayOfMonth(12)),
            None, date("2020-05-01"), 12, date("2020-05-12")),

          (".2", "the current date is Thursday 7th May with upcoming bank holiday",
            0, date("2020-05-07"), Some(PaymentDayOfMonth(15)),
            None, date("2020-05-07"), 15, date("2020-06-15")),

          (".3", "the current date is bank holiday Friday 8th May",
            0, date("2020-05-08"), Some(PaymentDayOfMonth(15)),
            None, date("2020-05-08"), 15, date("2020-06-15")),

          (".4", "the current date is Monday 11th May",
            0, date("2020-05-11"), Some(PaymentDayOfMonth(18)),
            None, date("2020-05-11"), 18, date("2020-06-18")),

          (".5", "the current date is the Monday 25th May so the payment dates roll into the next month",
            0, date("2020-05-25"), Some(PaymentDayOfMonth(1)),
            None, date("2020-05-25"), 1, date("2020-07-01")),
        )

        testPaymentsCalendar(testCases)
      }
      "3. return a payment schedule request with an initial payment when" - {
        val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "the current date is Friday 1st May with upcoming bank holiday",
            upfrontPaymentAmount, date("2020-05-01"), Some(PaymentDayOfMonth(12)),
            Some(date("2020-05-12")), date("2020-05-01"), 12, date("2020-06-12")),

          (".2", "the current date is Thursday 7th May with upcoming bank holiday",
            upfrontPaymentAmount, date("2020-05-07"), Some(PaymentDayOfMonth(15)),
            Some(date("2020-05-18")), date("2020-05-07"), 15, date("2020-06-15")),

          (".3", "the current date is bank holiday Friday 8th May",
            upfrontPaymentAmount, date("2020-05-08"), Some(PaymentDayOfMonth(15)),
            Some(date("2020-05-19")), date("2020-05-08"), 15, date("2020-06-15")),

          (".4", "the current date is Monday 11th May",
            upfrontPaymentAmount, date("2020-05-11"), Some(PaymentDayOfMonth(18)),
            Some(date("2020-05-22")), date("2020-05-11"), 18, date("2020-06-18")),

          (".5", "the current date is the Monday 25th May so the payment dates roll into the next month",
            upfrontPaymentAmount, date("2020-05-25"), Some(PaymentDayOfMonth(1)),
            Some(date("2020-06-05")), date("2020-05-25"), 1, date("2020-07-01"))
        )

        testPaymentsCalendar(testCases)
      }
    }
  }
}
