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
import org.scalatest.prop.Tables._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import testsupport.{DateSupport, ItSpec}
import uk.gov.hmrc.selfservicetimetopay.models.PaymentDayOfMonth

import java.time.LocalDate.now
import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDate, LocalDateTime}

class PaymentsCalendarSpec extends ItSpec with Matchers with DateSupport {

  private val zeroInitialPayment = BigDecimal(0)
  private val initialPaymentTooLarge = BigDecimal(468.01)
  private val defaultPaymentDayOfMonth = 28

  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  def date(date: String): LocalDate = LocalDate.parse(date)

  val customDateNow: LocalDate = now(Clock.fixed(LocalDateTime.parse(s"2023-02-17T00:00:00.880") toInstant UTC, systemDefault()))
  val standardPaymentDayOfMonth: Option[PaymentDayOfMonth] = Some(PaymentDayOfMonth(defaultPaymentDayOfMonth))

  def testPaymentsCalendar(testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate]): Unit = {

    forAll(testCases) { (id, caseDescription,
      inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay,
      expectedMaybeUpfrontPaymentDate, expectedPlanStartDate, expectedRegularPaymentsDay, expectedFirstRegularPaymentDay) =>
      s"$id. $caseDescription" in {

        val paymentsCalendar = PaymentsCalendar.generate(inputUpfrontPayment, inputDateNow, inputMaybeRegularPaymentDay)(appConfig)

        paymentsCalendar.maybeUpfrontPaymentDate shouldBe expectedMaybeUpfrontPaymentDate
        paymentsCalendar.planStartDate shouldBe expectedPlanStartDate
        paymentsCalendar.paymentDayOfMonth shouldBe expectedRegularPaymentsDay
        paymentsCalendar.regularPaymentDates.head shouldBe expectedFirstRegularPaymentDay
      }
    }
  }

  "1. Boundary of when first regular payment is in starting month or next month" - {
    val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

      ("id", "caseDescription",
        "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
        "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

      (".1", "without upfront payment, regular payment day selected far enough away" +
        " to cover days to process first payment so first regular payment in starting month",
        zeroInitialPayment, date("2023-01-01"), Some(PaymentDayOfMonth(12)),
        None, date("2023-01-01"), 12, date("2023-01-12")),

      (".2", "without upfront payment, regular payment day NOT selected far enough away" +
        " to cover days to process first payment so first regular payment next month from starting month",
        zeroInitialPayment, date("2023-01-01"), Some(PaymentDayOfMonth(11)),
        None, date("2023-01-01"), 11, date("2023-02-11")),

      (".3", "with upfront payment, regular payment day selected far enough away" +
        " to cover days to process first payment so  first regular payment in starting month",
        BigDecimal(100), date("2023-01-01"), Some(PaymentDayOfMonth(26)),
        Some(date("2023-01-12")), date("2023-01-01"), 26, date("2023-01-26")),

      (".4", "with upfront payment, regular payment day NOT selected far enough away" +
        " to cover days to process first payment so first regular payment next month from starting month",
        BigDecimal(100), date("2023-01-01"), Some(PaymentDayOfMonth(25)),
        Some(date("2023-01-12")), date("2023-01-01"), 25, date("2023-02-25"))
    )

    testPaymentsCalendar(testCases)

  }
  "2. When no regular payment day preference is given" - {
    val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

      ("id", "caseDescription",
        "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
        "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

      (".1", "no upfront payment, no regular payment day preference so defaults to 28 so first regular payment in starting month",
        zeroInitialPayment, date("2023-01-01"), None,
        None, date("2023-01-01"), 28, date("2023-01-28")),

      (".2", "upfront payment, no regular payment day preference so defaults to 28 so first regular payment in starting month",
        BigDecimal(100), date("2023-01-01"), None,
        Some(date("2023-01-12")), date("2023-01-01"), 28, date("2023-01-28"))
    )

    testPaymentsCalendar(testCases)

  }
  "3. Other cases" - {
    val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

      ("id", "caseDescription",
        "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
        "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

      (".1", "with upfront payment, regular payment day selected 10 days away" +
        " so given gap between payments, first regular payment next month from starting month",
        BigDecimal(100), date("2023-01-01"), Some(PaymentDayOfMonth(11)),
        Some(date("2023-01-12")), date("2023-01-01"), 11, date("2023-02-11")),

      (".2", "with upfront payment, regular payment day selected 11 days away" +
        " so given gap between payments, first regular payment next month from starting month",
        BigDecimal(100), date("2023-01-01"), Some(PaymentDayOfMonth(12)),
        Some(date("2023-01-12")), date("2023-01-01"), 12, date("2023-02-12")),

      (".3", "no upfront payment, regular payment day selected 25 days away" +
        " so first regular payment in starting month",
        zeroInitialPayment, date("2023-01-01"), Some(PaymentDayOfMonth(26)),
        None, date("2023-01-01"), 26, date("2023-01-26")),

      (".4", "no upfront payment, regular payment day selected 24 days away" +
        " so first regular payment in starting month",
        zeroInitialPayment, date("2023-01-01"), Some(PaymentDayOfMonth(25)),
        None, date("2023-01-01"), 25, date("2023-01-25")),

      ("6", "the current date is 17th February so without an upfront payment" +
        " the first regular payment date is 28th February",
        zeroInitialPayment, date("2023-02-17"), standardPaymentDayOfMonth,
        None, customDateNow, 28, date("2023-02-28"))
    )

    testPaymentsCalendar(testCases)

  }
  "4. return a payment schedule request with an initial payment even when user selects upfront payment leaving very little to pay on instalment" - {
    val testCases: TableFor9[String, String, BigDecimal, LocalDate, Option[PaymentDayOfMonth], Option[LocalDate], LocalDate, Int, LocalDate] = Table(

      ("id", "caseDescription",
        "inputUpfrontPayment", "inputDateNow", "inputMaybeRegularPaymentDay",
        "expectedMaybeUpfrontPaymentDate", "expectedPlanStartDate", "expectedRegularPaymentsDay", "expectedFirstRegularPaymentDate"),

      (".1", "the current date is Friday 1st May with upcoming bank holiday",
        initialPaymentTooLarge, date("2020-05-01"), None,
        Some(date("2020-05-12")), date("2020-05-01"), 28, date("2020-05-28")),

      (".2", "the current date is Thursday 7th May with upcoming bank holiday",
        initialPaymentTooLarge, date("2020-05-07"), None,
        Some(date("2020-05-18")), date("2020-05-07"), 28, date("2020-06-28")),

      (".3", "the current date is bank holiday Friday 8th May",
        initialPaymentTooLarge, date("2020-05-08"), None,
        Some(date("2020-05-19")), date("2020-05-08"), 28, date("2020-06-28")),

      (".4", "the current date is Monday 11th May",
        initialPaymentTooLarge, date("2020-05-11"), None,
        Some(date("2020-05-22")), date("2020-05-11"), 28, date("2020-06-28")),

      (".5", "the current date is the Monday 25th May so the payment dates roll into the next month",
        initialPaymentTooLarge, date("2020-05-25"), None,
        Some(date("2020-06-05")), date("2020-05-25"), 28, date("2020-06-28"))
    )

    testPaymentsCalendar(testCases)

  }
}
