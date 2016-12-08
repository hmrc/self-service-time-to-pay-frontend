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
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class CalculatorControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication with BeforeAndAfterEach {

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]

  val controller = new CalculatorController(mockEligibilityConnector, mockCalculatorConnector) {
    override lazy val sessionCache = mockSessionCache
    override lazy val authConnector = mockAuthConnector
  }

  override def beforeEach() {
    reset(mockEligibilityConnector, mockAuthConnector, mockSessionCache, mockCalculatorConnector)
  }

  "CalculatorControllerSpec" should {
    "Return OK for non-logged-in calculation submission" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.OK
      verify(mockSessionCache, times(1)).get(Matchers.any(), Matchers.any())
    }

    "Return 303 for non-logged-in when schedule is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLINoSchedule)))

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.NOT_FOUND
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
    }


    "Return 303 for non-logged-in when form data is invalid" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))

      val result = await(controller.submitCalculateInstalmentsPaymentToday().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
    }

    "Return Redirect for non-logged-in when debts is > £10,000" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))

      when(mockCalculatorConnector.calculatePaymentSchedule(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(Seq(ttpArrangement.schedule)))

      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map.empty)))

      val result = await(controller.submitCalculateInstalmentsPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "200")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getCalculateInstalments(None).url
      verify(mockSessionCache, times(1)).get(Matchers.any(), Matchers.any())
      verify(mockSessionCache, times(1)).put(Matchers.any())(Matchers.any(), Matchers.any())
      verify(mockCalculatorConnector, times(1)).calculatePaymentSchedule(Matchers.any())(Matchers.any())
    }
  }
}
