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
import ssttpcalculator.model.{Instalment, Payables, TaxLiability}
import testsupport.ItSpec

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
    "pays off the payment amount (in argument) from the Payables' liabilities starting with the oldest" - {
      "returns the same Payables if the payment amount is 0" in {
        val paymentAmount = 0

        payables2000inJuly2023.payOff(paymentAmount) shouldBe
          payables2000inJuly2023
      }
      "pays off only part of the oldest liability if the payment amount is smaller than it" - {
        "leaving what is left of the only liability if there is only one" in {
          val paymentAmount = 500

          payables2000inJuly2023.payOff(paymentAmount) shouldBe
            Payables(Seq(TaxLiability(1500, date("2023-07-31"))))
        }
        "leaving multiple liabilities if there are more than one" in {
          val paymentAmount = 500

          payablesTwoLiabilities.payOff(paymentAmount) shouldBe
            Payables(Seq(
              TaxLiability(500, date("2023-04-30")),
              TaxLiability(2000, date("2023-07-31"))
            ))
        }
      }
      "pays off the oldest liability and part of the next oldest is the payment amount is larger than the first liability" in {
        val paymentAmount = 1200

        payablesTwoLiabilities.payOff(paymentAmount) shouldBe
          Payables(Seq(TaxLiability(1800, date("2023-07-31"))))
      }
      "returns a Payables with no liabilities if the payment amount covers all the liabilities" - {
        "when there is only one liability" in {
          val paymentAmount = 2000

          payables2000inJuly2023.payOff(paymentAmount) shouldBe
            Payables(Seq())
        }
        "when there are multiple liabilities" in {
          val paymentAmount = 3000

          payablesTwoLiabilities.payOff(paymentAmount) shouldBe
            Payables(Seq())
        }
      }

    }
  }

  "CalculatorService.regularInstalments" - {
    "generates a list of monthly payment instalments of the regular payment amount until the payables are cleared" - {
      "if balance to pay is 0, creates an empty list (no instalments)" in {
        val regularPaymentAmount = 500

        calculatorService.regularInstalments(
          paymentsCalendar = paymentsCalendar,
          regularPaymentAmount = regularPaymentAmount,
          payables = Payables(liabilities = Seq())
        ) shouldBe Seq()

      }
      "if balance to pay <= the regular payment amount," +
        " creates a list with a single instalment of the balance to pay" in {
        val regularPaymentAmount = 2000

        calculatorService.regularInstalments(
          paymentsCalendar = paymentsCalendar,
          regularPaymentAmount = regularPaymentAmount,
          payables = payables2000inJuly2023
        ) shouldBe Seq(
          Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
        )
      }
      "if balance to pay > regular payment amount, creates list of multiple instalments" - {
        "if balance to pay exactly divisible by regular payment amount" +
          " all instalments are for exactly regular payment amount" in {
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
        "if balance to pay not exactly divisible by regular payment amount," +
          " all instalments = regular payment amount, except last which is less" in {
          val regularPaymentAmount = 600

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = payables2000inJuly2023
          ) shouldBe Seq(
            Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
            Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
            Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
            Instalment(paymentDate = date("2023-06-17"), amount = 200, interest = 0)
          )

        }


      }

    }

  }

}
