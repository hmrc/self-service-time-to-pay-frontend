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

import play.api.Logger
import ssttpcalculator.model.{Instalment, Payables, PaymentsCalendar, TaxLiability}
import testsupport.{ItSpec, UnitSpec}

import java.time.LocalDate

class CalculatorServiceSpec2023 extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  def date(date: String): LocalDate = LocalDate.parse(date)

  import testsupport.testdata.CalculatorDataGenerator.newCalculatorModel._



  "PaymentsCalendar.regularPaymentDates" - {
    "generates 24 months' worth of dates on the day set by regularPaymentsDay" in {
      paymentsCalendar.regularPaymentDates shouldBe Seq(
        date("2023-03-17"),
        date("2023-04-17"),
        date("2023-05-17"),
        date("2023-06-17"),
        date("2023-07-17"),
        date("2023-08-17"),
        date("2023-09-17"),
        date("2023-10-17"),
        date("2023-11-17"),
        date("2023-12-17"),
        date("2024-01-17"),
        date("2024-02-17"),
        date("2024-03-17"),
        date("2024-04-17"),
        date("2024-05-17"),
        date("2024-06-17"),
        date("2024-07-17"),
        date("2024-08-17"),
        date("2024-09-17"),
        date("2024-10-17"),
        date("2024-11-17"),
        date("2024-12-17"),
        date("2025-01-17"),
        date("2025-02-17"),
      )
    }
  }

  "Payables.payOff" - {
    "pays off the payment amount from the liabilities starting with the oldest" - {
      "pays off part of the oldest liability if the payment amount is smaller than it" in {
        payables2000inJuly2023.payOff(500) shouldBe
          Payables(Seq(TaxLiability(1500, date("2023-07-31"))))
      }
      "pays off the oldest liability and part of the next oldest is the payment amount is larger than the first liability" in {
        payablesTwoLiabilities.payOff(1200) shouldBe
          Payables(Seq(TaxLiability(1800, date("2023-07-31"))))
      }

    }
  }

  "CalculatorService.regularInstalments" - {
    "generates a list of monthly payment instalments of the regular payment amount until the payables are cleared" in {
      val regularPaymentAmount = 500

      calculatorService.regularInstalments(
        paymentsCalendar = paymentsCalendar,
        regularPaymentAmount = regularPaymentAmount,
        payables = payables2000inJuly2023
      ) shouldBe Seq(
        Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
        Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
        Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
        Instalment(paymentDate = date("2023-06-17"), amount = regularPaymentAmount, interest = 0)
      )

    }

  }

}
