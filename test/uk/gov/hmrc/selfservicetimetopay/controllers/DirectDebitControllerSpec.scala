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
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors.{DirectDebitConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future
import scala.util.Try


class DirectDebitControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with ScalaFutures {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val mockDDConnector = mock[DirectDebitConnector]
  val mockSessionCache = mock[SessionCacheConnector]
  val mockAuthConnector = mock[AuthConnector]

  "DirectDebitController" should {
    val controller = new DirectDebitController(mockDDConnector) {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
      override lazy val authConnector: AuthConnector = mockAuthConnector
    }

    "successfully display the direct debit form page" in {
      val response = await(controller.getDirectDebit(FakeRequest().withSession(sessionProvider.createSessionId())))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.arrangement.direct-debit.form.title"))
    }

    "submit direct debit form with valid form data and valid bank details and redirect to direct debit confirmation page" in {
      when(mockDDConnector.validateOrRetrieveAccounts(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(bankDetails)))

      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val request = FakeRequest().withSession(sessionProvider.createSessionId()).withFormUrlEncodedBody(validDirectDebitForm: _*)

      val response = await(controller.submitDirectDebit(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.DirectDebitController.getDirectDebitConfirmation().url
    }

    "submit direct debit form with valid form data and invalid bank details and redirect to invalid bank details page" in {
      when(mockDDConnector.validateOrRetrieveAccounts(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(directDebitBank)))

      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val request = FakeRequest().withSession(sessionProvider.createSessionId()).withFormUrlEncodedBody(invalidBankDetailsForm: _*)

      val response = await(controller.submitDirectDebit(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.DirectDebitController.getBankAccountNotFound().url
    }

    "submit direct debit form with invalid form data and return a bad request" in {
      val request = FakeRequest().withSession(sessionProvider.createSessionId()).withFormUrlEncodedBody(inValidDirectDebitForm: _*)

      val response = await(controller.submitDirectDebit(request))

      status(response) shouldBe BAD_REQUEST
    }

    "submit direct debit form with an authorised user without sa enrolment and throw an exception" in {
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUserNoSA)))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
    }

    "submit direct debit form with an unauthorised user and throw an exception" in {
      when(mockAuthConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(None))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
    }

    "successfully display the direct debit confirmation page" in {
      val response = await(controller.getDirectDebitConfirmation(FakeRequest().withSession(sessionProvider.createSessionId())))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.arrangement.direct-debit.confirmation.title"))
    }
  }

  private def shouldNotSucceed: PartialFunction[Result, Unit] = {
    case _ => fail("Method call should not have succeeded"); Unit
  }

  private def expectingRuntimeException: PartialFunction[Throwable, Unit] = {
    case e: RuntimeException => Unit
    case e => fail(s"Wrong exception type was thrown: ${e.getClass.getSimpleName}"); Unit
  }
}
