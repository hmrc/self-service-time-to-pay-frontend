/*
 * Copyright 2021 HM Revenue & Customs
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

import _root_.model._
import play.api.test.FakeRequest
import ssttpcalculator.model.PaymentSchedule
import testsupport.ItSpec
import testsupport.testdata.CalculatorDataGenerator._
import testsupport.testdata.TdAll.saUtr
import timetopaytaxpayer.cor.model.{CommunicationPreferences, Return, SelfAssessmentDetails}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.Month.APRIL
import java.time.temporal.ChronoUnit.MONTHS

class CalculatorControllerSpec extends ItSpec {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val twoMonths = 2
  private val threeMonths = 3
  private val fourMonths = 4
  private val fiveMonths = 5
  private val sixMonths = 6
  private val sevenMonths = 7

  override def beforeEach(): Unit = {
    super.beforeEach()
    generateSchedules()
    ()
  }

  trait SetUp {
    lazy val controller: CalculatorController = app.injector.instanceOf[CalculatorController]
    lazy val service: CalculatorService = app.injector.instanceOf[CalculatorService]

    lazy val paymentSchedules: List[PaymentSchedule] = Range(twoMonths, sevenMonths).inclusive.map { duration =>
      service.buildSchedule(calculatorInput(
        startDate.plus(duration, MONTHS), 2))
    }.toList
  }

  "closestSchedule returns the schedule with monthly payments nearest to the user's preferred amount" in new SetUp {
    confirm(whenUserPrefersMonthlyPayment(1), closestActualPayment = sevenMonthScheduleRegularPaymentAmount, duration = sevenMonths)
    confirm(whenUserPrefersMonthlyPayment(757), closestActualPayment = sevenMonthScheduleRegularPaymentAmount, duration = sevenMonths)
    confirm(whenUserPrefersMonthlyPayment(758), closestActualPayment = sixMonthScheduleRegularPaymentAmount, duration = sixMonths)
    confirm(whenUserPrefersMonthlyPayment(897), closestActualPayment = sixMonthScheduleRegularPaymentAmount, duration = sixMonths)
    confirm(whenUserPrefersMonthlyPayment(898), closestActualPayment = fiveMonthScheduleRegularPaymentAmount, duration = fiveMonths)
    confirm(whenUserPrefersMonthlyPayment(1102), closestActualPayment = fiveMonthScheduleRegularPaymentAmount, duration = fiveMonths)
    confirm(whenUserPrefersMonthlyPayment(1103), closestActualPayment = fourMonthScheduleRegularPaymentAmount, duration = fourMonths)
    confirm(whenUserPrefersMonthlyPayment(1428), closestActualPayment = fourMonthScheduleRegularPaymentAmount, duration = fourMonths)
    confirm(whenUserPrefersMonthlyPayment(1429), closestActualPayment = threeMonthScheduleRegularPaymentAmount, duration = threeMonths)
    confirm(whenUserPrefersMonthlyPayment(2041), closestActualPayment = threeMonthScheduleRegularPaymentAmount, duration = threeMonths)
    confirm(whenUserPrefersMonthlyPayment(2042), closestActualPayment = twoMonthScheduleRegularPaymentAmount, duration = twoMonths)

    private def confirm(schedule: PaymentSchedule, closestActualPayment: BigDecimal, duration: Int) = {
      schedule.firstInstallment.amount shouldBe closestActualPayment
      schedule.durationInMonths shouldBe duration
    }

    private def whenUserPrefersMonthlyPayment(amount: Int) = controller.computeClosestSchedule(amount, paymentSchedules)
  }

  "closestSchedules returns the closest schedule with the next closest 2 schedules if present" in new SetUp {
    private val taxReturnDate = LocalDate.of(2020, APRIL, 5)
    private val testReturns = List(Return(taxReturnDate, None, Some(taxReturnDate), None))
    private val communicationPreferences = CommunicationPreferences(
      welshLanguageIndicator = true, audioIndicator = true, largePrintIndicator = true, brailleIndicator = true)
    private val selfAssessmentDetails = SelfAssessmentDetails(saUtr, communicationPreferences, Nil, testReturns)

    private val twoMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == twoMonthScheduleRegularPaymentAmount).head
    private val threeMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == threeMonthScheduleRegularPaymentAmount).head
    private val fourMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == fourMonthScheduleRegularPaymentAmount).head
    private val fiveMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == fiveMonthScheduleRegularPaymentAmount).head
    private val sixMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == sixMonthScheduleRegularPaymentAmount).head
    private val sevenMonthSchedule = paymentSchedules.find(_.firstInstallment.amount == sevenMonthScheduleRegularPaymentAmount).head

    private val closestSchedulesToTwoMonthSchedule =
      controller.computeClosestSchedules(twoMonthSchedule, paymentSchedules, selfAssessmentDetails)(FakeRequest()).toSet
    closestSchedulesToTwoMonthSchedule shouldBe Set(twoMonthSchedule, threeMonthSchedule, fourMonthSchedule)

    private val closestSchedulesToSixMonthSchedule =
      controller.computeClosestSchedules(sixMonthSchedule, paymentSchedules, selfAssessmentDetails)(FakeRequest()).toSet
    closestSchedulesToSixMonthSchedule shouldBe Set(fiveMonthSchedule, sixMonthSchedule, sevenMonthSchedule)

    private val closestSchedulesToSevenMonthSchedule =
      controller.computeClosestSchedules(sevenMonthSchedule, paymentSchedules, selfAssessmentDetails)(FakeRequest()).toSet
    closestSchedulesToSevenMonthSchedule shouldBe Set(fiveMonthSchedule, sixMonthSchedule, sevenMonthSchedule)
  }
}
