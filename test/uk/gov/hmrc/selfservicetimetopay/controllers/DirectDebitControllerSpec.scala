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

  val ddConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val sessionCacheConnector: SessionCacheConnector = mock[SessionCacheConnector]
  val authConnector: AuthConnector = mock[AuthConnector]

  "DirectDebitController" should {
    val controller = new DirectDebitController(ddConnector, sessionCacheConnector, authConnector)

    "successfully display the direct debit form page" in {
      val response: Result = controller.getDirectDebit.apply(FakeRequest())

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.arrangement.direct-debit.form.title"))
    }

    "submit direct debit form with valid form data and valid bank details and redirect to direct debit confirmation page" in {
      when(ddConnector.validateOrRetrieveAccounts(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Left(bankDetails)))

      when(authConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      val response: Future[Result] = await(controller.submitDirectDebit.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.DirectDebitController.getDirectDebitConfirmation().url
    }

    "submit direct debit form with valid form data and invalid bank details and redirect to invalid bank details page" in {
      when(ddConnector.validateOrRetrieveAccounts(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Right(directDebitBank)))

      when(authConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      val request = FakeRequest().withFormUrlEncodedBody(invalidBankDetailsForm: _*)

      val response: Future[Result] = await(controller.submitDirectDebit.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.DirectDebitController.getDirectDebit().url
    }

    "submit direct debit form with invalid form data and return a bad request" in {
      val request = FakeRequest().withFormUrlEncodedBody(inValidDirectDebitForm: _*)

      val response: Future[Result] = await(controller.submitDirectDebit.apply(request))

      status(response) shouldBe BAD_REQUEST
    }

    "submit direct debit form with an authorised user without sa enrolment and throw an exception" in {
      when(authConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(Some(authorisedUserNoSA)))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      Try(await(controller.submitDirectDebit.apply(request))).map {
        case _ => fail()
      }.recover {
        case e: RuntimeException =>
        case _ => fail()
      }
    }

    "submit direct debit form with an unauthorised user and throw an exception" in {
      when(authConnector.currentAuthority(Matchers.any())).thenReturn(Future.successful(None))

      val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

      Try(await(controller.submitDirectDebit.apply(request))).map {
        case _ => fail()
      }.recover {
        case e: RuntimeException =>
        case _ => fail()
      }
    }

    "successfully display the direct debit confirmation page" in {
      val response: Result = controller.getDirectDebitConfirmation.apply(FakeRequest())

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.arrangement.direct-debit.confirmation.title"))
    }
  }
}
