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

    //    "PaymentsCalendar should" - {
    //      "include correct payments calendar information" - {
    //        "when the current date is the 1st" in {
    //          val clock = clockForMay(_1st)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(today.getDayOfMonth)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _1st
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_1st)
    //        }
    //      }
    //
    //      "when the current date is the 28th" in {
    //        val clock = clockForMay(_28th)
    //        val today = LocalDate.now(clock)
    //
    //        val PaymentsCalendar = PaymentsCalendar(
    //          taxLiabilities             = liabilities,
    //          withUpfrontPayment         = withUpfrontPaymentTrue,
    //          planStartDate              = today,
    //          maybeRegularPaymentDay = Some(RegularPaymentDay(today.getDayOfMonth)),
    //          maybePaymentToday          = None
    //        )(config)
    //
    //        PaymentsCalendar.planStartDate shouldBe today
    //        PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //        PaymentsCalendar.regularPaymentsDay shouldBe _28th
    //        PaymentsCalendar.regularPaymentDates.head shouldBe june(_28th)
    //      }
    //
    //      "when the current date is the 29th" in {
    //        val clock = clockForMay(_29th)
    //        val today = LocalDate.now(clock)
    //
    //        val PaymentsCalendar = PaymentsCalendar(
    //          taxLiabilities     = liabilities,
    //          withUpfrontPayment = withUpfrontPaymentTrue,
    //          planStartDate      = today,
    //          Some(RegularPaymentDay(today.getDayOfMonth)),
    //          None
    //        )(config)
    //
    //        PaymentsCalendar.planStartDate shouldBe today
    //        PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //        PaymentsCalendar.regularPaymentsDay shouldBe _1st
    //        PaymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
    //      }
    //      "without an initial payment when" - {
    //        "the current date is Friday 1st May with upcoming bank holiday" in {
    //          val clock = clockForMay(_1st)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_12th)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _12th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe may(_12th)
    //        }
    //
    //        "the current date is Thursday 7th May with upcoming bank holiday" in {
    //          val clock = clockForMay(_7th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_15th)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _15th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
    //
    //        }
    //
    //        "the current date is bank holiday Friday 8th May" in {
    //          val clock = clockForMay(_8th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_15th)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _15th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
    //        }
    //
    //        "the current date is Monday 11th May" in {
    //          val clock = clockForMay(_11th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_18th)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _18th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_18th)
    //        }
    //
    //        "the current date is the Monday 25th May so the payment dates roll into the next month" in {
    //          val clock = clockForMay(_25th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_1st)),
    //            None
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe None
    //          PaymentsCalendar.regularPaymentsDay shouldBe _1st
    //          PaymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
    //        }
    //      }
    //      "return a payment schedule request with an initial payment when" - {
    //        "the current date is Friday 1st May with upcoming bank holiday" in {
    //          val clock = clockForMay(_1st)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_12th)),
    //            Some(PaymentToday(true))
    //
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_12th))
    //          PaymentsCalendar.regularPaymentsDay shouldBe _12th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_12th)
    //        }
    //
    //        "the current date is Thursday 7th May with upcoming bank holiday" in {
    //          val clock = clockForMay(_7th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_15th)),
    //            Some(PaymentToday(true))
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_18th))
    //          PaymentsCalendar.regularPaymentsDay shouldBe _15th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
    //        }
    //
    //        "the current date is bank holiday Friday 8th May" in {
    //          val clock = clockForMay(_8th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_15th)),
    //            Some(PaymentToday(true))
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_19th))
    //          PaymentsCalendar.regularPaymentsDay shouldBe _15th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_15th)
    //        }
    //
    //        "the current date is Monday 11th May" in {
    //          val clock = clockForMay(_11th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_18th)),
    //            Some(PaymentToday(true))
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(may(_22nd))
    //          PaymentsCalendar.regularPaymentsDay shouldBe _18th
    //          PaymentsCalendar.regularPaymentDates.head shouldBe june(_18th)
    //        }
    //
    //        "the current date is the Monday 25th May so the payment dates roll into the next month" in {
    //          val clock = clockForMay(_25th)
    //          val today = LocalDate.now(clock)
    //
    //          val PaymentsCalendar = PaymentsCalendar(
    //            taxLiabilities     = liabilities,
    //            withUpfrontPayment = withUpfrontPaymentTrue,
    //            planStartDate      = today,
    //            Some(RegularPaymentDay(_1st)),
    //            Some(PaymentToday(true))
    //          )(config)
    //
    //          PaymentsCalendar.planStartDate shouldBe today
    //          PaymentsCalendar.maybeUpfrontPaymentDate shouldBe Some(june(_5th))
    //          PaymentsCalendar.regularPaymentsDay shouldBe _1st
    //          PaymentsCalendar.regularPaymentDates.head shouldBe july(_1st)
    //        }
    //      }
    //    }
  }
}
