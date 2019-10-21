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

import java.time.Clock

import _root_.controllers.action._
import audit.AuditService
import config.AppConfig
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Format
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import ssttparrangement.{ArrangementConnector, ArrangementController, SubmissionError, SubmissionSuccess}
import ssttpcalculator.{CalculatorConnector, CalculatorService}
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.EligibilityConnector
import journey.{Journey, JourneyService}
import req.RequestSupport
import testsupport.testdata.TdAll
import timetopaytaxpayer.cor.TaxpayerConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArrangementControllerSpec extends PlayMessagesSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  implicit val appConfig: AppConfig = mock[AppConfig]
  implicit val request = TdAll.request

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockMessagesControllerComponents: MessagesControllerComponents = mock[MessagesControllerComponents]
  val mockDirectDebitConnector: DirectDebitConnector = mock[DirectDebitConnector]
  val mockArrangementConnector: ArrangementConnector = mock[ArrangementConnector]
  val mockCalculatorService: CalculatorService = mock[CalculatorService]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]
  val mockTaxPayerConnector: TaxpayerConnector = mock[TaxpayerConnector]
  val mockEligibilityConnector: EligibilityConnector = mock[EligibilityConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val mockSessionCache: JourneyService = mock[JourneyService]
  val mockActions: Actions = mock[Actions]
  val mockCacheMap: CacheMap = mock[CacheMap]
  val mockRequestSupport: RequestSupport = mock[RequestSupport]
  val mockViews: Views = mock[Views]
  val mockClock: Clock = mock[Clock]

  val controller = new ArrangementController(
    mcc                  = mockMessagesControllerComponents,
    ddConnector          = mockDirectDebitConnector,
    arrangementConnector = mockArrangementConnector,
    calculatorService    = mockCalculatorService,
    calculatorConnector  = mockCalculatorConnector,
    taxPayerConnector    = mockTaxPayerConnector,
    eligibilityConnector = mockEligibilityConnector,
    auditService         = mockAuditService,
    journeyService       = mockSessionCache,
    as                   = mockActions,
    requestSupport       = mockRequestSupport,
    views                = mockViews,
    clock                = mockClock
  )
  val fakeRequest = FakeRequest().withSession(("_*", "_*"))
  val fakeResponse = controller.submit().apply(FakeRequest().withSession(("_*", "_*")))

  override protected def beforeEach(): Unit = {
    reset(mockAuthConnector,
          mockSessionCache,
          mockDirectDebitConnector,
          mockArrangementConnector,
          mockTaxPayerConnector,
          mockCalculatorConnector,
          mockEligibilityConnector)
  }

  val validDayForm = Seq(
    "dayOfMonth" -> "10"
  )
  "Self Service Time To Pay Arrangement Controller" must {

    "redirect to 'you need to file' when sa debits are less than £32.00 for determine eligibility" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val requiredSelfAssessment = selfAssessment.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(DebtIsInsignificant))))

      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer.copy(selfAssessment = requiredSelfAssessment)))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))
      when(mockSessionCache.saveJourney(any())(any()))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }
    "redirect to 'to ia' when the user is not on ia" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(IsNotOnIa))))

      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))
      //when(mockSessionCache.saveJourney(any())(any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockSessionCache.saveJourney(any())(any()))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse().url
    }
    "redirect to 'over ten k' when the user is has debts over 10k" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(TotalDebtIsTooHigh))))

      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))
      when(mockSessionCache.saveJourney(any())(any()))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge().url
    }
    "redirect to 'you need to file' when the user has not filled " in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val requiredSelfAssessment = selfAssessment.copy(debits = Seq.empty)
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(ReturnNeedsSubmitting))))

      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer.copy(selfAssessment = requiredSelfAssessment)))
      when(mockSessionCache.saveJourney(any())(any()))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile().url
    }

    "redirect to 'Tax Liabilities' when no amounts have been entered for determine eligibility" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      when(mockSessionCache.saveJourney(any())(any()))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpcalculator.routes.CalculatorController.getTaxLiabilities().url
    }

    "redirect to 'call us page' when entered amounts and sa amounts are equal and user is ineligible for determine eligibility " in {
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer))
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      when(mockSessionCache.saveJourney(any())(any()))
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = false, Seq(OldDebtIsTooHigh))))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion().url
    }

    "redirect to call us page when tax payer connector fails to retrieve data for determine eligibility" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.failed(throw new Exception("Not found taxpayer")))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))

      status(fakeResponse) mustBe SEE_OTHER
      redirectLocation(fakeResponse).get mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion().url
    }
    "send user to the change payment day page in" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.failed(throw new Exception("Not found taxpayer")))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      val response = controller.getChangeSchedulePaymentDay().apply(fakeRequest)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.arrangement.change_day.title"))
    }

    "send user to the Declaration page" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.failed(new Exception("No taxpayer found")))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      val response = controller.getDeclaration().apply(fakeRequest)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(fakeRequest)("ssttp.arrangement.declaration.h1"))
    }

    "successfully display the application complete page with required data in submission" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))

      val requiredSubmission = ttpSubmission

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(requiredSubmission))
      //TODO this method doesn't exist anymore not sure what to replace with
      //when(mockSessionCache.remove()(any())).thenReturn(Future.successful(mock[HttpResponse]))

      val response = controller.applicationComplete().apply(fakeRequest)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(fakeRequest)("ssttp.arrangement.complete.title"))
    }

    "redirect to the start page when missing required data for the application complete page" in {
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.getJourney())
        //TODO not sure what to use to represent the fail/none here
        // .thenReturn(Future.successful(None))
        .thenReturn(Future.successful(ttpSubmission))

      val response = controller.applicationComplete().apply(fakeRequest)

      status(response) mustBe SEE_OTHER
      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "return success and display the application complete page on successfully set up debit when DES call returns an error" in {

      implicit val hc = new HeaderCarrier
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      when(mockDirectDebitConnector.createPaymentPlan(any(), any())(any())).thenReturn(Future.successful(Right(directDebitInstructionPaymentPlan)))

      when(mockArrangementConnector.submitArrangements(any())(any())).thenReturn(Future.successful(Left(SubmissionError(GATEWAY_TIMEOUT, "Timeout"))))

      val response = controller.submit().apply(fakeRequest)

      ssttparrangement.routes.ArrangementController.applicationComplete().url must endWith(redirectLocation(response).get)
    }

    "redirect to start page if there is no data in the session cache" in {
      when(mockSessionCache.getJourney())
        //TODO not sure what to use to represent the fail/none here
        // .thenReturn(Future.successful(None))
        .thenReturn(Future.successful(ttpSubmission))
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val response = controller.submit().apply(fakeRequest)
      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "redirect to login if user not logged in" in {
      when(mockSessionCache.saveJourney(any())(any()))
      //TODO not sure what to use to represent the fail/none here
      // .thenReturn(Future.successful(None))
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val response = controller.submit().apply(fakeRequest)

      redirectLocation(response).get contains "/gg/sign-in"
    }

    "redirect to getTaxLiabilities page if the not logged in user has not created any debits" in {
      implicit val hc = new HeaderCarrier
      //      when(mockAuthConnector.currentAuthority(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(authorisedUser)))
      val localJourney = ttpSubmission
      when(mockEligibilityConnector.checkEligibility(any(), any())(any())).thenReturn(Future.successful(EligibilityStatus(eligible = true, Seq.empty)))
      when(mockTaxPayerConnector.getTaxPayer(any())(any())).thenReturn(Future.successful(taxPayer)) //121.20 debits
      when(mockSessionCache.getJourney()).thenReturn(Future.successful(localJourney))
      when(mockSessionCache.saveJourney(any())(any()))
      when(mockCacheMap.getEntry(any())(any[Format[Journey]]())).thenReturn(Some(localJourney))

      val response = controller.submit().apply(fakeRequest)

      ssttpcalculator.routes.CalculatorController.getTaxLiabilities().url must endWith(redirectLocation(response).get)
    }
  }
}

