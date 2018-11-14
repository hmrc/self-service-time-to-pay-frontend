/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.service

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.selfservicetimetopay.connectors.CalculatorConnector
import uk.gov.hmrc.selfservicetimetopay.resources.{selfAssessment, _}

import scala.concurrent.ExecutionContext.Implicits.global
class CalculatorServiceSpec extends UnitSpec  with MockitoSugar  with ScalaFutures {


  val mockCalConnector = mock[CalculatorConnector]
  val mockWorkingDaysService = mock[WorkingDaysService]
  val calculatorService = new CalculatorService(mockCalConnector,mockWorkingDaysService)
  implicit val hc = HeaderCarrier()
  "CalculatorService" should {
    "call the calculator connector a 10 times in" in {
      when(mockCalConnector.calculatePaymentSchedule(any())(any())).thenReturn(eventualSchedules)
      when(mockWorkingDaysService.addWorkingDays(any(),any())).thenReturn(LocalDate.now())
      calculatorService.getInstalmentsSchedule(selfAssessment.get)
      verify(mockCalConnector, times(10)).calculatePaymentSchedule(any())(any())
    }

    "return the map sorted from lowest to highest" in {
      when(mockCalConnector.calculatePaymentSchedule(any())(any())).thenReturn(eventualSchedules)
      when(mockWorkingDaysService.addWorkingDays(any(),any())).thenReturn(LocalDate.now())
      val result =  calculatorService.getInstalmentsSchedule(selfAssessment.get).futureValue
      result shouldBe Map(2 -> calculatorPaymentSchedule,
        3 -> calculatorPaymentSchedule,
        4 -> calculatorPaymentSchedule,
        5 -> calculatorPaymentSchedule,
        6 -> calculatorPaymentSchedule,
        7 -> calculatorPaymentSchedule,
        8 -> calculatorPaymentSchedule,
        9 -> calculatorPaymentSchedule,
        10 -> calculatorPaymentSchedule,
        11 -> calculatorPaymentSchedule)
    }
  }
}