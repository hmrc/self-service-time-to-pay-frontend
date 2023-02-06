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
import play.libs.Scala.Tuple
import ssttpcalculator.model.{Instalment, InterestRate, Payables, Payment, TaxLiability}
import testsupport.ItSpec

import java.time.{LocalDate, Year}

class CalculatorServiceSpec2023 extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  def date(date: String): LocalDate = LocalDate.parse(date)

  def interestAccrued(interestRateCalculator: LocalDate => InterestRate)(dueDate: LocalDate, payment: Payment): BigDecimal = {
    val currentInterestRate = interestRateCalculator(dueDate).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(dueDate.getYear).length()) / BigDecimal(100)
    val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(dueDate, payment.date))
    payment.amount * currentDailyRate * daysInterestToCharge
  }


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

  "CalculatorService.updatedLiabilities" - {
    "pays off the payment amount (in argument) from the Payables' liabilities starting with the oldest" - {
      "returns the same liabilities if the payment amount is 0" in {
        val payment = Payment(LocalDate.now, 0)

        calculatorService.taxLiabilitiesUpdated(payment, payablesWithOne2000LiabilityNoDueDate) shouldBe
          payablesWithOne2000LiabilityNoDueDate.liabilities
      }
      "pays off only part of the oldest liability if the payment amount is smaller than it" - {
        "leaving what is left of the only liability if there is only one" in {
          val payment = Payment(LocalDate.now, 500)

          calculatorService.taxLiabilitiesUpdated(payment, payablesWithOne2000LiabilityNoDueDate) shouldBe
            Seq(TaxLiability(1500, date("2100-01-01")))
        }
        "leaving multiple liabilities if there are more than one" in {
          val payment = Payment(LocalDate.now, 500)

          calculatorService.taxLiabilitiesUpdated(payment, payablesWithTwoLiabilitiesNoDueDate) shouldBe
            Seq(TaxLiability(500, date("2100-01-01")), TaxLiability(2000, date("2100-01-01")))
        }
      }
      "pays off the oldest liability and part of the next oldest is the payment amount is larger than the first liability" in {
        val payment = Payment(LocalDate.now, 1200)

        calculatorService.taxLiabilitiesUpdated(payment, payablesWithTwoLiabilitiesNoDueDate) shouldBe
          Seq(TaxLiability(1800, date("2100-01-01")))
      }
      "returns a Payables with no liabilities if the payment amount covers all the liabilities" - {
        "when there is only one liability" in {
          val payment = Payment(LocalDate.now, 2000)

          calculatorService.taxLiabilitiesUpdated(payment, payablesWithOne2000LiabilityNoDueDate) shouldBe
            Seq()
        }
        "when there are multiple liabilities" in {
          val payment = Payment(LocalDate.now, 3000)

          calculatorService.taxLiabilitiesUpdated(payment, payablesWithTwoLiabilitiesNoDueDate) shouldBe
            Seq()
        }
      }

    }
  }

  "CalculatorService.regularInstalments" - {
    "generates a list of monthly payment instalments of the regular payment amount until the payables are cleared" - {
      "where no interest is payable on any liabilities" - {
        "if balance to pay is 0, creates an empty list (no instalments)" in {
          val regularPaymentAmount = 500

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = Payables(Seq()),
            interestRateCalculator = fixedZeroInterest
          ) shouldBe Seq()

        }
        "if balance to pay <= the regular payment amount," +
          " creates a list with a single instalment of the balance to pay" in {
          val regularPaymentAmount = 2000

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = payablesWithOne2000LiabilityNoDueDate,
            interestRateCalculator = fixedZeroInterest
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
              payables = payablesWithOne2000LiabilityNoDueDate,
              interestRateCalculator = fixedZeroInterest
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
              payables = payablesWithOne2000LiabilityNoDueDate,
              interestRateCalculator = fixedZeroInterest
            ) shouldBe Seq(
              Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-06-17"), amount = 200, interest = 0)
            )
          }
        }
      }
      "where interest is payable on all liabilities" in {
        val debtAmount = 1000
        val dueDate = date("2022-03-17")
        val payables = Payables(liabilities = Seq(TaxLiability(debtAmount, dueDate)))

        val regularPaymentAmount = 1000

        val expectedFirstInstalmentPaymentDate = paymentsCalendar.regularPaymentDates(0)
        val expectedFirstPayment = Payment(expectedFirstInstalmentPaymentDate, regularPaymentAmount)

        val expectedInterestAccrued = interestAccrued(fixedInterestRate)(dueDate, expectedFirstPayment)

        calculatorService.regularInstalments(
          paymentsCalendar = paymentsCalendar,
          regularPaymentAmount = regularPaymentAmount,
          payables = payables,
          interestRateCalculator = fixedInterestRate) shouldBe Seq(
          Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = expectedInterestAccrued),
          Instalment(paymentDate = date("2023-04-17"), amount = expectedInterestAccrued, interest = 0)
        )
      }
    }
  }
}

