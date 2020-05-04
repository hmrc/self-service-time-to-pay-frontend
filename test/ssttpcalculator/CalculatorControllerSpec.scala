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

package ssttpcalculator

import java.time.temporal.ChronoUnit.MONTHS

import model._
import play.api.test.FakeRequest
import testsupport.ItSpec
import testsupport.stubs.CalculatorStub.{calculatorInput, generateSchedules, startDate}
import timetopaycalculator.cor.model.PaymentSchedule
import uk.gov.hmrc.http.HeaderCarrier

class CalculatorControllerSpec extends ItSpec {
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val twoMonths = 2
  private val threeMonths = 3
  private val fourMonths = 4
  private val fiveMonths = 5
  private val sixMonths = 6
  private val sevenMonths = 7

  private lazy val connector = app.injector.instanceOf[CalculatorConnector]
  private lazy val controller = app.injector.instanceOf[CalculatorController]

  "closestSchedule returns the schedule with monthly payments nearest to the user's preferred amount" in {
    generateSchedules()

    val paymentSchedules: List[PaymentSchedule] = Range(twoMonths, sevenMonths).inclusive.map { duration =>
      connector.calculatePaymentSchedule(calculatorInput(
        startDate.plus(duration, MONTHS), 2))(FakeRequest()).futureValue
    }.toList

    confirm(whenUserPrefersMonthlyPayment(1), closestActualPayment = 700, duration = sevenMonths)
    confirm(whenUserPrefersMonthlyPayment(757), closestActualPayment = 700, duration = sevenMonths)
    confirm(whenUserPrefersMonthlyPayment(758), closestActualPayment = 816, duration = sixMonths)
    confirm(whenUserPrefersMonthlyPayment(897), closestActualPayment = 816, duration = sixMonths)
    confirm(whenUserPrefersMonthlyPayment(898), closestActualPayment = 980, duration = fiveMonths)
    confirm(whenUserPrefersMonthlyPayment(1102), closestActualPayment = 980, duration = fiveMonths)
    confirm(whenUserPrefersMonthlyPayment(1103), closestActualPayment = 1225, duration = fourMonths)
    confirm(whenUserPrefersMonthlyPayment(1428), closestActualPayment = 1225, duration = fourMonths)
    confirm(whenUserPrefersMonthlyPayment(1429), closestActualPayment = 1633, duration = threeMonths)
    confirm(whenUserPrefersMonthlyPayment(2041), closestActualPayment = 1633, duration = threeMonths)
    confirm(whenUserPrefersMonthlyPayment(2042), closestActualPayment = 2450, duration = twoMonths)

      def confirm(schedule: PaymentSchedule, closestActualPayment: BigDecimal, duration: Int) = {
        schedule.firstInstallment.amount shouldBe closestActualPayment
        schedule.durationInMonths shouldBe duration
      }

      def whenUserPrefersMonthlyPayment(amount: Int) = controller.closestSchedule(amount, paymentSchedules)
  }
}
