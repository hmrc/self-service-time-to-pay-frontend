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
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor11
import ssttpcalculator.model.{PaymentSchedule, PaymentsCalendar, TaxLiability}
import testsupport.ItSpec
import uk.gov.hmrc.selfservicetimetopay.models.PaymentDayOfMonth

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode.HALF_UP

class PaymentPlansServiceSpecAlternate extends ItSpec {

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: PaymentPlansService = fakeApplication().injector.instanceOf[PaymentPlansService]
  val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  def debit(amt: BigDecimal, due: String): TaxLiability = TaxLiability(amount  = amt.setScale(2), dueDate = LocalDate.parse(due))

  def date(date: String): LocalDate = LocalDate.parse(date)

  "CalculatorService.schedule generates a schedule with" - {
    "first set of test cases" - {
      val testCasesFirstSet: TableFor11[String, Seq[TaxLiability], LocalDate, LocalDate, LocalDate, Int, Int, Double, Double, Double, Double] = Table(
        ("id", "inputDebits", "inputStartDate", "expectedLastPaymentDate", "inputFirstRegularPaymentDate", "inputUpfrontPaymentAmount", "expectedDuration", "expectedTotalPayable", "expectedTotalInterestCharged", "inputRegularInstalmentAmount", "expectedFinalInstalmentAmount"),
        ("1.a.i.c", Seq(debit(2000.00, "2017-01-31")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 0, 11, 2032.59, 32.59, 200.00, 32.59),
        ("1.a.ii.c", Seq(debit(2000.00, "2015-01-31")), date("2016-03-14"), date("2017-02-20"), date("2016-04-20"), 0, 11, 2095.45, 95.45, 200.00, 95.45),
        ("1.b.ii.c", Seq(debit(2000.00, "2016-01-31")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 0, 11, 2090.24, 90.24, 200.00, 90.24),
        ("1.d", Seq(debit(2000.00, "2017-01-31")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 1000, 11, 2019.54, 19.54, 100.00, 19.54),
        ("1.e", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 2000, 11, 3023.76, 23.76, 100.00, 23.76),
        ("1.f", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 2500, 11, 4032.92, 32.92, 150.00, 32.92),
        ("2.a", Seq(debit(2000.00, "2017-03-31")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 0, 11, 2023.7, 23.7, 200.00, 23.7),
        ("2.b", Seq(debit(2000.00, "2017-03-18")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 1000, 11, 2012.83, 12.83, 100.00, 12.83),
        ("2.c", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 2000, 11, 4025.51, 25.51, 200.00, 25.51),
        ("2.d", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 2500, 11, 4019.13, 19.13, 150.00, 19.13),
        ("2.e", Seq(debit(2000.00, "2017-03-31")), date("2017-03-11"), date("2018-02-20"), date("2017-04-20"), 1000, 11, 2011.85, 11.85, 100.00, 11.85)
      )

      "accurate interest payable" - {

        forAll(testCasesFirstSet) { (id,
          inputDebits, inputStartDate,
          _, inputFirstRegularPaymentDate, inputUpfrontPaymentAmount,
          _, expectedTotalPayable, expectedTotalInterestCharged,
          inputRegularInstalmentAmount, _) =>

          s"$id. total interest charged of $expectedTotalInterestCharged as part of total payable of $expectedTotalPayable" in {
            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            val totalPayableInSchedule = schedule.initialPayment + schedule.instalments.map(_.amount).sum

            totalPayableInSchedule.setScale(2, HALF_UP) shouldBe expectedTotalPayable
            schedule.totalInterestCharged.setScale(2, HALF_UP) shouldBe expectedTotalInterestCharged
          }
        }
      }
      "correct payment amounts" - {

        forAll(testCasesFirstSet) { (id, inputDebits, inputStartDate, _, inputFirstRegularPaymentDate,
          inputUpfrontPaymentAmount, _, _, _, inputRegularInstalmentAmount,
          expectedFinalInstalmentAmount) =>

          s"$id. upfront payment of $inputUpfrontPaymentAmount, regular instalments of $inputRegularInstalmentAmount and a final instalment of $expectedFinalInstalmentAmount" in {
            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            schedule.initialPayment shouldBe inputUpfrontPaymentAmount

            val instalments = schedule.instalments

            instalments.init.foreach(instalment => instalment.amount.setScale(2, HALF_UP) shouldBe inputRegularInstalmentAmount)
            instalments.last.amount.setScale(2, HALF_UP) shouldBe expectedFinalInstalmentAmount
          }
        }

      }
      "correct instalment dates" - {

        forAll(testCasesFirstSet) { (id, inputDebits, inputStartDate, expectedLastPaymentDate, inputFirstRegularPaymentDate,
          inputUpfrontPaymentAmount, _, _, _, inputRegularInstalmentAmount, _) =>

          s"$id. first instalment on $inputFirstRegularPaymentDate and last instalment on $expectedLastPaymentDate" in {
            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            val instalments = schedule.instalments

            instalments.head.paymentDate shouldBe inputFirstRegularPaymentDate
            instalments.last.paymentDate shouldBe expectedLastPaymentDate
          }
        }
      }
      "right number of instalments / duration" - {

        forAll(testCasesFirstSet) { (id, inputDebits, inputStartDate, _, inputFirstRegularPaymentDate,
          inputUpfrontPaymentAmount, expectedDuration, _, _,
          inputRegularInstalmentAmount, _) =>

          s"$id. number of instalments $expectedDuration" in {
            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            val instalments = schedule.instalments

            instalments.size shouldBe expectedDuration
          }
        }
      }
    }
    "second set of test cases" - {
      val testCasesSecondSet = Table[String, Seq[TaxLiability], LocalDate, LocalDate, LocalDate, Double, Double, Int](
        ("id", "inputDebits", "inputStartDate", "expectedLastPaymentDate", "inputFirstPaymentDate", "inputUpfrontPayment", "inputRegularInstalmentAmount", "expectedDuration"),
        ("1.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-02-21"), date("2017-04-21"), 0, 200, 11),
        ("1.ii", Seq(debit(2000.00, "2015-01-31")), date("2015-03-14"), date("2016-02-21"), date("2015-04-21"), 0, 200, 11),
        ("1.iii", Seq(debit(2000.00, "2016-01-31")), date("2016-03-14"), date("2017-02-21"), date("2016-04-21"), 0, 200, 11),
        ("2.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-02-02"), date("2017-04-02"), 0, 200, 11),
        ("2.ii", Seq(debit(2000.00, "2015-01-31")), date("2016-03-14"), date("2017-02-02"), date("2016-04-02"), 0, 200, 11),
        ("2.iii", Seq(debit(2000.00, "2016-01-31")), date("2017-03-14"), date("2018-02-02"), date("2017-04-02"), 0, 200, 11),
        ("3.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-02-21"), date("2017-04-21"), 1000, 100, 11),
        ("3.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-14"), date("2018-02-21"), date("2017-04-21"), 2000, 100, 11),
        ("3.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-14"), date("2018-02-21"), date("2017-04-21"), 2500, 150, 11),
        ("4.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-03-02"), date("2017-05-02"), 1000, 100, 11),
        ("4.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-14"), date("2018-03-02"), date("2017-05-02"), 2000, 100, 11),
        ("4.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-14"), date("2018-03-02"), date("2017-05-02"), 2500, 150, 11),
        ("5.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-02-14"), date("2017-04-14"), 1000, 100, 11),
        ("6.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-03-02"), date("2017-05-02"), 1000, 100, 11),
        ("7.i", Seq(debit(2000.00, "2017-01-31")), date("2017-03-14"), date("2018-02-21"), date("2017-04-21"), 1000, 100, 11)
      )

      "correct instalment dates" - {
        forAll(testCasesSecondSet) { (id, inputDebits, inputStartDate, expectedLastPaymentDate, inputFirstRegularPaymentDate, inputUpfrontPaymentAmount, inputRegularInstalmentAmount, _) =>
          s"$id. first instalment on $inputFirstRegularPaymentDate and last instalment on $expectedLastPaymentDate" in {

            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            val instalments = schedule.instalments

            instalments.head.paymentDate shouldBe inputFirstRegularPaymentDate
            instalments.last.paymentDate shouldBe expectedLastPaymentDate
          }
        }

      }
      "right number of instalments / duration" - {
        forAll(testCasesSecondSet) { (id, inputDebits, inputStartDate, _, inputFirstRegularPaymentDate, inputUpfrontPaymentAmount, inputRegularInstalmentAmount, expectedDuration) =>

          s"$id. number of instalments $expectedDuration" in {
            val paymentsCalendar = PaymentsCalendar.generate(
              taxLiabilities         = inputDebits,
              upfrontPaymentAmount   = inputUpfrontPaymentAmount,
              dateNow                = inputStartDate,
              maybePaymentDayOfMonth = Some(PaymentDayOfMonth(inputFirstRegularPaymentDate.getDayOfMonth))
            )(appConfig)

            val schedule: PaymentSchedule = calculatorService.schedule(inputDebits, inputRegularInstalmentAmount, paymentsCalendar, inputUpfrontPaymentAmount).get

            val instalments = schedule.instalments

            instalments.size shouldBe expectedDuration
          }
        }
      }
    }
  }
}
