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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._
import uk.gov.hmrc.selfservicetimetopay.util.TTPSession

import scala.concurrent.Future

class ArrangementControllerSpec extends PlayMessagesSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val ddConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val arrangementConnector: ArrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector: TaxPayerConnector = mock[TaxPayerConnector]
  val calculatorConnector: CalculatorConnector = mock[CalculatorConnector]
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockCacheMap: CacheMap = mock[CacheMap]

  val controller = new ArrangementController(messagesApi, ddConnector, arrangementConnector, calculatorConnector, taxPayerConnector, mockEligibilityConnector) {
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
      mockEligibilityConnector)
  }

  val validDayForm = Seq(
    "dayOfMonth" -> "10"
  )

  "Self Service Time To Pay Arrangement Controller" must {
    "redirect to start with an empty submission for determine misalignment" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )


      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.start().url
    }

    "redirect to 'you need to file' when sa debits are less than £32.00 for determine misalignment" in {
      val requiredSa = selfAssessment.get.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(DebtIsInsignificant))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer.copy(selfAssessment = Some(requiredSa)))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )


      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }
    "redirect to 'you need to file' when the user has not filled " in {
      val requiredSa = selfAssessment.get.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(ReturnNeedsSubmitting))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer.copy(selfAssessment = Some(requiredSa)))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      val response = controller.determineMisalignment()
        .apply(FakeRequest()
          .withSession(
            SessionKeys.userId -> "someUserId",
            TTPSession.newTTPSession()
          )
        )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }


    "redirect to 'Tax Liabilities' when no amounts have been entered for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.CalculatorController.getTaxLiabilities().url
    }

    "redirect to 'instalment summary' when entered amounts and sa amounts are equal and user is eligible for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = taxPayer.selfAssessment.get.debits)))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.ArrangementController.getInstalmentSummary().url
    }

    "redirect to 'call us page' when entered amounts and sa amounts are equal and user is ineligible for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = taxPayer.selfAssessment.get.debits)))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(DebtIsInsignificant))))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "redirect to misalignment when entered amounts and sa amounts aren't equal for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = Seq(calculatorAmountDue))))))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.CalculatorController.getMisalignmentPage().url
    }

    "redirect to call us page when tax payer connector fails to retrieve data for determine misalignment" in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(None))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val response = controller.determineMisalignment().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "successfully display the instalment summary page with required data in submission" in {
      val requiredSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))


      val request = FakeRequest()
      .withSession(
        SessionKeys.userId -> "someUserId",
        TTPSession.newTTPSession()
      )


      val response = controller.getInstalmentSummary().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.arrangement.instalment-summary.title"))
    }

    "redirect to the start page when missing required data for the instalment summary page" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.getInstalmentSummary().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.start().url
    }

    "successfully display the application complete page with required data in submission" in {

      val requiredSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      when(mockSessionCache.remove()(any())).thenReturn(Future.successful(mock[HttpResponse]))

      val request = FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSession.newTTPSession()
      )


      val response = controller.applicationComplete().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.arrangement.complete.title"))
    }

    "redirect to the start page when missing required data for the application complete page" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.applicationComplete().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      status(response) mustBe SEE_OTHER
      controllers.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "return success and display the application complete page on successfully set up debit when DES call returns an error" in {

      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(arrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Left(SubmissionError(GATEWAY_TIMEOUT, "Timeout"))))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )

      controllers.routes.ArrangementController.applicationComplete().url must endWith(redirectLocation(response).get)
    }

    "redirect to start page if there is no data in the session cache" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(
        SessionKeys.userId -> "someUserId",
        TTPSession.newTTPSession()
      )
      )
      controllers.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "update payment schedule date" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      when(calculatorConnector.calculatePaymentSchedule(any())(any())).thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      val response = controller.changeSchedulePaymentDay()
        .apply(FakeRequest("POST", "/arrangement/instalment-summary/change-day").withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
        .withFormUrlEncodedBody(validDayForm: _*))

      controllers.routes.ArrangementController.getInstalmentSummary().url must endWith(redirectLocation(response).get)
    }

    "redirect to login if user not logged in" in {

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit").withSession(
          TTPSession.newTTPSession()
        )
      )


      redirectLocation(response).get contains "/gg/sign-in"
    }

    "redirect to misalignment page if logged in and not logged in debits do not sum() to the same value" in {
      implicit val hc = new HeaderCarrier
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      val localTtpSubmission = ttpSubmission.copy(calculatorData =
        ttpSubmission.calculatorData.copy(debits = Seq(Debit(amount = 212.01, dueDate = LocalDate.now()))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment")
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
       )

      controllers.routes.CalculatorController.getMisalignmentPage().url must endWith(redirectLocation(response).get)
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

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment").withSession(
        SessionKeys.userId -> "someUserId",
        TTPSession.newTTPSession()
      ))

      controllers.routes.ArrangementController.getInstalmentSummary().url must endWith(redirectLocation(response).get)
    }

    "redirect to getTaxLiabilities page if the not logged in user has not created any debits" in {
      implicit val hc = new HeaderCarrier

      val localTtpSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = Seq.empty))
      when(mockEligibilityConnector.checkEligibility(any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))

      val response = controller.determineMisalignment().apply(FakeRequest("GET", "/arrangement/determine-misalignment").withSession(
          SessionKeys.userId -> "someUserId",
          TTPSession.newTTPSession()
        )
      )


      controllers.routes.CalculatorController.getTaxLiabilities().url must endWith(redirectLocation(response).get)
    }
  }

  "ttpSessionId" must {
    val controller = new TimeToPayController() {
      def go() = Action { Ok("") }
    }

    "be set within the session cookie when the user first hits a page" in {
      val eventualResult = controller.go()(FakeRequest())
      status(eventualResult) mustBe SEE_OTHER
      session(eventualResult).get(TTPSession.ttpSessionId).isDefined mustBe true
    }

  }
}
