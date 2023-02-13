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
import journey.PaymentToday
import play.api.Logger
import ssttpcalculator.model.{Instalment, InterestRate, Payables, Payment, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import testsupport.ItSpec
import timetopaytaxpayer.cor.model.{CommunicationPreferences, Debit, SaUtr, SelfAssessmentDetails}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

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
              val liabilities = Seq(liability)
              val payables = Payables(Seq(liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 0
              val regularPaymentAmount = 1000

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                paymentsCalendar.maybeUpfrontPaymentDate,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = Seq(liability)
              val payables = Payables(Seq(liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 0
              val regularPaymentAmount = 100

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                paymentsCalendar.maybeUpfrontPaymentDate,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(9).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = Seq(liability)
              val payables = Payables(Seq(liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 500
              val regularPaymentAmount = 1000

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              println(s"regular payments day within first month: $regularPaymentsDayWithinFirstMonth")

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              println(s"paymentsCalendar.regularPaymentDates: ${paymentsCalendar.regularPaymentDates}")

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                Some(paymentsCalendar.regularPaymentDates.head),
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount,
                maybePaymentToday = Some(PaymentToday(true))
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              println(s"result: $result")

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = Seq(liability)
              val payables = Payables(Seq(liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 200
              val regularPaymentAmount = 100

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                Some(paymentsCalendar.regularPaymentDates.head),
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(7).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = (1 to 4).map(_ => liability)
              val payables = Payables((1 to 4).map(_ => liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 0
              val regularPaymentAmount = 1000

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                paymentsCalendar.maybeUpfrontPaymentDate,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = (1 to 4).map(_ => liability)
              val payables = Payables((1 to 4).map(_ => liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 0
              val regularPaymentAmount = 100

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = None,
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                paymentsCalendar.maybeUpfrontPaymentDate,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(9).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = (1 to 4).map(_ => liability)
              val payables = Payables((1 to 4).map(_ => liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 500
              val regularPaymentAmount = 1000

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                Some(paymentsCalendar.regularPaymentDates.head),
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusDays(appConfig.lastPaymentDelayDays)
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
              val liabilities = (1 to 4).map(_ => liability)
              val payables = Payables((1 to 4).map(_ => liability))
              val sumOfPayables = payables.liabilities.map(_.amount).sum

              val upfrontPaymentAmount = 200
              val regularPaymentAmount = 100

              val regularPaymentsDayWithinFirstMonth = fixedToday
                .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
                .getDayOfMonth

              val paymentsCalendar = PaymentsCalendar(
                planStartDate           = fixedToday,
                maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
                regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
              )

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                LocalDate.parse("2017-03-11"),
                Some(paymentsCalendar.regularPaymentDates.head),
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                regularPaymentAmount
              )

              val result = calculatorService.schedule(taxPaymentPlan).get

              result.startDate shouldBe fixedToday
              result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(7).plusDays(appConfig.lastPaymentDelayDays)
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
          val liabilities = Seq(liability)
          val payables = Payables(Seq(liability))
          val sumOfPayables = payables.liabilities.map(_.amount).sum

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 1000

          val regularPaymentsDayWithinFirstMonth = fixedToday
            .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
          )

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            LocalDate.parse("2017-03-11"),
            paymentsCalendar.maybeUpfrontPaymentDate,
            Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
            regularPaymentAmount
          )

          val result = calculatorService.schedule(taxPaymentPlan).get

          result.startDate shouldBe fixedToday
          result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(1).plusDays(appConfig.lastPaymentDelayDays)
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
          val liabilities = Seq(liability)
          val payables = Payables(Seq(liability))
          val sumOfPayables = payables.liabilities.map(_.amount).sum

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 250

          val regularPaymentsDayWithinFirstMonth = fixedToday
            .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = None,
            regularPaymentsDay      = regularPaymentsDayWithinFirstMonth
          )

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            LocalDate.parse("2017-03-11"),
            paymentsCalendar.maybeUpfrontPaymentDate,
            Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
            regularPaymentAmount
          )

          val result = calculatorService.schedule(taxPaymentPlan).get

          result.startDate shouldBe fixedToday
          result.endDate shouldBe fixedToday.withDayOfMonth(regularPaymentsDayWithinFirstMonth).plusMonths(4).plusDays(appConfig.lastPaymentDelayDays)
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
          val liabilities = Seq(liability)
          val payables = Payables(Seq(liability))

          val upfrontPaymentAmount = 0
          val regularPaymentAmount = 100
          val preferredPaymentDay = fixedToday
            .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments - 1)
            .getDayOfMonth

          val paymentsCalendar = PaymentsCalendar(
            planStartDate           = fixedToday,
            maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
            regularPaymentsDay      = preferredPaymentDay
          )

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            LocalDate.parse("2017-03-11"),
            paymentsCalendar.maybeUpfrontPaymentDate,
            Some(ArrangementDayOfMonth(preferredPaymentDay)),
            regularPaymentAmount
          )

          val result = calculatorService.schedule(taxPaymentPlan)

          result shouldBe None

        }
      }
      "single liability, upfront payment, multiple instalments, no late payment interest, first instalment into next month" in {
        val liabilityAmount = 1000
        val liabilityDueDate = fixedToday.plusMonths(12)
        val liability = TaxLiability(liabilityAmount, liabilityDueDate)
        val liabilities = Seq(liability)
        val payables = Payables(Seq(liability))
        val sumOfPayables = payables.liabilities.map(_.amount).sum

        val upfrontPaymentAmount = 200
        val regularPaymentAmount = 100
        val preferredPaymentDay = fixedToday
          .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments - 1)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate           = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
          regularPaymentsDay      = preferredPaymentDay
        )

        val taxPaymentPlan = TaxPaymentPlan(
          liabilities,
          upfrontPaymentAmount,
          fixedToday,
          LocalDate.parse("2017-03-11"),
          Some(paymentsCalendar.regularPaymentDates.head),
          Some(ArrangementDayOfMonth(preferredPaymentDay)),
          regularPaymentAmount,
          Some(PaymentToday((true)))
        )

        val result = calculatorService.schedule(taxPaymentPlan).get

        result.startDate shouldBe fixedToday
        result.endDate shouldBe fixedToday.withDayOfMonth(preferredPaymentDay).plusMonths(8).plusDays(appConfig.lastPaymentDelayDays)
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
        val liabilities = Seq(liability)
        val payables = Payables(Seq(liability))

        val upfrontPaymentAmount = 0
        val regularPaymentAmount = 100
        val preferredPaymentDay = fixedToday
          .plusDays(appConfig.daysToProcessFirstPayment + appConfig.minGapBetweenPayments - 1)
          .getDayOfMonth

        val paymentsCalendar = PaymentsCalendar(
          planStartDate           = fixedToday,
          maybeUpfrontPaymentDate = Some(fixedToday.plusDays(appConfig.daysToProcessFirstPayment)),
          regularPaymentsDay      = preferredPaymentDay
        )

        val taxPaymentPlan = TaxPaymentPlan(
          liabilities,
          upfrontPaymentAmount,
          fixedToday,
          LocalDate.parse("2017-03-11"),
          paymentsCalendar.maybeUpfrontPaymentDate,
          Some(ArrangementDayOfMonth(preferredPaymentDay)),
          regularPaymentAmount
        )

        val result = calculatorService.schedule(taxPaymentPlan)

        result shouldBe None
      }
    }

    "paymentPlanOptions generates up to three schedules" - {
      "three, if neither first two cover total to pay in a single month" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 5000, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000
        val paymentToday = None
        val dateToday = fixedToday

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

        result.length shouldBe 3
      }
      "plans for  50%, 60% and 80% of remaining income after spending" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 5000, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000
        val paymentToday = None
        val dateToday = fixedToday

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

        result.head.instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.5)
        result(1).instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.6)
        result(2).instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.8)
      }
      "only one, if 50% of remaining income after spending covers total to pay" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 500, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000
        val paymentToday = None
        val dateToday = fixedToday

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

        result.length shouldBe 1
      }
      "two, if 60% of remaining income after spending covers total to pay, but not 50%" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 600, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000
        val paymentToday = None
        val dateToday = fixedToday

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

        result.length shouldBe 2
      }
      "considers late payment interest in total to pay" - {
        "50% covers everything, one plan" in {
          val sa = SelfAssessmentDetails(
            SaUtr("saUtr"),
            CommunicationPreferences(false, false, false, false),
            Seq(Debit("originCode", 500, date("2023-01-31"), None, date("2022-04-05"))),
            Seq()
          )

          val initialPayment = 0
          val preferredPaymentDay = None
          val remainingIncomeAfterSpending = 1100
          val paymentToday = None
          val dateToday = fixedToday

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

          result.length shouldBe 1
        }
        "60% but not 50% covers, two plans" in {
          val sa = SelfAssessmentDetails(
            SaUtr("saUtr"),
            CommunicationPreferences(false, false, false, false),
            Seq(Debit("originCode", 550, date("2023-01-31"), None, date("2022-04-05"))),
            Seq()
          )

          val initialPayment = 0
          val preferredPaymentDay = None
          val remainingIncomeAfterSpending = 1000
          val paymentToday = None
          val dateToday = fixedToday

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending, paymentToday, dateToday)

          result.length shouldBe 2

        }
      }
    }
  }
}
