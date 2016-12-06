/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.controllers

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllers.calculator.CalculateInstalmentsController
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class CalculateInstalmentsControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication with BeforeAndAfterEach {

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]

  val controller = new CalculateInstalmentsController(mockEligibilityConnector, mockCalculatorConnector) {
    override lazy val sessionCache = mockSessionCache
    override lazy val authConnector = mockAuthConnector
  }

  override def beforeEach() {
    reset(mockEligibilityConnector, mockAuthConnector, mockSessionCache, mockCalculatorConnector)
  }

  "CalculateInstalmentsControllerSpec" should {
    "Return OK for non-logged-in calculation submission" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      when(mockEligibilityConnector.checkEligibility(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(eligibilityStatusOk))

      when(mockCalculatorConnector.calculatePaymentSchedule(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Some(Seq(calculatorPaymentSchedule))))

      val result = await(controller.getCalculateInstalments(None).apply(FakeRequest().withSession(sessionProvider.createSessionId())))

      status(result) shouldBe Status.OK
      verify(mockEligibilityConnector, times(1)).checkEligibility(Matchers.any())(Matchers.any())
      verify(mockCalculatorConnector, times(1)).calculatePaymentSchedule(Matchers.any())(Matchers.any())
    }

    "Return Ineligible Response for non-logged-in when amounts > £10,000" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))

      when(mockEligibilityConnector.checkEligibility(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(eligibilityStatusDebtTooHigh))

      val result = await(controller.getCalculateInstalments(None).apply(FakeRequest().withSession(sessionProvider.createSessionId())))

      status(result) shouldBe Status.SEE_OTHER
      verify(mockEligibilityConnector, times(1)).checkEligibility(Matchers.any())(Matchers.any())
      verify(mockCalculatorConnector, times(0)).calculatePaymentSchedule(Matchers.any())(Matchers.any())
    }
  }
}
