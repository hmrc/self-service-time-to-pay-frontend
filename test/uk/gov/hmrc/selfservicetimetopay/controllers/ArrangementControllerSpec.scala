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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength, SaAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class ArrangementControllerSpec extends UnitSpec
  with MockitoSugar with WithFakeApplication with ScalaFutures {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val ddConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val arrangementConnector: ArrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector: TaxPayerConnector = mock[TaxPayerConnector]
  val calculatorConnector: CalculatorConnector = mock[CalculatorConnector]
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]

  val controller = new ArrangementController(ddConnector, arrangementConnector, calculatorConnector, taxPayerConnector) {
    override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    override lazy val authConnector: AuthConnector = mockAuthConnector

    val loggedInUser = LoggedInUser("foo/123456789", None, None, None, CredentialStrength.Weak, ConfidenceLevel.L100)
    val saAccount = SaAccount(link = "link", utr = SaUtr("1233"))
    val authContext = AuthContext(user = loggedInUser, principal = Principal(name = Some("usere"),
      accounts = Accounts(sa = Some(saAccount))), attorney = None, userDetailsUri = None, enrolmentsUri = None)

    override def AuthorisedSaUser(body: AsyncPlayUserRequest) = Action.async {
      body(authContext)
    }
  }

  val unauthorisedController = new ArrangementController(ddConnector, arrangementConnector, calculatorConnector, taxPayerConnector) {
    override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    override lazy val authConnector: AuthConnector = mockAuthConnector

  }


  val validDayForm = Seq(
    "dayOfMonth" -> "10"
  )

  "Self Service Time To Pay Arrangement Controller" should {
    "return success and display the application complete page" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(directDebitInstructionPaymentPlan))

      when(arrangementConnector.submitArrangements(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(SubmissionSuccess())))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(sessionProvider.createSessionId()))

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.applicationComplete().url
    }

    "redirect to start if no data in session cache" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(sessionProvider.createSessionId()))

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.start().url

    }
    "update payment schedule date" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.changeSchedulePaymentDay().apply(FakeRequest("POST", "/arrangement/instalment-summary/change-day").withSession(sessionProvider.createSessionId())
        .withFormUrlEncodedBody(validDayForm: _*))

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.getInstalmentSummary().url
    }
    "redirect to login if user not logged in" in {

      val response = unauthorisedController.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(sessionProvider.createSessionId()))

      redirectLocation(response).get contains "/gg/sign-in"

    }

  }
}
