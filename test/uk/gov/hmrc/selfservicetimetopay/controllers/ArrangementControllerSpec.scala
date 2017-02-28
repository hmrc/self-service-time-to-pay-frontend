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

import java.time.LocalDate

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Format
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._
import uk.gov.hmrc.selfservicetimetopay.util.SessionProvider

import scala.concurrent.Future

class ArrangementControllerSpec extends PlayMessagesSpec
  with MockitoSugar  with ScalaFutures with BeforeAndAfterEach {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val ddConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val arrangementConnector: ArrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector: TaxPayerConnector = mock[TaxPayerConnector]
  val calculatorConnector: CalculatorConnector = mock[CalculatorConnector]
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockSessionProvider: SessionProvider = mock[SessionProvider]
  val mockCacheMap: CacheMap = mock[CacheMap]

  val controller = new ArrangementController(mock[play.api.i18n.MessagesApi], ddConnector, arrangementConnector, calculatorConnector, taxPayerConnector, mockEligibilityConnector) {
    override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    override lazy val authConnector: AuthConnector = mockAuthConnector
    override lazy val authenticationProvider: GovernmentGateway = mockAuthenticationProvider
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuthConnector,
      mockSessionCache,
      ddConnector,
      arrangementConnector,
      taxPayerConnector,
      calculatorConnector,
      mockSessionProvider,
      mockEligibilityConnector)
  }

  val validDayForm = Seq(
    "dayOfMonth" -> "10"
  )

  "Self Service Time To Pay Arrangement Controller" should {
    "return success and display the application complete page" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(arrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Right(SubmissionSuccess())))


      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withCookies(sessionProvider.createTtpCookie()).withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.ArrangementController.applicationComplete().url must endWith(redirectLocation(response).get)
    }

    "return success and display the application complete page on successfully set up debit when DES call returns an error" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(arrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Left(SubmissionError(504, "Timeout"))))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withCookies(sessionProvider.createTtpCookie()).withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.ArrangementController.applicationComplete().url must endWith(redirectLocation(response).get)
    }

    "redirect to start if no data in session cache" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)

    }

    "update payment schedule date" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.changeSchedulePaymentDay()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary/change-day")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId")
        .withFormUrlEncodedBody(validDayForm: _*))

      controllers.routes.ArrangementController.getInstalmentSummary().url must endWith(redirectLocation(response).get)
    }

    "redirect to login if user not logged in" in {
      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withCookies(sessionProvider.createTtpCookie()))

      redirectLocation(response).get contains "/gg/sign-in"
    }

    "redirect to ineligible (call us) page if EligibilityStatus is set to false within the TTPSubmission data (where an authenticated resource is called)" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(eligibilityStatus = Option(EligibilityStatus(eligible = false, Seq("error")))))))

      val response = controller.getInstalmentSummary()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary")
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url must endWith(redirectLocation(response).get)
    }

    "NOT redirect to ineligible (call us) page if EligibilityStatus is None within the TTPSubmission data (where an authenticated resource is called)" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.getInstalmentSummary()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary")
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      redirectLocation(response) mustNot be(controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url)
    }

    "NOT redirect to ineligible (call us) page if EligibilityStatus is successful within" +
      "the TTPSubmission data (where an authenticated resource is called)" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(eligibilityStatus = Option(EligibilityStatus(eligible = true, Seq.empty))))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.getInstalmentSummary()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary")
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      redirectLocation(response) mustNot be(controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url)
    }

    "redirect to misalignment page if logged in and not logged in debits do not sum() to the same value" in {
      implicit val hc = new HeaderCarrier

      val localTtpSubmission = ttpSubmission.copy(calculatorData =
        ttpSubmission.calculatorData.copy(debits = Seq(Debit(amount = 212.01, dueDate = LocalDate.now()))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.CalculatorController.getMisalignmentPage().url must endWith(redirectLocation(response).get)
    }

    "redirect to getPayTodayQuestion page if logged in and not logged in debits do sum() to the same value" in {
      implicit val hc = new HeaderCarrier

      val localTtpSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.CalculatorController.getPayTodayQuestion().url must endWith(redirectLocation(response).get)
    }

    "redirect to getPayTodayQuestion page if the not logged in user has not created any debits" in {
      implicit val hc = new HeaderCarrier

      val localTtpSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = Seq.empty))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId"))

      controllers.routes.CalculatorController.getPayTodayQuestion().url must endWith(redirectLocation(response).get)
    }
  }

  "ttpSessionId" must {
    val controller = new TimeToPayController() {
      override val sessionProvider: SessionProvider = mockSessionProvider

      def go() = Action { Ok("") }
    }

    "be set within the session cookie when the user first hits a page" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status((controller.go()(FakeRequest()))) mustBe 303
      verify(mockSessionProvider, times(1)).createTtpCookie()
    }

    "be set within the session cookie every time a user hits a page without it being sent in the session" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status((controller.go()(FakeRequest()))) mustBe 303
      status((controller.go()(FakeRequest()))) mustBe 303
      status((controller.go()(FakeRequest().withSession("sessionId" -> "22222")))) mustBe 303
      status((controller.go()(FakeRequest().withSession()))) mustBe 303
      verify(mockSessionProvider, times(4)).createTtpCookie()
    }

    "be maintained across multiple not logged in requests" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status((controller.go()(FakeRequest()))) mustBe 303
      status((controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) mustBe 200
      status((controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) mustBe 200
      status((controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) mustBe 200
      status((controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) mustBe 200

      status((controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "123456"))))) mustBe 200
      verify(mockSessionProvider, times(1)).createTtpCookie()
    }
  }
}
