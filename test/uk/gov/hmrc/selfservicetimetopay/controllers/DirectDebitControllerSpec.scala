/*
 * Copyright 2017 HM Revenue & Customs
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
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.connectors.{DirectDebitConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models.DirectDebitBank
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future
import scala.util.Try

class DirectDebitControllerSpec extends PlayMessagesSpec with MockitoSugar {

  private val gaToken = "GA-TOKEN"

  val mockDDConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  "DirectDebitController" should {
    val controller = new DirectDebitController(messagesApi, mockDDConnector) {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
      override lazy val authConnector: AuthConnector = mockAuthConnector
      override lazy val authenticationProvider: GovernmentGateway = mockAuthenticationProvider
    }

    "successfully display the direct debit form page" in {running(app) {
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData =
        ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits)))))

      val request = FakeRequest()
        .withSession(SessionKeys.userId -> "someUserId")
        .withCookies(sessionProvider.createTtpCookie())
      implicit val messages = getMessages(request)
      val response = controller.getDirectDebit(request)

      status(response) mustBe OK

      contentAsString(response) must include(Messages("ssttp.arrangement.direct-debit.form.title"))
    }}

    "submit direct debit form with valid form data and valid bank details and redirect to direct debit confirmation page" in { running(app) {

      when(mockDDConnector.getBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData =
        ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits)))))

      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId")
        .withFormUrlEncodedBody(validDirectDebitForm: _*)
      implicit val messages = getMessages(request)
      val response = controller.submitDirectDebit(request)
      status(response) mustBe BAD_REQUEST

    }}

    "submit direct debit form with valid form data and invalid bank details it should be a bad request" in { running(app) {

      when(mockDDConnector.getBank(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId")
        .withFormUrlEncodedBody(invalidBankDetailsForm: _*)
      implicit val messages = getMessages(request)
      val response = controller.submitDirectDebit(request)

      status(response) mustBe BAD_REQUEST

      contentAsString(response) must include (Messages("ssttp.direct-debit.form.bank-not-found-info"))
    }}

    "submit direct debit form with invalid form data and return a bad request" in { running(app) { 
      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId")
        .withFormUrlEncodedBody(inValidDirectDebitForm: _*)
      implicit val messages = getMessages(request)
      val response = controller.submitDirectDebit(request)

      status(response) mustBe BAD_REQUEST
    }}

    "submit direct debit form with an authorised user without sa enrolment and throw an exception" in { running(app) {

      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUserNoSA)))

      val request = FakeRequest()
        .withSession(SessionKeys.userId -> "someUserId")
        .withFormUrlEncodedBody(validDirectDebitForm: _*)
      implicit val messages = getMessages(request)
      Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
    }}

    "submit direct debit form with an unauthorised user and throw an exception" in { running(app) {

      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(None))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
    }}

    "successfully display the direct debit confirmation page" in { running(app) {

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      val request = FakeRequest()
        .withSession(SessionKeys.userId -> "someUserId")
        .withCookies(sessionProvider.createTtpCookie())
      implicit val messages = getMessages(request)
      val response = controller.getDirectDebitConfirmation(request)

      status(response) mustBe OK

      contentAsString(response) must include(getMessages(request)("ssttp.arrangement.direct-debit.confirmation.title"))
    }}
  }

  private def shouldNotSucceed: PartialFunction[Result, Unit] = {
    case _ => fail("Method call should not have succeeded"); Unit
  }

  private def expectingRuntimeException: PartialFunction[Throwable, Unit] = {
    case e: RuntimeException => Unit
    case e => fail(s"Wrong exception type was thrown: ${e.getClass.getSimpleName}"); Unit
  }
}
