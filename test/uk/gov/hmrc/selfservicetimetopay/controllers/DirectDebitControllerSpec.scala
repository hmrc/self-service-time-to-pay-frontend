/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppConfig
import controllers.action.Actions
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import ssttpdirectdebit.{DirectDebitConnector, DirectDebitController}
import journey.JourneyService
import token.TokenService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.resources._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class DirectDebitControllerSpec extends PlayMessagesSpec with MockitoSugar {

  val mockDDConnector: DirectDebitConnector = mock[DirectDebitConnector]

  val mockSessionCache: JourneyService = mock[JourneyService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockSessionCache4TokensConnector: TokenService = mock[TokenService]
  val mcc: MessagesControllerComponents = mock

  implicit val appConfig: AppConfig = mock[AppConfig]

  "DirectDebitController" should {
    val controller = new DirectDebitController(
      mcc                  = mcc,
      directDebitConnector = mockDDConnector,
      as                   = mock[Actions],
      submissionService    = mockSessionCache,
      ???,
      ???
    )

    "successfully display the direct debit form page" in {
      running(app) {

        when(mockSessionCache.getJourney(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData =
          ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits)))))
        //        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
        when(mockSessionCache4TokensConnector.put(any())(any())).thenReturn(Future.successful(()))

        val request = FakeRequest()
          .withSession(goodSession: _*)
        implicit val messages = getMessages(request)
        val response = controller.getDirectDebit(request)

        status(response) mustBe OK

        contentAsString(response) must include(Messages("ssttp.arrangement.direct-debit.form.title"))
      }
    }

    "submit direct debit form with valid form data and valid bank details and redirect to direct debit confirmation page" in {
      running(app) {

        when(mockDDConnector.getBank(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        //        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
        when(mockSessionCache.getJourney(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData =
          ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits)))))

        val request = FakeRequest()
          .withSession(goodSession: _*)
          .withFormUrlEncodedBody(validDirectDebitForm: _*)
        implicit val messages = getMessages(request)
        val response = controller.submitDirectDebit(request)
        status(response) mustBe BAD_REQUEST

      }
    }

    "submit direct debit form with valid form data and invalid bank details it should be a bad request" in {
      running(app) {

        when(mockDDConnector.getBank(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

        when(mockSessionCache.getJourney(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
        when(mockSessionCache.saveJourney(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

        val request = FakeRequest()
          .withSession(goodSession: _*)
          .withFormUrlEncodedBody(invalidBankDetailsForm: _*)
        implicit val messages = getMessages(request)
        val response = controller.submitDirectDebit(request)

        status(response) mustBe BAD_REQUEST

      }
    }

    "submit direct debit form with an invalid number it should display a single error" in {
      running(app) {

        when(mockDDConnector.getBank(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

        when(mockSessionCache.getJourney(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
        when(mockSessionCache.saveJourney(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

        val request = FakeRequest()
          .withSession(goodSession: _*)
          .withFormUrlEncodedBody(Seq(
            "accountName" -> "Jane Doe",
            "sortCode" -> "af",
            "accountNumber" -> "12345678",
            "singleAccountHolder" -> "true"
          ): _*)
        implicit val messages = getMessages(request)
        val response = controller.submitDirectDebit(request)

        status(response) mustBe BAD_REQUEST
        contentAsString(response) must not include Messages("ssttp.direct-debit.form.error.sortCode.required")
        contentAsString(response) must include (Messages("ssttp.direct-debit.form.error.sortCode.not-valid"))
      }
    }

    "submit direct debit form with invalid form data and return a bad request" in {
      running(app) {
        val request = FakeRequest()
          .withSession(goodSession: _*)
          .withFormUrlEncodedBody(inValidDirectDebitForm: _*)
        implicit val messages = getMessages(request)
        val response = controller.submitDirectDebit(request)

        status(response) mustBe BAD_REQUEST
      }
    }

    "submit direct debit form with an authorised user without sa enrolment and throw an exception" in {
      running(app) {

        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUserNoSA)))

        val request = FakeRequest()
          .withSession(goodSession: _*)
          .withFormUrlEncodedBody(validDirectDebitForm: _*)
        implicit val messages = getMessages(request)
        Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
      }
    }

    "submit direct debit form with an unauthorised user and throw an exception" in {
      running(app) {

        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        val request = FakeRequest().withFormUrlEncodedBody(validDirectDebitForm: _*)

        Try(await(controller.submitDirectDebit(request))).map(shouldNotSucceed).recover(expectingRuntimeException)
      }
    }

    "successfully display the direct debit confirmation page" in {
      running(app) {
        //when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
        when(mockSessionCache.getJourney(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
        val request = FakeRequest()
          .withSession(goodSession: _*)
        implicit val messages = getMessages(request)
        val response = controller.getDirectDebitConfirmation(request)

        status(response) mustBe OK

        contentAsString(response) must include(getMessages(request)("ssttp.arrangement.direct-debit.confirmation.title"))
      }
    }
  }

  private def shouldNotSucceed: PartialFunction[Result, Unit] = {
    case _ => fail("Method call should not have succeeded"); Unit
  }

  private def expectingRuntimeException: PartialFunction[Throwable, Unit] = {
    case e: RuntimeException => Unit
    case e                   => fail(s"Wrong exception type was thrown: ${e.getClass.getSimpleName}"); Unit
  }
}
