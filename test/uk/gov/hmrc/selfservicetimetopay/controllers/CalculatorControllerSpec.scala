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

import java.time.LocalDate

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.models.{Debit, EligibilityExistingTTP, EligibilityTypeOfTax}
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

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.OK
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now())))
      )

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "300.00")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now())))
      )

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.999")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 if the form value < total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now())))
      )

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.994")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 for non-logged-in when schedule is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLINoSchedule)))

      val result = await(controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.NOT_FOUND
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(controller.submitPaymentToday().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
    }

  }
}
