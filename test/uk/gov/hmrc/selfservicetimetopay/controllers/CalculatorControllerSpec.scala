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

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream._
import config.AppConfig
import controllers.action.Actions
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Application
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._
import play.api.test._
import ssttpcalculator.{CalculatorController, CalculatorService}
import sttpsubmission.SubmissionService
import token.TokenService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class CalculatorControllerSpec extends PlayMessagesSpec with MockitoSugar with BeforeAndAfterEach {

  val mockSessionCache: SubmissionService = mock[SubmissionService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockCalculatorService: CalculatorService = mock[CalculatorService]
  val mockSessionCache4TokensConnector: TokenService = mock[TokenService]
  implicit val appConfig: AppConfig = mock[AppConfig]

  val controller: CalculatorController = new CalculatorController(
    messagesApi       = messagesApi,
    calculatorService = mockCalculatorService,
    i18nSupport       = i18nSupport,
    as                = mock[Actions],
    submissionService = mockSessionCache
  )

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val mat: akka.stream.Materializer = ActorMaterializer()

  override def beforeEach() {
    reset(mockSessionCache, mockCalculatorService)
  }

  //  when(mockAuthConnector.currentAuthority(any(), any())).thenReturn(Future.successful(Some(authorisedUser)))
  when(mockSessionCache4TokensConnector.put(any())(any())).thenReturn(Future.successful(()))

  "CalculatorControllerSpec" should {
    "getCalculateInstalments Return 303 when there is no Sa in session" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(
        mockSessionCache.getTtpSessionCarrier(any(), any(), any())
      )
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      val result = controller
        .getCalculateInstalments()
        .apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "getCalculateInstalments Return 200 when there is a Sa in session" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.getCalculateInstalments().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe OK
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }
    "submitCalculateInstalments Return 303 when there is no Sa in session" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "submitCalculateInstalments Return 400 when there is a Sa in session but nothing was posted" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "submitCalculateInstalments Return 303 when there is a Sa in session" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withSession(goodSession: _*)
        .withFormUrlEncodedBody("chosen-month" -> "3"))

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "submitCalculateInstalments put the chosen months of instalments into the session" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withSession(goodSession: _*)
        .withFormUrlEncodedBody("chosen-month" -> "3"))
      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
      verify(mockSessionCache, times(1)).putTtpSessionCarrier(any())(any(), any(), any())
    }

    "Return BadRequest if the form value = total amount due" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier
      val submission = ttpSubmissionNLI
        .copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = BigDecimal("300.00"), dueDate = LocalDate.now()))))

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = requestWithCsrfToken(controller.submitPaymentToday(), "300.00")

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "Return BadRequest if the form value has more than 2 decimal places" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      val submission = ttpSubmissionNLI
        .copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = requestWithCsrfToken(controller.submitPaymentToday(), "299.999")

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getTtpSessionCarrier(any(), any(), any())
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val result = controller.submitPaymentToday().apply(FakeRequest())
      status(result) mustBe SEE_OTHER
    }

    "Return the payment-today from for getPayTodayQuestion if there is an initial payment  already made" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k.copy(calculatorData = CalculatorInput.initial.copy(initialPayment = BigDecimal(2))))))
      val request = FakeRequest()
        .withSession(goodSession: _*)
      val result = controller.getPayTodayQuestion().apply(request)

      status(result) mustBe SEE_OTHER
    }

    "Return 303 for getPayTodayQuestion when TTPSubmission is missing" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe SEE_OTHER
      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 303 for submitPayTodayQuestion when there are no debits" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = Seq.empty)))))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(result) mustBe SEE_OTHER
      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 200 for submitPayTodayQuestion if there are debits and valid eligibility answers" in {
      implicit val hc: HeaderCarrier = new HeaderCarrier

      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))
      val request = FakeRequest()
        .withSession(goodSession: _*)
      val result = controller.getPayTodayQuestion().apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.payment_today_question.title"))
    }

    "successfully display payment summary page" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.getPaymentSummary().apply(request)

      status(response) mustBe OK

      contentAsString(response) must include(getMessages(request)("ssttp.calculator.form.payment_summary.title"))
    }

    "successfully redirect to start page when trying to access what you owe review page if there are no debits" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k.copy(calculatorData = CalculatorInput.initial))))
      val response = controller.getPaymentSummary().apply(FakeRequest().withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER

      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "successfully redirect to start page when there are invalid eligibility questions" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val response = controller.getPaymentSummary().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER

      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "submitSignIn should redirect with a good session " in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val response = controller.submitSignIn().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER
    }

    "getPaymentToday should redirect with a good session " in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val response = controller.getPaymentToday().apply(FakeRequest()
        .withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER
    }

    "getPaymentPlanCalculator should load the Payment Plan Calculator Start" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.getPaymentPlanCalculator().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.payment-plan-calculator.start.title"))
    }

    "getAmountDue should load the amount due page" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.getAmountDue().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.amount-due.start.title"))
    }

    "submitAmountDue should load the getAmountDue Page with a 400" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.submitAmountDue().apply(request)

      status(response) mustBe BAD_REQUEST
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.amount-due.start.title"))
    }

    "submitAmountDue should update the session with amount submitted" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.submitAmountDue().apply(request.withFormUrlEncodedBody("amount" -> "500"))

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getCalculateInstalmentsUnAuth().url must endWith(redirectLocation(response).get)
      verify(mockSessionCache, times(1)).putTtpSessionCarrier(any())(any(), any(), any())
    }

    "getCalculateInstalmentsUnAuth should load the getCalculateInstalmentsUnAuth if amountDue is in the session" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(2)))))))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.getCalculateInstalmentsUnAuth().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.results.title"))
    }

    "getCheckCalculation should load the check calculation page if amountDue is in the session and the chosen shcedule is there" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(2), Some(calculatorPaymentSchedule)))))))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.getCheckCalculation().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.check-calculation.h1"))
    }

    "submitCalculateInstalmentsUnAuth should return a bad request if the data is bad " in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(2)))))))
      when(mockCalculatorService.getInstalmentsScheduleUnAuth(any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      val request = FakeRequest().withSession(goodSession: _*)
      val response = controller.submitCalculateInstalmentsUnAuth().apply(request)

      status(response) mustBe BAD_REQUEST
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.results.title"))
    }

    "submitCalculateInstalmentsUnAuth should redirect a bad request if the data is bad " in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(2)))))))
      when(mockCalculatorService.getInstalmentsScheduleUnAuth(any())(any(), any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      val request = FakeRequest().withFormUrlEncodedBody("chosen-month" -> "2").withSession(goodSession: _*)
      val response = controller.submitCalculateInstalmentsUnAuth().apply(request)

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getCheckCalculation().url must endWith(redirectLocation(response).get)
    }

    "submitPayTodayQuestion should redirect with a good session and good request and to the getPaymentToday if true is selected" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(
        Future.successful(
          Some(ttpSubmissionNLI.copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = 300.0, dueDate = LocalDate.now())))))
        )
      )
      val response = controller.submitPayTodayQuestion().apply(FakeRequest()
        .withFormUrlEncodedBody("paytoday" -> "true")
        .withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getPaymentToday().url must endWith(redirectLocation(response).get)
    }

    "submitPayTodayQuestion should redirect with a good session and good request and to the getCalculateInstalments if true is selected" in {
      when(mockSessionCache.getTtpSessionCarrier(any(), any(), any())).thenReturn(
        Future.successful(
          Some(ttpSubmissionNLI.copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = 300.0, dueDate = LocalDate.now())))))
        )
      )
      when(mockSessionCache.putTtpSessionCarrier(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
      val response = controller.submitPayTodayQuestion().apply(FakeRequest()
        .withFormUrlEncodedBody("paytoday" -> "false")
        .withSession(goodSession: _*))

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getCalculateInstalments().url must endWith(redirectLocation(response).get)
    }
  }

  private def requestWithCsrfToken(action: Action[AnyContent], amount: String)(implicit app: Application) = {
    val csrfAddToken = app.injector.instanceOf[play.filters.csrf.CSRFAddToken]
    csrfAddToken(action).apply(FakeRequest()
      .withFormUrlEncodedBody("amount" -> amount)
      .withSession(goodSession: _*))
  }
}
