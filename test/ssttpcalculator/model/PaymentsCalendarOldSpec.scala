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
import org.scalatest.prop.TableFor10
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import org.scalatest.prop.Tables._
import ssttpcalculator.CalculatorService
import testsupport.{DateSupport, ItSpec}
import uk.gov.hmrc.selfservicetimetopay.models.RegularPaymentDay

import java.time.{Clock, LocalDate, LocalDateTime, Month}
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC

class PaymentsCalendarOldSpec extends ItSpec with Matchers with DateSupport {

  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

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

  def testPaymentsCalendar(testCases: TableFor10[String, String,
    Seq[TaxLiability], BigDecimal, LocalDate, Option[RegularPaymentDay],
    Option[LocalDate], LocalDate, Int, LocalDate]): Unit = {

    forAll(testCases) { (id, caseDescription,
                         inputDebits, inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay,
                         expectedMaybeUpfrontPaymentDate, expectedPlanStartDate, expectedRegularPaymentsDay, expectedFirstRegularPaymentDay) =>
      s"$id. $caseDescription" in {

        val taxPaymentPlan = PaymentsCalendar.generate(inputDebits, inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay)(config)

        taxPaymentPlan.maybeUpfrontPaymentDate shouldBe expectedMaybeUpfrontPaymentDate
        taxPaymentPlan.planStartDate shouldBe expectedPlanStartDate
        taxPaymentPlan.regularPaymentsDay shouldBe expectedRegularPaymentsDay
        taxPaymentPlan.regularPaymentDates.head shouldBe expectedFirstRegularPaymentDay
      }
    }
  }

  val upfrontPaymentAmount = BigDecimal(4000.00)


  "CalculatorService.schedule" - {

    val liabilities = List(TaxLiability(BigDecimal(3559.20), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JULY, 31)))

    val upfrontPaymentAmount = BigDecimal(4000.00)

    val regularPaymentAmount = 500

    val clock = clockForMay(_1st)
    val today = LocalDate.now(clock)

    val paymentsCalendar = PaymentsCalendar.generate(liabilities, upfrontPaymentAmount, today)(config)

    "when passed an upfront payment amount that is more than 0 and where the remaining liabilities are more than £32 returns a schedule with" - {

      val schedule = calculatorService.schedule(liabilities, regularPaymentAmount, paymentsCalendar, upfrontPaymentAmount).get

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

        val testCases: TableFor10[String, String, Seq[TaxLiability], BigDecimal, LocalDate, Option[RegularPaymentDay], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputDebits", "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "when the current date is the 1st",
            liabilities, 0, date("2020-05-01"), Some(RegularPaymentDay(1)),
            None, date("2020-05-01"), 1, date("2020-06-01")),

          (".2", "when the current date is the 28th",
            liabilities, 0, date("2020-05-28"), Some(RegularPaymentDay(28)),
            None, date("2020-05-28"), 28, date("2020-06-28")),

          (".3", "when the current date is the 29th",
            liabilities, 0, date("2020-05-29"), Some(RegularPaymentDay(29)),
            None, date("2020-05-29"), 28, date("2020-06-28")),
        )

        testPaymentsCalendar(testCases)
      }

      "2. without an initial payment when" - {

        val testCases: TableFor10[String, String, Seq[TaxLiability], BigDecimal, LocalDate, Option[RegularPaymentDay], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputDebits", "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "the current date is Friday 1st May with upcoming bank holiday",
            liabilities, 0, date("2020-05-01"), Some(RegularPaymentDay(12)),
            None, date("2020-05-01"), 12, date("2020-05-12")),

          (".2", "the current date is Thursday 7th May with upcoming bank holiday",
            liabilities, 0, date("2020-05-07"), Some(RegularPaymentDay(15)),
            None, date("2020-05-07"), 15, date("2020-06-15")),

          (".3", "the current date is bank holiday Friday 8th May",
            liabilities, 0, date("2020-05-08"), Some(RegularPaymentDay(15)),
            None, date("2020-05-08"), 15, date("2020-06-15")),

          (".4", "the current date is Monday 11th May",
            liabilities, 0, date("2020-05-11"), Some(RegularPaymentDay(18)),
            None, date("2020-05-11"), 18, date("2020-06-18")),

          (".5", "the current date is the Monday 25th May so the payment dates roll into the next month",
            liabilities, 0, date("2020-05-25"), Some(RegularPaymentDay(1)),
            None, date("2020-05-25"), 1, date("2020-07-01")),
        )

        testPaymentsCalendar(testCases)
      }
      "3. return a payment schedule request with an initial payment when" - {
        val testCases: TableFor10[String, String, Seq[TaxLiability], BigDecimal, LocalDate, Option[RegularPaymentDay], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

          ("id", "caseDescription",
            "inputDebits", "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
            "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

          (".1", "the current date is Friday 1st May with upcoming bank holiday",
            liabilities, upfrontPaymentAmount, date("2020-05-01"), Some(RegularPaymentDay(12)),
            Some(date("2020-05-12")), date("2020-05-01"), 12, date("2020-06-12")),

          (".2", "the current date is Thursday 7th May with upcoming bank holiday",
            liabilities, upfrontPaymentAmount, date("2020-05-07"), Some(RegularPaymentDay(15)),
            Some(date("2020-05-18")), date("2020-05-07"), 15, date("2020-06-15")),

          (".3", "the current date is bank holiday Friday 8th May",
            liabilities, upfrontPaymentAmount, date("2020-05-08"), Some(RegularPaymentDay(15)),
            Some(date("2020-05-19")), date("2020-05-08"), 15, date("2020-06-15")),

          (".4", "the current date is Monday 11th May",
            liabilities, upfrontPaymentAmount, date("2020-05-11"), Some(RegularPaymentDay(18)),
            Some(date("2020-05-22")), date("2020-05-11"), 18, date("2020-06-18")),

          (".5", "the current date is the Monday 25th May so the payment dates roll into the next month",
            liabilities, upfrontPaymentAmount, date("2020-05-25"), Some(RegularPaymentDay(1)),
            Some(date("2020-06-05")), date("2020-05-25"), 1, date("2020-07-01"))
        )

        testPaymentsCalendar(testCases)
      }
    }
  }
}
