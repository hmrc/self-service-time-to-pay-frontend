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

  val fixedToday: LocalDate = date("2023-03-01")

  "CalculatorService" - {
    ".buildSchedule returns a valid payment schedule or nothing if none is possible" - {
      "no late payment interest" - {
        "single liability" - {
          "no upfront payment" - {
            "one instalment" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            "multiple instalments" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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

          }
          "upfront payment" - {
            "one instalment" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            "multiple instalments" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
          }
        }
        "multiple liabilities" - {
          "no upfront payment" - {
            "one instalment" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            "multiple instalments" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
          }
          "upfront payment" - {
            "one instalment" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            "multiple instalments" in {
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
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
          }
        }
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
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
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
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
            regularPaymentsDay      = preferredPaymentDay
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
          planStartDate           = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay      = preferredPaymentDay
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
          planStartDate           = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessUpfrontPayment)),
          regularPaymentsDay      = preferredPaymentDay
        )

        val result = calculatorService.buildScheduleNew(paymentsCalendar, upfrontPaymentAmount, regularPaymentAmount, payables)

        result shouldBe None
      }
//      "return a payment schedule request with no initial payment when the user tries to make a payment which would leave less than £32 balance when" - {
//        "the current date is Friday 1st May with upcoming bank holiday" in {
//          val clock = clockForMay(_1st)
//          val currentDate = LocalDate.now(clock)
//          val firstPaymentDate = Some(june(_11th))
//
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = july(_1st), firstPaymentDate)
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = august(_1st), firstPaymentDate)
//        }
//
//        "the current date is Thursday 7th May with upcoming bank holiday" in {
//          val clock = clockForMay(_7th)
//          val currentDate = LocalDate.now(clock)
//          val firstPaymentDate = Some(june(_15th))
//
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = july(_7th), firstPaymentDate)
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = august(_7th), firstPaymentDate)
//        }
//
//        "the current date is bank holiday Friday 8th May" in {
//          val clock = clockForMay(_8th)
//          val currentDate = LocalDate.now(clock)
//          val firstPaymentDate = Some(june(_15th))
//
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = july(_8th), firstPaymentDate)
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = august(_8th), firstPaymentDate)
//        }
//
//        "the current date is Monday 11th May" in {
//          val clock = clockForMay(_11th)
//          val currentDate = LocalDate.now(clock)
//          val firstPaymentDate = Some(june(_18th))
//
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = july(_11th), firstPaymentDate)
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = august(_11th), firstPaymentDate)
//        }
//
//        "the current date is the Monday 25th May so the payment dates roll into the next month" in {
//          val clock = clockForMay(_25th)
//          val currentDate = may(_25th)
//          val firstPaymentDate = july(_1st)
//
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, oneMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = july(_25th), firstPaymentDate = Some(firstPaymentDate))
//          makeTaxPaymentPlan(debits, initialPaymentTooLarge, twoMonthDuration)(clock) shouldBe TaxPaymentPlan(
//            debits, noInitialPayment, currentDate, endDate = august(_25th), firstPaymentDate = Some(firstPaymentDate))
//        }
//      }
//      "availablePaymentSchedules with an endDate" should {
//        implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
//        val LastPaymentDelayDays = 7
//
//        "return a payment schedule with endDate" when {
//          "matching last payment date plush seven days" in {
//            val startDate = LocalDate.now
//            val endDate = startDate.plusMonths(3)
//
//            val taxPaymentPlan = TaxPaymentPlan(
//              Seq(
//                TaxLiability(1000, startDate)
//              ),
//              0,
//              startDate,
//              endDate,
//              None)
//
//            val calculatorService = new CalculatorService(new ClockProvider(), new DurationService(), new InterestRateService())
//
//            val result: PaymentSchedule = calculatorService.buildSchedule(taxPaymentPlan)
//
//            result.endDate shouldBe result.instalments.last.paymentDate.plusDays(LastPaymentDelayDays)
//          }
//
//          "ignoring public holidays,  last payment date plush seven days" in {
//            val startDate = LocalDate.of(2022, 6, 1)
//            val endDate = startDate.plusMonths(6)
//
//            val taxPaymentPlan = TaxPaymentPlan(
//              Seq(
//                TaxLiability(1000, startDate)
//              ),
//              0,
//              startDate,
//              endDate,
//              None)
//
//            val calculatorService = new CalculatorService(new ClockProvider(), new DurationService(), new InterestRateService())
//
//            val result: PaymentSchedule = calculatorService.buildSchedule(taxPaymentPlan)
//
//            result.endDate shouldBe result.instalments.last.paymentDate.plusDays(LastPaymentDelayDays)
//          }
//        }
//      }
    }
  }
}

