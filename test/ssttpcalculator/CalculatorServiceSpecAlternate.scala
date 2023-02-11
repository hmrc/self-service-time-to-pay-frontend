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

import com.github.nscala_time.time.Imports.LocalDateTime
import com.github.nscala_time.time.StaticDateTimeZone.UTC
import config.AppConfig
import journey.PaymentToday
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.Logger
import ssttpcalculator.model.{Payables, PaymentSchedule, TaxLiability, TaxPaymentPlan}
import testsupport.ItSpec
import timetopaytaxpayer.cor.model.{CommunicationPreferences, Debit, SaUtr, SelfAssessmentDetails}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDate, LocalDateTime}

class CalculatorServiceSpecAlternate extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService = fakeApplication().injector.instanceOf[CalculatorService]
  val paymentDatesService = fakeApplication().injector.instanceOf[PaymentDatesService]

  val appConfig = fakeApplication().injector.instanceOf[AppConfig]

  def debit(amt: BigDecimal, due: String): TaxLiability = TaxLiability(amount  = amt.setScale(2), dueDate = LocalDate.parse(due))
  def date(date: String): LocalDate = LocalDate.parse(date)

  val interestCalculationScenarios = Table[String, Seq[TaxLiability], LocalDate, LocalDate, LocalDate, Option[ArrangementDayOfMonth], Option[PaymentToday], Int, Int, Double, Double, Double, Double](
    ("id", "debits", "startDate", "endDate", "firstPaymentDate", "maybeArrangementDayOfMonth", "maybePaymentToday", "initialPayment", "duration", "totalPayable", "totalInterestCharged", "regularInstalmentAmount", "finalInstalmentAmount"),
    ("1.a.i.c", Seq(debit(2000.00, "2017-01-31")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), None, 0, 11, 2032.59, 32.59, 200.00, 232.59),
    ("1.a.ii.c", Seq(debit(2000.00, "2015-01-31")), date("2016-03-14"), date("2017-01-20"), date("2016-04-20"), Some(ArrangementDayOfMonth(20)), None, 0, 11, 2095.45, 95.45, 200.00, 295.45),
    ("1.b.ii.c", Seq(debit(2000.00, "2016-01-31")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), None, 0, 11, 2090.24, 90.24, 200.00, 290.24),
    ("1.d", Seq(debit(2000.00, "2017-01-31")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), None, 0, 21, 2019.54, 19.54, 100.00, 119.54),
    ("1.e", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 2000, 11, 3023.76, 23.76, 100.00, 123.76),
    ("1.f", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 2500, 11, 4032.92, 32.92, 150.00, 182.92),
    ("2.a", Seq(debit(2000.00, "2017-03-31")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), None, 0, 11, 2023.7, 23.7, 200.00, 223.7),
    ("2.b", Seq(debit(2000.00, "2017-03-18")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 1000, 11, 2012.83, 12.83, 100.00, 112.83),
    ("2.c", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 2000, 11, 4025.51, 25.51, 200.00, 225.51),
    ("2.d", Seq(debit(2000.00, "2017-03-18"), debit(2000.00, "2017-03-19")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 2500, 11, 4019.13, 19.13, 150.00, 169.13),
    ("2.e", Seq(debit(2000.00, "2017-03-31")), date("2017-03-11"), date("2018-01-20"), date("2017-04-20"), Some(ArrangementDayOfMonth(20)), Some(PaymentToday(true)), 1000, 11, 2011.85, 11.85, 100.00, 111.85)
  )

  forAll(interestCalculationScenarios) { (id, debits, startDate, endDate, firstPaymentDate, maybeArrangementDayOfMonth, maybePaymentToday, initialPayment, duration, totalPayable, totalInterestCharged, regularInstalmentAmount, finalInstalmentAmount) =>
    s"The calculator service should, for $id calculate totalInterestCharged of $totalInterestCharged with totalPayable of $totalPayable, regularInstalmentAmount of $regularInstalmentAmount and finalInstalmentAmount of $finalInstalmentAmount" in {

      val calculation = TaxPaymentPlan(debits, initialPayment, startDate, endDate, Some(firstPaymentDate))

      val paymentsCalendar = paymentDatesService.paymentsCalendar(maybePaymentToday, maybeArrangementDayOfMonth, startDate)(appConfig)
      val upfrontPaymentAmount = initialPayment
      val regularPaymentAmount = regularInstalmentAmount
      val payables = Payables(liabilities = debits)

      val schedule: PaymentSchedule = calculatorService.buildScheduleNew(
        paymentsCalendar,
        upfrontPaymentAmount,
        regularPaymentAmount,
        payables
      ).get

      val amountPaid = schedule.instalments.map { _.amount }.sum

      val totalPaid = amountPaid + schedule.initialPayment

      logger.info(s"Payment Schedule: Initial: ${schedule.initialPayment}, Over ${schedule.instalments.size}, Regular: ${schedule.instalments.head.amount}, Final: ${schedule.instalments.last.amount}, Total: $totalPaid")

      //      totalPaid.doubleValue() shouldBe totalPayable.doubleValue()
      //      schedule.totalInterestCharged.doubleValue() shouldBe totalInterestCharged.doubleValue()

      val instalments = schedule.instalments

      instalments.size shouldBe duration
      instalments.head.amount shouldBe regularInstalmentAmount
      //      instalments.last.amount.doubleValue() shouldBe finalInstalmentAmount.doubleValue()
    }
  }

  val paymentPlanOptionsInputs = Table[String, Seq[TaxLiability], Option[ArrangementDayOfMonth], Option[PaymentToday], Double, Double, Seq[Tuple2[Double, Int]]](
    ("id", "debits", "maybeArrangementDayOfMonth", "maybePaymentToday", "initialPayment", "remainingIncomeAfterSpending", "instalmentsAmountsDurations"),
    ("1.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(21)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("1.ii", Seq(debit(2000.00, "2015-01-31")), Some(ArrangementDayOfMonth(21)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("1.iii", Seq(debit(2000.00, "2016-01-31")), Some(ArrangementDayOfMonth(21)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("2.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(2)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("2.ii", Seq(debit(2000.00, "2015-01-31")), Some(ArrangementDayOfMonth(2)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("2.iii", Seq(debit(2000.00, "2016-01-31")), Some(ArrangementDayOfMonth(2)), None, 0, 1000, Seq((500, 5), (600, 4), (800, 3))),
    ("3.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(21)), Some(PaymentToday(true)), 1000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("3.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), Some(ArrangementDayOfMonth(21)), Some(PaymentToday(true)), 2000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("3.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), Some(ArrangementDayOfMonth(21)), Some(PaymentToday(true)), 2500, 1000, Seq((500, 4), (600, 3), (800, 3))),
    ("4.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(2)), Some(PaymentToday(true)), 1000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("4.ii", Seq(debit(2000.00, "2017-01-31"), debit(1000.00, "2017-02-01")), Some(ArrangementDayOfMonth(2)), Some(PaymentToday(true)), 2000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("4.iii", Seq(debit(2000.00, "2017-01-31"), debit(2000.00, "2017-02-01")), Some(ArrangementDayOfMonth(2)), Some(PaymentToday(true)), 2500, 1000, Seq((500, 4), (600, 3), (800, 3))),
    ("5.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(14)), Some(PaymentToday(true)), 1000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("6.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(2)), Some(PaymentToday(true)), 1000, 1000, Seq((500, 3), (600, 2), (800, 2))),
    ("7.i", Seq(debit(2000.00, "2017-01-31")), Some(ArrangementDayOfMonth(21)), Some(PaymentToday(true)), 1000, 1000, Seq((500, 3), (600, 2), (800, 2)))
  )

  forAll(paymentPlanOptionsInputs) { (id, debits, mayBeArrangementDayOfMonth, maybePaymentToday, initialPayment, remainingIncomeAfterSpending, instalmentsAmountsDurations) =>
    s"The calculator service should, for $id, generate payment plan options" in {

      val selfAssessmentDetails = SelfAssessmentDetails(
        SaUtr("***"),
        CommunicationPreferences(false, false, false, false),
        debits.map(debit => Debit(
          "***",
          debit.amount,
          debit.dueDate,
          None,
          debit.dueDate
        )),
        Seq()
      )

      val result = calculatorService.paymentPlanOptions(
        selfAssessmentDetails,
        initialPayment,
        mayBeArrangementDayOfMonth,
        remainingIncomeAfterSpending,
        maybePaymentToday,
        date("2018-01-20")
      )(appConfig)

      val resultsZippedWithExpectation = result.map(_.instalments).zip(instalmentsAmountsDurations)

      logger.info(
        s"""Payment Plans Options:
            |First option instalments: startDate: ${result(0).instalments}
            |Second option instalments: ${result(1).instalments}
            |Third option instalments: ${result(2).instalments}
    """)

      resultsZippedWithExpectation.foreach(pair => {
        pair._1.head.amount shouldBe pair._2._1
        pair._1.length shouldBe pair._2._2
      })
    }
  }
}
