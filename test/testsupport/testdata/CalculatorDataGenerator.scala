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

package testsupport.testdata

import java.time.LocalDate
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json.{prettyPrint, stringify, toJson}
import ssttpcalculator.model.{Instalment, InterestRate, Payables, PaymentSchedule, PaymentsCalendar, TaxLiability, TaxPaymentPlan}
import testsupport.DateSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

object CalculatorDataGenerator extends Status with DateSupport {
  val eightMonthScheduleRegularPaymentAmount = 637
  val sevenMonthScheduleRegularPaymentAmount = 700
  val sixMonthScheduleRegularPaymentAmount = 816.67
  val fiveMonthScheduleRegularPaymentAmount = 980
  val fourMonthScheduleRegularPaymentAmount = 1225
  val threeMonthScheduleRegularPaymentAmount = 1633.33
  val twoMonthScheduleRegularPaymentAmount = 2450

  private val thisYear = _2019
  private val nextYear = _2020

  val startDate: LocalDate = LocalDate.of(thisYear, november, _25th)

  private val debit1Value = 2500
  private val debit2Value = 2400
  private val totalDebt = debit1Value + debit2Value
  private val initialPayment = 0
  private val totalInterest = 200

  private val LastPaymentDelayDays = 7

  def generateSchedules(paymentDayOfMonth: Int = _25th, firstPaymentDayOfMonth: Int = _2nd): StubMapping = {
      def paymentDate(month: Int) = LocalDate.of(nextYear, month, paymentDayOfMonth)

    val firstPaymentDate = LocalDate.of(thisYear, december, paymentDayOfMonth)
    val januaryPaymentDate = paymentDate(january)
    val februaryPaymentDate = paymentDate(february)
    val marchPaymentDate = paymentDate(march)
    val aprilPaymentDate = paymentDate(april)
    val mayPaymentDate = paymentDate(may)
    val junePaymentDate = paymentDate(june)
    val julyPaymentDate = paymentDate(july)

    generateSchedule(
      paymentSchedule(List(firstPaymentDate), twoMonthScheduleRegularPaymentAmount, finalInstalment = januaryPaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate), threeMonthScheduleRegularPaymentAmount, finalInstalment = februaryPaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate, februaryPaymentDate),
        fourMonthScheduleRegularPaymentAmount,
        finalInstalment = marchPaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate, februaryPaymentDate, marchPaymentDate),
        fiveMonthScheduleRegularPaymentAmount,
        finalInstalment = aprilPaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate, februaryPaymentDate, marchPaymentDate, aprilPaymentDate),
        sixMonthScheduleRegularPaymentAmount,
        finalInstalment = mayPaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate, februaryPaymentDate, marchPaymentDate, aprilPaymentDate, mayPaymentDate),
        sevenMonthScheduleRegularPaymentAmount,
        finalInstalment = junePaymentDate),
      firstPaymentDayOfMonth)
    generateSchedule(
      paymentSchedule(
        List(firstPaymentDate, januaryPaymentDate, februaryPaymentDate, marchPaymentDate, aprilPaymentDate, mayPaymentDate, junePaymentDate),
        eightMonthScheduleRegularPaymentAmount,
        finalInstalment = julyPaymentDate),
      firstPaymentDayOfMonth)
  }

  private def generateSchedule(schedule: PaymentSchedule, firstPaymentDayOfMonth: Int) =
    stubFor(
      post(urlPathEqualTo("/time-to-pay-calculator/paymentschedule"))
        .withRequestBody(equalToJson(stringify(toJson(calculatorInput(schedule.endDate, firstPaymentDayOfMonth)))))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(prettyPrint(toJson(schedule))))
    )

  def calculatorInput(endDate: LocalDate, firstPaymentDayOfMonth: Int): TaxPaymentPlan =
    TaxPaymentPlan(
      Seq(TaxLiability(debit1Value, startDate), TaxLiability(debit2Value, startDate)),
      initialPayment,
      startDate,
      endDate,
      Some(LocalDate.of(thisYear, december, firstPaymentDayOfMonth)))

  private def paymentSchedule(regularInstalments: List[LocalDate], regularPayment: BigDecimal, finalInstalment: LocalDate) = {
    val finalPayment = totalDebt - (regularPayment * regularInstalments.size)
    val regularInstallments = regularInstalments.map(paymentDate => Instalment(paymentDate, regularPayment, 0))
    val finalInstallment = Instalment(finalInstalment, finalPayment + totalInterest, totalInterest)

    new PaymentSchedule(
      startDate            = startDate,
      endDate              = finalInstallment.paymentDate.plusDays(LastPaymentDelayDays),
      initialPayment       = initialPayment,
      amountToPay          = totalDebt,
      instalmentBalance    = totalDebt,
      totalInterestCharged = totalInterest,
      totalPayable         = totalDebt + totalInterest,
      instalments          = regularInstallments :+ finalInstallment)
  }

  object newCalculatorModel {


    def date(date: String): LocalDate = LocalDate.parse(date)



    val aPaymentOnAccountNoInterestPayable: TaxLiability = TaxLiability(amount = 1000, dueDate = date("2100-01-01"))
    val anotherPaymentOnAccountNoInterestPayable: TaxLiability = TaxLiability(amount = 2000, dueDate = date("2100-01-01"))
    val aDebtWithInterestPayable: TaxLiability = TaxLiability(amount = 1000, dueDate = date("2022-03-17"))

    val payablesWithOne2000LiabilityNoDueDate: Payables = Payables(liabilities = Seq(anotherPaymentOnAccountNoInterestPayable))
    val payablesWithTwoLiabilitiesNoDueDate: Payables = Payables(liabilities = Seq(aPaymentOnAccountNoInterestPayable, anotherPaymentOnAccountNoInterestPayable))
    val payablesWithOneDebt: Payables = Payables(liabilities = Seq(aDebtWithInterestPayable))

    def fixedZeroInterest(d: LocalDate = date("2023-02-01")): InterestRate = {
      InterestRate(
        startDate = date("1900-01-01"), endDate = date("2100-12-31"), rate = 0
      )
    }

    def fixedInterestRate(rate: BigDecimal = 1): LocalDate => InterestRate = (_: LocalDate) => InterestRate(
      startDate = date("1900-01-01"),
      endDate = date("2100-12-31"),
      rate = rate
    )

    def fixedInterestRates(rate: BigDecimal = 1): (LocalDate, LocalDate) => Seq[InterestRate] = {
      (startDate: LocalDate, endDate: LocalDate) => {
        Seq(InterestRate(
          startDate = startDate,
          endDate = endDate,
          rate = rate
        ))
      }
    }

    def testPayables(liabilities: (BigDecimal, LocalDate)*): Payables = {
      Payables(liabilities.map(liability => TaxLiability(liability._1, liability._2)))
    }
  }
}
