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
import play.api.Logger
import ssttpcalculator.model.{Instalment, InterestRate, Payables, Payment, PaymentsCalendar, TaxLiability}
import testsupport.ItSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.{LocalDate, Year}

class CalculatorServiceSpec2023 extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  implicit val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  def date(date: String): LocalDate = LocalDate.parse(date)

  def approximatelyEqual(a: BigDecimal, b: BigDecimal): Boolean = (a - b) < threshold && (b - a) < threshold
  val threshold = 0.000001


  def interestSincePlanStartDate(interestRateCalculator: LocalDate => InterestRate)(startDate: LocalDate, payment: Payment): BigDecimal = {
    val currentInterestRate = interestRateCalculator(startDate).rate
    val currentDailyRate = currentInterestRate / BigDecimal(Year.of(startDate.getYear).length()) / BigDecimal(100)
    val daysInterestToCharge = BigDecimal(durationService.getDaysBetween(startDate, payment.date))
    payment.amount * currentDailyRate * daysInterestToCharge
  }

  import testsupport.testdata.CalculatorDataGenerator.newCalculatorModel._

  val fixedToday: LocalDate = date("2023-03-01")

  "CalculatorService" - {
    ".buildSchedule returns a valid payment schedule or nothing if none is possible" - {
      "single liability, no upfront payment, one instalment, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 1000

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments shouldBe
          Seq(Instalment(paymentDate = fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth), amount = 1000, interest = 0))
      }
      "single liability, upfront payment, one instalment, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 500
        val regularPaymentAmount = 1000

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments shouldBe
          Seq(Instalment(paymentDate = fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth), amount = 500, interest = 0))
      }
      "single liability, no upfront payment, multiple instalments, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 100

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(9)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments.length shouldBe 10
        result.instalments.headOption.get.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.instalments.foreach(instalment => {
          instalment.paymentDate.getDayOfMonth shouldBe regularPaymentsDayWithinFirstMonth
          instalment.amount shouldBe 100
          instalment.interest shouldBe 0
        })
      }
      "single liability, upfront payment, multiple instalments, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 200
        val regularPaymentAmount = 100

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(7)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments.length shouldBe 8
        result.instalments.headOption.get.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.instalments.foreach(instalment => {
          instalment.paymentDate.getDayOfMonth shouldBe regularPaymentsDayWithinFirstMonth
          instalment.amount shouldBe 100
          instalment.interest shouldBe 0
        })
      }
      "multiple liabilities, no upfront payment, one instalment, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 250
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables((1 to 4).map(_ => liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 1000

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments shouldBe
          Seq(Instalment(paymentDate = fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth), amount = 1000, interest = 0))
      }
      "multiple liabilities, upfront payment, one instalment, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 250
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables((1 to 4).map(_ => liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 500
        val regularPaymentAmount = 1000

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments shouldBe
          Seq(Instalment(paymentDate = fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth), amount = 500, interest = 0))
      }
      "multiple liabilities, no upfront payment, multiple instalments, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 250
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables((1 to 4).map(_ => liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 100

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = None,
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(9)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments.length shouldBe 10
        result.instalments.headOption.get.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.instalments.foreach(instalment => {
          instalment.paymentDate.getDayOfMonth shouldBe regularPaymentsDayWithinFirstMonth
          instalment.amount shouldBe 100
          instalment.interest shouldBe 0
        })
      }
      "multiple liabilities, upfront payment, multiple instalments, no late payment interest, first instalment at end of starting month" in {
        val liabilityAmount = 250
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables((1 to 4).map(_ => liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 200
        val regularPaymentAmount = 100

        val regularPaymentsDayWithinFirstMonth = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = regularPaymentsDayWithinFirstMonth
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(7)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments.length shouldBe 8
        result.instalments.headOption.get.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
        result.instalments.foreach(instalment => {
          instalment.paymentDate.getDayOfMonth shouldBe regularPaymentsDayWithinFirstMonth
          instalment.amount shouldBe 100
          instalment.interest shouldBe 0
        })
      }
      "late payment interest" - {
        "two instalments" in {
          val liabilityAmount = 1000
          val liabilityDueDate = fixedToday.minusMonths(6)
          val liability = TaxLiability(liabilityAmount, liabilityDueDate)
          val payables = Payables(Seq(liability))
          val sumOfPayables = payables.liabilities.map(_.amount).sum

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 1000

          val regularPaymentsDayWithinFirstMonth = fixedToday
            .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay = regularPaymentsDayWithinFirstMonth
          )

          val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

          result.startDate shouldBe fixedToday
          result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(1)
          result.initialPayment shouldBe upfrontPaymentAmount
          result.amountToPay shouldBe sumOfPayables
          result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
          result.totalInterestCharged > 0 shouldBe true
          result.totalPayable > sumOfPayables shouldBe true
          result.instalments.length shouldBe 2

          val firstInstalment = result.instalments.headOption.get
          firstInstalment.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth)
          firstInstalment.amount shouldBe 1000
          firstInstalment.interest > 0 shouldBe true

          val finalInstalment = result.instalments.last
          finalInstalment.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(1)
          finalInstalment.amount > 0 shouldBe true
          finalInstalment.amount <= 1000 shouldBe true
          println("finalInstalment.amount: " + finalInstalment.amount)
          println("result.totalInterestCharged" + result.totalInterestCharged)
          approximatelyEqual(finalInstalment.amount, result.totalInterestCharged) shouldBe true
          finalInstalment.interest shouldBe 0
        }
        "more than two instalments" in {
          val liabilityAmount = 1000
          val liabilityDueDate = fixedToday.minusMonths(6)
          val liability = TaxLiability(liabilityAmount, liabilityDueDate)
          val payables = Payables(Seq(liability))
          val sumOfPayables = payables.liabilities.map(_.amount).sum

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 250

          val regularPaymentsDayWithinFirstMonth = fixedToday
            .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay = regularPaymentsDayWithinFirstMonth
          )

          val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

          result.startDate shouldBe fixedToday
          result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(4)
          result.initialPayment shouldBe upfrontPaymentAmount
          result.amountToPay shouldBe sumOfPayables
          result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
          result.totalInterestCharged > 0 shouldBe true
          result.totalPayable > sumOfPayables shouldBe true
          result.instalments.length shouldBe 5

          result.instalments.init.foreach(instalment => {
            instalment.paymentDate.getDayOfMonth shouldBe regularPaymentsDayWithinFirstMonth
            instalment.amount shouldBe 250
            instalment.interest > 0 shouldBe true
          })

          val finalInstalment = result.instalments.last
          finalInstalment.paymentDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(4)
          finalInstalment.amount > 0 shouldBe true
          finalInstalment.amount <= 250 shouldBe true
          println("finalInstalment.amount: " + finalInstalment.amount)
          println("result.totalInterestCharged" + result.totalInterestCharged)
          approximatelyEqual(finalInstalment.amount, result.totalInterestCharged) shouldBe true
          finalInstalment.interest shouldBe 0
        }
        "late payment interest means schedule of no more than 24 months is not possible" in {
          val liabilityAmount = 2400
          val liabilityDueDate = fixedToday
          val liability = TaxLiability(liabilityAmount, liabilityDueDate)
          val payables = Payables(Seq(liability))

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 100
          val preferredPaymentDay = fixedToday
            .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments - 1)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate = fixedToday,
            maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
            regularPaymentsDay = preferredPaymentDay
          )

          val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables)

          result shouldBe None

        }
      }
      "single liability, upfront payment, multiple instalments, no late payment interest, first instalment into next month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 200
        val regularPaymentAmount = 100
        val preferredPaymentDay = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments - 1)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = preferredPaymentDay
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(preferredPaymentDay).plusMonths(8)
        result.initialPayment shouldBe upfrontPaymentAmount
        result.amountToPay shouldBe sumOfPayables
        result.instalmentBalance shouldBe sumOfPayables - upfrontPaymentAmount
        result.totalInterestCharged shouldBe 0
        result.totalPayable shouldBe sumOfPayables
        result.instalments.length shouldBe 8
        result.instalments.headOption.get.paymentDate shouldBe fixedToday.withDayOfMonth(preferredPaymentDay).plusMonths(1)
        result.instalments.foreach(instalment => {
          instalment.paymentDate.getDayOfMonth shouldBe preferredPaymentDay
          instalment.amount shouldBe 100
          instalment.interest shouldBe 0
        })
      }
      "no schedule if cannot pay off within 24 month" in {
        val liabilityAmount = 2500
        val liabilityDueDate = fixedToday.plusMonths(26)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val payables = Payables(Seq(liability))

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 100
        val preferredPaymentDay = fixedToday
          .plusDays(appConfig.daysToProcessUpfrontPayment + appConfig.minGapBetweenPayments - 1)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay = preferredPaymentDay
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables)

        result shouldBe None
      }
    }





    //    ".totalHistoricInterest" - {
    //      "returns the total interest accrued across the payables from their respective due dates to the journey date (today)" - {
    //        "returns 0 where there is one payable not past its due date" in {
    //          val liabilityAmount = 1000
    //          val liabilityDueDate = date("2023-07-31")
    //          val liability = TaxLiability(liabilityAmount, liabilityDueDate)
    //          val payables = Payables(Seq(liability))
    //
    //          calculatorService.totalHistoricInterest(
    //            payables         = payables,
    //            paymentsCalendar = paymentsCalendar,
    //            periodToRates    = fixedInterestRates()
    //          ) shouldBe 0
    //
    //        }
    //        "returns 0 where there are multiple payables none past their due dates" in {
    //          val firstLiabilityAmount = 1000
    //          val firstLiabilityDueDate = date("2023-07-31")
    //          val firstLiability = TaxLiability(firstLiabilityAmount, firstLiabilityDueDate)
    //          val secondLiabilityAmount = 1000
    //          val secondLiabilityDueDate = date("2024-01-31")
    //          val secondLiability = TaxLiability(secondLiabilityAmount, secondLiabilityDueDate)
    //          val payables = Payables(Seq(firstLiability, secondLiability))
    //
    //          calculatorService.totalHistoricInterest(
    //            payables         = payables,
    //            paymentsCalendar = paymentsCalendar,
    //            periodToRates    = fixedInterestRates()
    //          ) shouldBe 0
    //
    //        }
    //        "returns interest only on payables past due date, where some but not all payables are past their due dates" in {
    //          val firstDebtAmount = 1000
    //          val firstDebtDueDate = date("2022-01-31")
    //          val firstDebt = TaxLiability(firstDebtAmount, firstDebtDueDate)
    //          val secondDebtAmount = 1000
    //          val secondDebtDueDate = date("2022-07-31")
    //          val secondDebt = TaxLiability(secondDebtAmount, secondDebtDueDate)
    //          val thirdLiabilityAmount = 1000
    //          val thirdLiabilityDueDate = date("2023-07-31")
    //          val thirdLiability = TaxLiability(thirdLiabilityAmount, thirdLiabilityDueDate)
    //          val fourthLiabilityAmount = 1000
    //          val fourthLiabilityDueDate = date("2024-01-31")
    //          val fourthLiability = TaxLiability(fourthLiabilityAmount, fourthLiabilityDueDate)
    //
    //          val payables = Payables(Seq(firstDebt, secondDebt, thirdLiability, fourthLiability))
    //
    //          val dateOfCalculation = paymentsCalendar.planStartDate
    //
    //          val expectedHistoricInterestOnFirstDebt = interestSincePlanStartDate(fixedInterestRate())(firstDebtDueDate, Payment(dateOfCalculation, firstDebtAmount))
    //          val expectedHistoricInterestOnSecondDebt = interestSincePlanStartDate(fixedInterestRate())(secondDebtDueDate, Payment(dateOfCalculation, secondDebtAmount))
    //
    //          calculatorService.totalHistoricInterest(
    //            payables         = payables,
    //            paymentsCalendar = paymentsCalendar,
    //            periodToRates    = fixedInterestRates()
    //          ) shouldBe expectedHistoricInterestOnFirstDebt + expectedHistoricInterestOnSecondDebt
    //
    //        }
    //      }
    //    }
    //    ".payablesLessUpfrontPayment" - {
    //      "returns a copy of payables with the upfront payment removed" - {
    //        "where the upfront payment is 0, payables is returned unchanged" in {
    //          calculatorService.payablesLessUpfrontPayment(
    //            upfrontPaymentAmount = 0,
    //            payables             = payablesWithOne2000LiabilityNoDueDate,
    //          ) shouldBe payablesWithOne2000LiabilityNoDueDate
    //        }
    //        "where no interest is payable on any liabilities" - {
    //          "if upfront payment is less the first liability" - {
    //            "returns a copy of payables with first reduced by upfront payment" in {
    //              val result = calculatorService.payablesLessUpfrontPayment(
    //                upfrontPaymentAmount = 500,
    //                payables             = payablesWithOne2000LiabilityNoDueDate,
    //              )
    //
    //              result.liabilities.length shouldEqual (payablesWithOne2000LiabilityNoDueDate.liabilities.length)
    //              result.liabilities.head.amount shouldBe 1500
    //            }
    //          }
    //          "if upfront payment is more than the first liability but less than first two" - {
    //            "returns a copy of payables with first removed and second reduced by the remainder" in {
    //              calculatorService.payablesLessUpfrontPayment(
    //                upfrontPaymentAmount = 2000,
    //                payables             = payablesWithTwoLiabilitiesNoDueDate,
    //              ) shouldBe Payables(Seq(
    //                anotherPaymentOnAccountNoInterestPayable.copy(amount = 1000)
    //              ))
    //            }
    //          }
    //          "if upfront payment covers all the liabilities" - {
    //            "returns a payables with no liabilities" in {
    //              calculatorService.payablesLessUpfrontPayment(
    //                upfrontPaymentAmount = 3000,
    //                payables             = payablesWithTwoLiabilitiesNoDueDate,
    //              ) shouldBe Payables(Seq())
    //            }
    //          }
    //        }
    //        "where interest in payable on liabilities" - {
    //          "if upfront payment is less the first liability" - {
    //            "returns payables with first liability reduced and late payment interest added at end" in {
    //              val upfrontPaymentAmount = 800
    //
    //              val firstDebtDueDate = date("2022-03-17")
    //              val taxLiability = TaxLiability(amount  = 1000, dueDate = firstDebtDueDate)
    //
    //              val payablesInitially = Payables(Seq(taxLiability))
    //
    //              val result = calculatorService.payablesLessUpfrontPayment(
    //                upfrontPaymentAmount,
    //                payablesInitially,
    //              )
    //
    //              result.liabilities.head shouldEqual taxLiability.copy(amount = taxLiability.amount - upfrontPaymentAmount)
    //
    //              val upfrontPaymentDateAsToday = paymentsCalendar.planStartDate
    //              val expectedUpfrontPayment = Payment(upfrontPaymentDateAsToday, upfrontPaymentAmount)
    //              val expectedInterestAccruedOnFirstDebt = interestSincePlanStartDate(fixedInterestRate())(firstDebtDueDate, expectedUpfrontPayment)
    //              result.liabilities.last.amount shouldBe expectedInterestAccruedOnFirstDebt
    //            }
    //          }
    //          "if upfront payment is more than first liability and less than first two" - {
    //            "returns payables with first liability removed, second reduced, and late payment interest against first two liabilities added at end" in {
    //              val firstDebtAmount = 1000
    //              val firstDebtDueDate = date("2022-03-17")
    //              val firstDebt = TaxLiability(firstDebtAmount, firstDebtDueDate)
    //
    //              val secondDebtAmount = 1000
    //              val secondDebtDueDate = date("2022-09-17")
    //              val secondDebt = TaxLiability(secondDebtAmount, secondDebtDueDate)
    //
    //              val payablesInitially = Payables(liabilities = Seq(firstDebt, secondDebt))
    //
    //              val upfrontPaymentAmount = 1200
    //
    //              val upfrontPaymentDateAsJourneyToday = paymentsCalendar.planStartDate
    //              val upfrontPaymentAgainstFirstDebt = Payment(upfrontPaymentDateAsJourneyToday, firstDebtAmount)
    //              val expectedInterestAccruedOnFirstDebt = interestSincePlanStartDate(fixedInterestRate())(firstDebtDueDate, upfrontPaymentAgainstFirstDebt)
    //
    //              val upfrontPaymentAgainstSecondDebt = Payment(upfrontPaymentDateAsJourneyToday, upfrontPaymentAmount - firstDebtAmount)
    //              val expectedInterestAccruedOnSecondDebt = interestSincePlanStartDate(fixedInterestRate())(secondDebtDueDate, upfrontPaymentAgainstSecondDebt)
    //
    //              val result = calculatorService.payablesLessUpfrontPayment(
    //                upfrontPaymentAmount,
    //                payablesInitially,
    //              )
    //
    //              result.liabilities.length shouldEqual payablesInitially.liabilities.length
    //              result.liabilities.head shouldBe secondDebt.copy(amount = secondDebtAmount - (upfrontPaymentAmount - firstDebtAmount))
    //              result.liabilities.last.amount shouldEqual expectedInterestAccruedOnFirstDebt + expectedInterestAccruedOnSecondDebt
    //            }
    //          }
    //        }
    //      }
    //    }
    //
    //    ".regularInstalments" - {
    //      "generates a list of monthly payment instalments at the regular payment amount" +
    //        " until the payables (including any late payment interest payable) are cleared" - {
    //          "where no interest is payable on any liabilities" - {
    //            "if balance to pay is 0, creates an empty list (no instalments)" in {
    //              val regularPaymentAmount = 500
    //
    //              calculatorService.regularInstalments(
    //                paymentsCalendar     = paymentsCalendar,
    //                regularPaymentAmount = regularPaymentAmount,
    //                payables             = Payables(Seq()),
    //                dateToRate           = fixedZeroInterest
    //              ) shouldBe Seq()
    //
    //            }
    //            "if balance to pay <= regular payment amount creates list with single instalment of the balance to pay" in {
    //              val regularPaymentAmount = 2000
    //
    //              println(s"paymentsCalendar: $paymentsCalendar")
    //              println(s"paymentsCalendar regular payment dates ${paymentsCalendar.regularPaymentDates}")
    //
    //              calculatorService.regularInstalments(
    //                paymentsCalendar     = paymentsCalendar,
    //                regularPaymentAmount = regularPaymentAmount,
    //                payables             = payablesWithOne2000LiabilityNoDueDate,
    //                dateToRate           = fixedZeroInterest
    //              ) shouldBe Seq(
    //                Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
    //              )
    //            }
    //            "if balance to pay > regular payment amount, creates list of multiple instalments" - {
    //              "if balance to pay divisible by regular payment amount all instalments are exactly regular amount" in {
    //                val regularPaymentAmount = 500
    //
    //                calculatorService.regularInstalments(
    //                  paymentsCalendar     = paymentsCalendar,
    //                  regularPaymentAmount = regularPaymentAmount,
    //                  payables             = payablesWithOne2000LiabilityNoDueDate,
    //                  dateToRate           = fixedZeroInterest
    //                ) shouldBe Seq(
    //                  Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
    //                  Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
    //                  Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
    //                  Instalment(paymentDate = date("2023-06-17"), amount = regularPaymentAmount, interest = 0)
    //                )
    //              }
    //              "if balance to pay not exactly divisible by regular payment amount all instalments = regular payment amount" +
    //                ", except last which is less" in {
    //                  val regularPaymentAmount = 600
    //
    //                  calculatorService.regularInstalments(
    //                    paymentsCalendar     = paymentsCalendar,
    //                    regularPaymentAmount = regularPaymentAmount,
    //                    payables             = payablesWithOne2000LiabilityNoDueDate,
    //                    dateToRate           = fixedZeroInterest
    //                  ) shouldBe Seq(
    //                    Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = 0),
    //                    Instalment(paymentDate = date("2023-04-17"), amount = regularPaymentAmount, interest = 0),
    //                    Instalment(paymentDate = date("2023-05-17"), amount = regularPaymentAmount, interest = 0),
    //                    Instalment(paymentDate = date("2023-06-17"), amount = 200, interest = 0)
    //                  )
    //                }
    //            }
    //          }
    //          "where interest is payable on all liabilities" - {
    //            "single liability" in {
    //              val debtAmount = 1000
    //              val dueDate = date("2022-03-17")
    //              val payables = Payables(liabilities = Seq(TaxLiability(debtAmount, dueDate)))
    //
    //              val regularPaymentAmount = 1000
    //
    //              val expectedFirstInstalmentPaymentDate = paymentsCalendar.regularPaymentDates.head
    //              val expectedFirstPayment = Payment(expectedFirstInstalmentPaymentDate, regularPaymentAmount)
    //
    //              val expectedInterestAccrued = interestSincePlanStartDate(fixedInterestRate(1))(paymentsCalendar.planStartDate, expectedFirstPayment)
    //
    //              calculatorService.regularInstalments(
    //                paymentsCalendar     = paymentsCalendar,
    //                regularPaymentAmount = regularPaymentAmount,
    //                payables             = payables,
    //                dateToRate           = fixedInterestRate(1)) shouldBe Seq(
    //                  Instalment(date("2023-03-17"), regularPaymentAmount, expectedInterestAccrued),
    //                  Instalment(date("2023-04-17"), expectedInterestAccrued, 0)
    //                )
    //            }
    //            "multiple liabilities" - {
    //              "two debts, paid in two months" in {
    //                val firstDebtAmount = 1000
    //                val firstDebtDueDate = date("2022-03-17")
    //                val secondDebtAmount = 100
    //                val secondDebtDueDate = date("2022-09-17")
    //                val payables = Payables(liabilities = Seq(
    //                  TaxLiability(firstDebtAmount, firstDebtDueDate),
    //                  TaxLiability(secondDebtAmount, secondDebtDueDate)
    //                ))
    //
    //                val regularPaymentAmount = 1000
    //
    //                val expectedFirstInstalmentPaymentDate = paymentsCalendar.regularPaymentDates.head
    //                val expectedFirstPayment = Payment(expectedFirstInstalmentPaymentDate, regularPaymentAmount)
    //                val expectedInterestAccruedOnFirstDebt = interestSincePlanStartDate(fixedInterestRate())(paymentsCalendar.planStartDate, expectedFirstPayment)
    //
    //                val expectedSecondInstalmentPaymentDate = paymentsCalendar.regularPaymentDates(1)
    //                val expectedSecondPayment = Payment(expectedSecondInstalmentPaymentDate, secondDebtAmount)
    //                val expectedInterestAccruedOnSecondDebt = interestSincePlanStartDate(fixedInterestRate())(paymentsCalendar.planStartDate, expectedSecondPayment)
    //
    //                calculatorService.regularInstalments(
    //                  paymentsCalendar,
    //                  regularPaymentAmount,
    //                  payables,
    //                  fixedInterestRate(1)
    //                ) shouldBe Seq(
    //                    Instalment(paymentDate = date("2023-03-17"), amount = regularPaymentAmount, interest = expectedInterestAccruedOnFirstDebt),
    //                    Instalment(
    //                      paymentDate = date("2023-04-17"),
    //                      amount      = secondDebtAmount + expectedInterestAccruedOnFirstDebt + expectedInterestAccruedOnSecondDebt,
    //                      interest    = expectedInterestAccruedOnSecondDebt
    //                    )
    //                  )
    //              }
    //              "two debts, paid in more than two months" - {
    //                "with final month clearing late payment interest" in {
    //                  val payables = testPayables((5000, date("2022-03-17")), (5000, date("2022-09-17")))
    //                  val regularPaymentAmount = 1000
    //
    //                  val result = calculatorService.regularInstalments(
    //                    paymentsCalendar,
    //                    regularPaymentAmount,
    //                    payables,
    //                    fixedInterestRate(1)
    //                  ).get
    //
    //                  result.length shouldBe 11
    //                  result.init.foreach(instalment => {
    //                    instalment.amount shouldBe 1000
    //                    instalment.interest > 0 shouldBe true
    //                  })
    //                  result.last.amount <= 1000 shouldBe true
    //                  result.map(_.interest).sum shouldEqual result.last.amount
    //                }
    //                "with late payment interest over multiple final months" in {
    //                  val payables = testPayables((5000, date("2022-03-17")), (5000, date("2022-09-17")))
    //                  val regularPaymentAmount = 1000
    //
    //                  val result = calculatorService.regularInstalments(
    //                    paymentsCalendar,
    //                    regularPaymentAmount,
    //                    payables,
    //                    fixedInterestRate(30)
    //                  ).get
    //
    //                  result.length > 11 shouldBe true
    //                  result.init.foreach(instalment => instalment.amount shouldBe 1000)
    //                  result.slice(0, -2).foreach(instalment => instalment.interest > 0 shouldBe true)
    //                  result.last.amount <= 1000 shouldBe true
    //                  result.map(_.interest).sum shouldEqual result.last.amount + result.init.last.amount
    //                }
    //              }
    //            }
    //          }
    //        }
    //    }
    //  }
  }
}

