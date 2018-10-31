/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.Format
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._
import uk.gov.hmrc.selfservicetimetopay.service.{AuditService, CalculatorService}
import uk.gov.hmrc.selfservicetimetopay.util.TTPSessionId

import scala.concurrent.Future

class ArrangementControllerSpec extends PlayMessagesSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val ddConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val auditService: AuditService = mock[AuditService]
  val arrangementConnector: ArrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector: TaxPayerConnector = mock[TaxPayerConnector]
  val calculatorService: CalculatorService = mock[CalculatorService]
  val calculatorConnecter: CalculatorConnector = mock[CalculatorConnector]
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockCacheMap: CacheMap = mock[CacheMap]

  val controller = new ArrangementController(messagesApi, ddConnector, arrangementConnector, calculatorService, calculatorConnecter, taxPayerConnector, mockEligibilityConnector, auditService) {
    override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    override lazy val authConnector: AuthConnector = mockAuthConnector
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuthConnector,
      mockSessionCache,
      ddConnector,
      arrangementConnector,
      taxPayerConnector,
      calculatorService,
      mockEligibilityConnector)
  }

  val validDayForm = Seq(
    "dayOfMonth" -> "10"
  )
  "Self Service Time To Pay Arrangement Controller" must {

    "redirect to 'you need to file' when sa debits are less than £32.00 for determine eligibility" in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val requiredSa = selfAssessment.get.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any(), any())(any(), any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(DebtIsInsignificant))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer.copy(selfAssessment = Some(requiredSa)))))

      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      val response = controller.determineEligibility().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSessionId.newTTPSession(),
          "token" -> "1234"
        )
      )


      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }
    "redirect to 'you need to file' when the user has not filled " in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val requiredSa = selfAssessment.get.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any(), any())(any(), any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(ReturnNeedsSubmitting))))

      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer.copy(selfAssessment = Some(requiredSa)))))
      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      val response = controller.determineEligibility()
        .apply(FakeRequest()
          .withSession(
            SessionKeys.userId -> "someUserId",
            TTPSessionId.newTTPSession(),
            "token" -> "1234"
          )
        )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }


    "redirect to 'Tax Liabilities' when no amounts have been entered for determine eligibility" in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any(), any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial))))

      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = controller.determineEligibility().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSessionId.newTTPSession(),
          "token" -> "1234"
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.CalculatorController.getTaxLiabilities().url
    }


    "redirect to 'call us page' when entered amounts and sa amounts are equal and user is ineligible for determine eligibility " in {
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer)))
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = taxPayer.selfAssessment.get.debits)))))

      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any(), any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(DebtIsInsignificant))))

      val response = controller.determineEligibility().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSessionId.newTTPSession(),
          "token" -> "1234"
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion().url
    }


    "redirect to call us page when tax payer connector fails to retrieve data for determine eligibility" in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(None))

      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val response = controller.determineEligibility().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSessionId.newTTPSession(),
          "token" -> "1234"
        )
      )

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion().url
    }


    "successfully display the application complete page with required data in submission" in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      val requiredSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = taxPayer.selfAssessment.get.debits))

      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      when(mockSessionCache.remove()(any(), any())).thenReturn(Future.successful(mock[HttpResponse]))

      val request = FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSessionId.newTTPSession(),
        "token" -> "1234"
      )


      val response = controller.applicationComplete().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.arrangement.complete.title"))
    }

    "redirect to the start page when missing required data for the application complete page" in {
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val response = controller.applicationComplete().apply(FakeRequest()
        .withSession(
          SessionKeys.userId -> "someUserId",
          TTPSessionId.newTTPSession(),
          "token" -> "1234"
        )
      )

      status(response) mustBe SEE_OTHER
      controllers.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "return success and display the application complete page on successfully set up debit when DES call returns an error" in {

      implicit val hc = new HeaderCarrier
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(arrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Left(SubmissionError(GATEWAY_TIMEOUT, "Timeout"))))

      val response = controller.submit().apply(FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSessionId.newTTPSession(),
        "token" -> "1234"
      )
      )

      controllers.routes.ArrangementController.applicationComplete().url must endWith(redirectLocation(response).get)
    }

    "redirect to start page if there is no data in the session cache" in {
      when(mockSessionCache.get(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val response = controller.submit().apply(FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSessionId.newTTPSession(),
        "token" -> "1234"
      )
      )
      controllers.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }


    "redirect to login if user not logged in" in {
      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockSessionCache.get(any(), any(), any())).thenReturn(Future.successful(None))
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val response = controller.submit().apply(FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSessionId.newTTPSession(),
        "token" -> "1234"
      )
      )


      redirectLocation(response).get contains "/gg/sign-in"
    }


    "redirect to getTaxLiabilities page if the not logged in user has not created any debits" in {
      implicit val hc = new HeaderCarrier
      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val localTtpSubmission = ttpSubmission.copy(calculatorData = ttpSubmission.calculatorData.copy(debits = Seq.empty))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any(), any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(taxPayerConnector.getTaxPayer(any())(any(), any())).thenReturn(Future.successful(Some(taxPayer))) //121.20 debits
      when(mockSessionCache.get(any(), any(), any())).thenReturn(Future.successful(Some(localTtpSubmission)))
      when(mockSessionCache.put(any())(any(), any(), any())).thenReturn(Future.successful(mockCacheMap))
      when(mockCacheMap.getEntry(any())(any[Format[TTPSubmission]]())).thenReturn(Some(localTtpSubmission))

      val response = controller.determineEligibility().apply(FakeRequest().withSession(
        SessionKeys.userId -> "someUserId",
        TTPSessionId.newTTPSession(),
        "token" -> "1234"
      )
      )


      controllers.routes.CalculatorController.getTaxLiabilities().url must endWith(redirectLocation(response).get)
    }
  }

  "ttpSessionId" must {
    val controller = new TimeToPayController() {
      def go() = Action {
        Ok("")
      }
    }

    "be set within the session cookie when the user first hits a page" in {
      val eventualResult = controller.go()(FakeRequest())
      status(eventualResult) mustBe SEE_OTHER
      session(eventualResult).get(TTPSessionId.ttpSessionId).isDefined mustBe true
    }

  }
}
