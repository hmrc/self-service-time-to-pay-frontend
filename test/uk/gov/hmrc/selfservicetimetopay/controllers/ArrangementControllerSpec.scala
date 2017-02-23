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
import play.api.http.Status
import play.api.i18n.Messages
import play.api.libs.json.Format
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.GovernmentGateway
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._
import uk.gov.hmrc.selfservicetimetopay.util.SessionProvider

import scala.concurrent.Future

class ArrangementControllerSpec extends UnitSpec
  with MockitoSugar with WithFakeApplication with ScalaFutures with BeforeAndAfterEach {
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

  val controller = new ArrangementController(ddConnector, arrangementConnector, calculatorConnector, taxPayerConnector, mockEligibilityConnector) {
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
    "redirect to start with an empty submission for determine misalignment" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.start().url
    }

    "redirect to you need to file when sa debits are less than £32.00 for determine misalignment" in {
      val requiredSa = selfAssessment.get.copy(debits = Seq.empty)

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer.copy(selfAssessment = Some(requiredSa)))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }

    "redirect to pay today question when no amounts have been entered for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.CalculatorController.getPayTodayQuestion().url
    }

    "redirect to instalment summary when entered amounts and sa amounts are equal and user is eligible for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = taxPayer.selfAssessment.get.debits)))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.ArrangementController.getInstalmentSummary().url
    }

    "redirect to call us page when entered amounts and sa amounts are equal and user is ineligible for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = taxPayer.selfAssessment.get.debits)))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq("ineligible"))))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "redirect to misalignment when entered amounts and sa amounts aren't equal for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDue))))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.CalculatorController.getMisalignmentPage().url
    }

    "redirect to call us page when tax payer connector fails to retrieve data for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(None))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val response = await(controller.determineMisalignment()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "successfully display the instalment summary page with required data in submission" in {
      val requiredSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = await(controller.getInstalmentSummary()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe OK
      bodyOf(response) should include(Messages("ssttp.arrangement.instalment-summary.title"))
    }

    "redirect to the start page when missing required data for the instalment summary page" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.getInstalmentSummary()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.start().url
    }

    "successfully display the application complete page with required data in submission" in {

      val requiredSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      when(mockSessionCache.remove()(any())).thenReturn(Future.successful(mock[HttpResponse]))

      val response = await(controller.applicationComplete()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId")))

      status(response) shouldBe OK
      bodyOf(response) should include(Messages("ssttp.arrangement.complete.title"))
    }

    "redirect to the start page when missing required data for the application complete page" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.applicationComplete()
        .apply(FakeRequest()
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe routes.SelfServiceTimeToPayController.start().url
    }

    "return success and display the application complete page on successfully set up debit when DES call returns an error" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(arrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Left(SubmissionError(504, "Timeout"))))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withCookies(sessionProvider.createTtpCookie()).withSession(SessionKeys.userId -> "someUserId"))

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.applicationComplete().url
    }

    "redirect to start if no data in session cache" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit")
        .withCookies(sessionProvider.createTtpCookie())
        .withSession(SessionKeys.userId -> "someUserId"))

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.start().url
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

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.getInstalmentSummary().url
    }

    "redirect to login if user not logged in" in {
      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withCookies(sessionProvider.createTtpCookie()))

      redirectLocation(response).get contains "/gg/sign-in"
    }

    "redirect to start page if EligibilityStatus is set to false within the TTPSubmission data (where an authenticated resource is called)" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(eligibilityStatus = Option(EligibilityStatus(eligible = false, Seq("error")))))))

      val response = controller.getInstalmentSummary()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary")
          .withCookies(sessionProvider.createTtpCookie())
          .withSession(SessionKeys.userId -> "someUserId"))

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.start().url
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

      redirectLocation(response).get shouldBe controllers.routes.CalculatorController.getMisalignmentPage().url
    }

    "redirect to instalment summary page if logged in and not logged in debits do sum() to the same value" in {
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

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.getInstalmentSummary().url
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

      redirectLocation(response).get shouldBe controllers.routes.CalculatorController.getPayTodayQuestion().url
    }
  }

  "ttpSessionId" should {
    val controller = new TimeToPayController() {
      override val sessionProvider: SessionProvider = mockSessionProvider

      def go() = Action {
        Ok("")
      }
    }

    "be set within the session cookie when the user first hits a page" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status(await(controller.go()(FakeRequest()))) shouldBe 303
      verify(mockSessionProvider, times(1)).createTtpCookie()
    }

    "be set within the session cookie every time a user hits a page without it being sent in the session" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status(await(controller.go()(FakeRequest()))) shouldBe 303
      status(await(controller.go()(FakeRequest()))) shouldBe 303
      status(await(controller.go()(FakeRequest().withSession("sessionId" -> "22222")))) shouldBe 303
      status(await(controller.go()(FakeRequest().withSession()))) shouldBe 303
      verify(mockSessionProvider, times(4)).createTtpCookie()
    }

    "be maintained across multiple not logged in requests" in {
      when(mockSessionProvider.createTtpCookie()).thenReturn(Cookie(name = ttpSessionId, value = "12345"))
      status(await(controller.go()(FakeRequest()))) shouldBe 303
      status(await(controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) shouldBe 200
      status(await(controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) shouldBe 200
      status(await(controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) shouldBe 200
      status(await(controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "12345"))))) shouldBe 200

      status(await(controller.go()(FakeRequest().withCookies(Cookie(name = ttpSessionId, value = "123456"))))) shouldBe 200
      verify(mockSessionProvider, times(1)).createTtpCookie()
    }
  }
}
