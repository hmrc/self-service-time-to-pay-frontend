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
import ssttpcalculator.model.{Instalment, InterestRate, Payables, Payment, TaxLiability, TaxPaymentPlan}
import testsupport.ItSpec
import timetopaytaxpayer.cor.model.{CommunicationPreferences, Debit, SaUtr, SelfAssessmentDetails}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDate, LocalDateTime, Year}
class CalculatorServiceSpec2023 extends ItSpec {
  private val logger = Logger(getClass)

  val interestRateService: InterestRateService = fakeApplication().injector.instanceOf[InterestRateService]
  val durationService: DurationService = fakeApplication().injector.instanceOf[DurationService]
  val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

  implicit val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]

  private def fixedClock = {
    val currentDateTime = LocalDateTime.parse("2020-05-02T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  def date(date: String): LocalDate = LocalDate.parse(date)

  def approximatelyEqual(a: BigDecimal, b: BigDecimal): Boolean = (a - b) < threshold && (b - a) < threshold

  val threshold = 0.000001

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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth)),
                maybePaymentToday = Some(PaymentToday(true))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

              val taxPaymentPlan = TaxPaymentPlan(
                liabilities,
                upfrontPaymentAmount,
                fixedToday,
                regularPaymentAmount,
                Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            regularPaymentAmount,
            Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            regularPaymentAmount,
            Some(ArrangementDayOfMonth(regularPaymentsDayWithinFirstMonth))
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

          val taxPaymentPlan = TaxPaymentPlan(
            liabilities,
            upfrontPaymentAmount,
            fixedToday,
            regularPaymentAmount,
            Some(ArrangementDayOfMonth(preferredPaymentDay))
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

        val taxPaymentPlan = TaxPaymentPlan(
          liabilities,
          upfrontPaymentAmount,
          fixedToday,
          regularPaymentAmount,
          Some(ArrangementDayOfMonth(preferredPaymentDay)),
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

        val taxPaymentPlan = TaxPaymentPlan(
          liabilities,
          upfrontPaymentAmount,
          fixedToday,
          regularPaymentAmount,
          Some(ArrangementDayOfMonth(preferredPaymentDay))
        )

        val result = calculatorService.schedule(taxPaymentPlan)

        result shouldBe None
      }
    }

    "paymentPlanOptions generates based on remaining income after spending up to three schedules in a map" - {
      "three (50%, 60% and 80% of remaining income), if neither first two cover total to pay in a single month" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 5000, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

        println(result)

        result.size shouldBe 3
      }
      "with correct keys ('50', '60' and '80')" in {
        val sa = SelfAssessmentDetails(
          SaUtr("saUtr"),
          CommunicationPreferences(false, false, false, false),
          Seq(Debit("originCode", 5000, date("2023-07-31"), None, date("2022-04-05"))),
          Seq()
        )

        val initialPayment = 0
        val preferredPaymentDay = None
        val remainingIncomeAfterSpending = 1000

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

        println(result)

        result(50)
          .instalments.head.amount shouldBe remainingIncomeAfterSpending * 0.5
        result(60)
          .instalments.head.amount shouldBe remainingIncomeAfterSpending * 0.6
        result(80)
          .instalments.head.amount shouldBe remainingIncomeAfterSpending * 0.8
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

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

        result(50).instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.5)
        result(60).instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.6)
        result(80).instalments.init.foreach(instalment => instalment.amount shouldBe remainingIncomeAfterSpending * 0.8)
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

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

        result.size shouldBe 1
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

        val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

        result.size shouldBe 2
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

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

          result.size shouldBe 1
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

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

          result.size shouldBe 2
        }
      }
      "if a plan at 50% of remaining income would be more than 24 months, return no plans at all" - {
        "for example £3000 to pay, with £200 left over income" in {
          val sa = SelfAssessmentDetails(
            SaUtr("saUtr"),
            CommunicationPreferences(false, false, false, false),
            Seq(
              Debit("originCode", 1000, date("2020-01-31"), None, date("2019-04-05")),
              Debit("originCode", 1000, date("2020-01-31"), None, date("2020-04-01")),
              Debit("originCode", 1000, date("2020-07-31"), None, date("2020-04-05"))
            ),
            Seq()
          )

          val initialPayment = 0
          val preferredPaymentDay = Some(ArrangementDayOfMonth(28))
          val remainingIncomeAfterSpending = 200

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

          result.size shouldBe 0
        }
        "for example £3000 to pay, with £220 left over income" in {
          val sa = SelfAssessmentDetails(
            SaUtr("saUtr"),
            CommunicationPreferences(false, false, false, false),
            Seq(
              Debit("originCode", 1000, date("2020-01-31"), None, date("2019-04-05")),
              Debit("originCode", 1000, date("2020-01-31"), None, date("2020-04-01")),
              Debit("originCode", 1000, date("2020-07-31"), None, date("2020-04-05"))
            ),
            Seq()
          )

          val initialPayment = 0
          val preferredPaymentDay = Some(ArrangementDayOfMonth(28))
          val remainingIncomeAfterSpending = 220

          val result = calculatorService.scheduleOptions(sa, initialPayment, preferredPaymentDay, remainingIncomeAfterSpending)(fixedClock, appConfig)

          result.size shouldBe 0

        }
      }

    }
  }
}
