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
import ssttpcalculator.model.{PaymentsCalendar, TaxLiability}
import testsupport.{DateSupport, ItSpec}

import java.time.{LocalDate, Month}

class PaymentsPlansServiceSpec extends ItSpec with Matchers with DateSupport {

  val paymentPlansService: PaymentPlansService = fakeApplication().injector.instanceOf[PaymentPlansService]
  val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  def date(date: String): LocalDate = LocalDate.parse(date)

  val upfrontPaymentAmount: BigDecimal = BigDecimal(4000.00)

  "PaymentPlansService.schedule" - {

    val liabilities = List(TaxLiability(BigDecimal(3559.20), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JANUARY, 31)),
                           TaxLiability(BigDecimal(1779.60), LocalDate.of(2022, Month.JULY, 31)))

    val upfrontPaymentAmount = BigDecimal(4000.00)

    val regularPaymentAmount = 500

    val today = LocalDate.parse("2020-05-01")

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
        schedule.instalments.nonEmpty shouldBe true
      }
    }
  }
}
