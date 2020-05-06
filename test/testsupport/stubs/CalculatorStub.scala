/*
 * Copyright 2020 HM Revenue & Customs
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

package testsupport.stubs

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json.{prettyPrint, stringify, toJson}
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}

object CalculatorStub extends Status {
  val eightMonthScheduleRegularPaymentAmount = 637
  val sevenMonthScheduleRegularPaymentAmount = 700
  val sixMonthScheduleRegularPaymentAmount = 816
  val fiveMonthScheduleRegularPaymentAmount = 980
  val fourMonthScheduleRegularPaymentAmount = 1225
  val threeMonthScheduleRegularPaymentAmount = 1633
  val twoMonthScheduleRegularPaymentAmount = 2450

  private val thisYear = 2019
  private val nextYear = 2020

  private val november = 11
  private val december = 12
  private val january = 1
  private val february = 2
  private val march = 3
  private val april = 4
  private val may = 5
  private val june = 6
  private val july = 7

  val startDate: LocalDate = LocalDate.of(thisYear, november, 25)

  private val debit1Value = 2500
  private val debit2Value = 2400
  private val totalDebt = debit1Value + debit2Value
  private val initialPayment = 0
  private val totalInterest = 200

  def generateSchedules(paymentDayOfMonth: Int = 25, firstPaymentDayOfMonth: Int = 2): StubMapping = {
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

  def calculatorInput(endDate: LocalDate, firstPaymentDayOfMonth: Int): CalculatorInput =
    CalculatorInput(
      Seq(DebitInput(debit1Value, startDate), DebitInput(debit2Value, startDate)),
      initialPayment,
      startDate,
      endDate,
      Some(LocalDate.of(thisYear, december, firstPaymentDayOfMonth)))

  private def paymentSchedule(regularInstalments: List[LocalDate], regularPayment: Int, finalInstalment: LocalDate) = {
    val finalPayment = totalDebt - (regularPayment * regularInstalments.size)
    val regularInstallments = regularInstalments.map(paymentDate => Instalment(paymentDate, regularPayment, 0))
    val finalInstallment = Instalment(finalInstalment, finalPayment + totalInterest, totalInterest)

    new PaymentSchedule(
      startDate            = startDate,
      endDate              = finalInstallment.paymentDate,
      initialPayment       = initialPayment,
      amountToPay          = totalDebt,
      instalmentBalance    = totalDebt,
      totalInterestCharged = totalInterest,
      totalPayable         = totalDebt + totalInterest,
      instalments          = regularInstallments :+ finalInstallment)
  }
}
