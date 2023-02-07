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
import ssttpcalculator.model.{Instalment, InterestRate, Payables, Payment, PaymentsCalendar, TaxLiability}
import testsupport.ItSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{LocalDate, Year}

class CalculatorServiceSpec2023 extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  implicit val servicesConfig: ServicesConfig = fakeApplication().injector.instanceOf[ServicesConfig]

  def date(date: String): LocalDate = LocalDate.parse(date)

  def interestAccrued(interestRateCalculator: LocalDate => InterestRate)(dueDate: LocalDate, payment: Payment): BigDecimal = {
    val currentInterestRate = interestRateCalculator(dueDate).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(dueDate.getYear).length()) / BigDecimal(100)
    val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(dueDate, payment.date))
    payment.amount * currentDailyRate * daysInterestToCharge
  }

  val paymentsCalendar: PaymentsCalendar = PaymentsCalendar(
    createdOn = date("2023-02-02"),
    maybeUpfrontPaymentDate = Some(date("2023-02-12")),
    regularPaymentsDay = 17
  )


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

  "CalculatorService.regularInstalments" - {
    "generates a list of monthly payment instalments at the regular payment amount until the payables are cleared" - {
      "where no interest is payable on any liabilities" - {
        "if balance to pay is 0, creates an empty list (no instalments)" in {
          val regularPaymentAmount = 500

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = Payables(Seq()),
            dateToRate = fixedZeroInterest
          ) shouldBe Seq()

        }
        "if balance to pay <= regular payment amount creates list with single instalment of the balance to pay" in {
          val regularPaymentAmount = 2000

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = payablesWithOne2000LiabilityNoDueDate,
            dateToRate = fixedZeroInterest
          ) shouldBe Seq(
            Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
          )
        }
        "if balance to pay > regular payment amount, creates list of multiple instalments" - {
          "if balance to pay divisible by regular payment amount all instalments are exactly regular amount" in {
            val regularPaymentAmount = 500

            calculatorService.regularInstalments(
              paymentsCalendar = paymentsCalendar,
              regularPaymentAmount = regularPaymentAmount,
              payables = payablesWithOne2000LiabilityNoDueDate,
              dateToRate = fixedZeroInterest
            ) shouldBe Seq(
              Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-06-17"), amount = regularPaymentAmount, interest = 0)
            )
          }
          "if balance to pay not exactly divisible by regular payment amount all instalments = regular payment amount" +
            ", except last which is less" in {
            val regularPaymentAmount = 600

            calculatorService.regularInstalments(
              paymentsCalendar = paymentsCalendar,
              regularPaymentAmount = regularPaymentAmount,
              payables = payablesWithOne2000LiabilityNoDueDate,
              dateToRate = fixedZeroInterest
            ) shouldBe Seq(
              Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
              Instalment(paymentDate = date("2023-06-17"), amount = 200, interest = 0)
            )
          }
        }
      }
      "where interest is payable on all liabilities" - {
        "single liability" in {
          val debtAmount = 1000
          val dueDate = date("2022-03-17")
          val payables = Payables(liabilities = Seq(TaxLiability(debtAmount, dueDate)))

          val regularPaymentAmount = 1000

          val expectedFirstInstalmentPaymentDate = paymentsCalendar.regularPaymentDates.head
          val expectedFirstPayment = Payment(expectedFirstInstalmentPaymentDate, regularPaymentAmount)

          val expectedInterestAccrued = interestAccrued(fixedInterestRate(1))(dueDate, expectedFirstPayment)

          calculatorService.regularInstalments(
            paymentsCalendar = paymentsCalendar,
            regularPaymentAmount = regularPaymentAmount,
            payables = payables,
            dateToRate = fixedInterestRate(1)) shouldBe Seq(
            Instalment(date("2023-03-17"), regularPaymentAmount, expectedInterestAccrued),
            Instalment(date("2023-04-17"), expectedInterestAccrued, 0)
          )
        }
        "multiple liabilities" - {
          "two debts, paid in two months" in {
            val firstDebtAmount = 1000
            val firstDebtDueDate = date("2022-03-17")
            val secondDebtAmount = 100
            val secondDebtDueDate = date("2022-09-17")
            val payables = Payables(liabilities = Seq(
              TaxLiability(firstDebtAmount, firstDebtDueDate),
              TaxLiability(secondDebtAmount, secondDebtDueDate)
            ))

            val regularPaymentAmount = 1000

            val expectedFirstInstalmentPaymentDate = paymentsCalendar.regularPaymentDates.head
            val expectedFirstPayment = Payment(expectedFirstInstalmentPaymentDate, regularPaymentAmount)
            val expectedInterestAccruedOnFirstDebt = interestAccrued(fixedInterestRate())(firstDebtDueDate, expectedFirstPayment)

            val expectedSecondInstalmentPaymentDate = paymentsCalendar.regularPaymentDates(1)
            val expectedSecondPayment = Payment(expectedSecondInstalmentPaymentDate, secondDebtAmount)
            val expectedInterestAccruedOnSecondDebt = interestAccrued(fixedInterestRate())(secondDebtDueDate, expectedSecondPayment)

            calculatorService.regularInstalments(
              paymentsCalendar,
              regularPaymentAmount,
              payables,
              fixedInterestRate(1)
            ) shouldBe Seq(
              Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = expectedInterestAccruedOnFirstDebt),
              Instalment(
                paymentDate = date("2023-04-17"),
                amount = secondDebtAmount + expectedInterestAccruedOnFirstDebt + expectedInterestAccruedOnSecondDebt,
                interest = expectedInterestAccruedOnSecondDebt
              )
            )
          }
          "two debts, paid in more than two months" - {
            "with final month clearing late payment interest" in {
              val payables = testPayables((5000, date("2022-03-17")), (5000, date("2022-09-17")))
              val regularPaymentAmount = 1000

              val result = calculatorService.regularInstalments(
                paymentsCalendar,
                regularPaymentAmount,
                payables,
                fixedInterestRate(1)
              )

              result.length shouldBe 11
              result.init.foreach(instalment => {
                instalment.amount shouldBe 1000
                instalment.interest > 0 shouldBe true
              })
              result.last.amount <= 1000 shouldBe true
              result.map(_.interest).sum shouldEqual result.last.amount
            }
            "with late payment interest over two final months" in {
              val payables = testPayables((5000, date("2022-03-17")), (5000, date("2022-09-17")))
              val regularPaymentAmount = 1000

              val result = calculatorService.regularInstalments(
                paymentsCalendar,
                regularPaymentAmount,
                payables,
                fixedInterestRate(10)
              )

              result.length shouldBe 12
              result.init.foreach(instalment => instalment.amount shouldBe 1000)
              result.slice(0, -2).foreach(instalment => instalment.interest > 0 shouldBe true)
              result.last.amount <= 1000 shouldBe true
              result.map(_.interest).sum shouldEqual result.last.amount + result.init.last.amount
            }
          }
        }
      }
    }
  }
}

