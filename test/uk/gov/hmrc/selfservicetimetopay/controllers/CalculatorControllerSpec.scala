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

import akka.actor.ActorSystem
import akka.stream._
import config.AppConfig
import controllers.action.Actions
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Application
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.test.Helpers._
import play.api.test._
import ssttpcalculator.{CalculatorController, CalculatorService}
import journey.{Journey, JourneyService}
import req.RequestSupport
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus}
import uk.gov.hmrc.selfservicetimetopay.resources._
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CalculatorControllerSpec extends PlayMessagesSpec with MockitoSugar with BeforeAndAfterEach {

  implicit val appConfig: AppConfig = mock[AppConfig]
  implicit val request = TdAll.request
  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val mat: akka.stream.Materializer = ActorMaterializer()

  val mockSessionCache: JourneyService = mock[JourneyService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockCalculatorService: CalculatorService = mock[CalculatorService]
  val mockMessagesControllerComponents: MessagesControllerComponents = mock[MessagesControllerComponents]
  val mockActions: Actions = mock[Actions]
  val mockViews: Views = mock[Views]
  val mockRequestSupport: RequestSupport = mock[RequestSupport]
  val controller: CalculatorController = new CalculatorController(
    mcc               = mockMessagesControllerComponents,
    calculatorService = mockCalculatorService,
    as                = mockActions,
    journeyService    = mockSessionCache,
    views             = mockViews,
    requestSupport    = mockRequestSupport
  )
  val fakeRequest = FakeRequest()

  override def beforeEach() {
    reset(mockSessionCache, mockCalculatorService)
  }

  //when(mockAuthConnector.currentAuthority(any(), any())).thenReturn(Future.successful(Some(authorisedUser)))

  "CalculatorControllerSpec" should {
    "getCalculateInstalments Return 303 when there is no Sa in session" in {

      when(
        mockSessionCache.getJourney()
      )
        .thenReturn(Future.successful(ttpSubmissionNLI))

      val result = controller
        .getCalculateInstalments()
        .apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getJourney()
    }

    "getCalculateInstalments Return 200 when there is a Sa in session" in {

      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmission))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.getCalculateInstalments().apply(FakeRequest())

      status(result) mustBe OK
      verify(mockSessionCache, times(1)).getJourney()
    }
    "submitCalculateInstalments Return 303 when there is no Sa in session" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmissionNLI))

      val result = controller.submitCalculateInstalments().apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getJourney()
    }

    "submitCalculateInstalments Return 400 when there is a Sa in session but nothing was posted" in {

      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmission))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest())

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getJourney()
    }

    "submitCalculateInstalments Return 303 when there is a Sa in session" in {
      when(mockSessionCache.saveJourney(any())(any()))
      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmission))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withFormUrlEncodedBody("chosen-month" -> "3"))

      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getJourney()
    }

    "submitCalculateInstalments put the chosen months of instalments into the session" in {
      when(mockSessionCache.saveJourney(any())(any()))
      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmission))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      val result = controller.submitCalculateInstalments().apply(FakeRequest()
        .withFormUrlEncodedBody("chosen-month" -> "3"))
      status(result) mustBe SEE_OTHER
      verify(mockSessionCache, times(1)).getJourney()
      verify(mockSessionCache, times(1)).saveJourney(any())(any())
    }

    "Return BadRequest if the form value = total amount due" in {
      //          val submission = ttpSubmissionNLI
      //            .copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = BigDecimal("300.00"), dueDate = LocalDate.now()))))

      val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLI))

      implicit val result = requestWithCsrfToken(controller.submitPaymentToday(), "300.00")

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getJourney()
    }

    "Return BadRequest if the form value has more than 2 decimal places" in {
      //          val submission = ttpSubmissionNLI
      //            .copy(calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount  = 300.0, dueDate = LocalDate.now()))))

      val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLI))

      implicit val result = requestWithCsrfToken(controller.submitPaymentToday(), "299.999")

      status(result) mustBe BAD_REQUEST
      verify(mockSessionCache, times(1)).getJourney()
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {

      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      val result = controller.submitPaymentToday().apply(FakeRequest())
      status(result) mustBe SEE_OTHER
    }

    "Return the payment-today from for getPayTodayQuestion if there is an initial payment  already made" in {
      val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))

      //          when(mockSessionCache.getJourney())
      //            .thenReturn(Future.successful(ttpSubmissionNLIOver10k.copy(calculatorData = CalculatorInput.initial.copy(initialPayment = BigDecimal(2)))))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLI))
      val result = controller.getPayTodayQuestion().apply(fakeRequest)

      status(result) mustBe SEE_OTHER
    }

    "Return 303 for getPayTodayQuestion when TTPSubmission is missing" in {

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))

      val result = controller.getPayTodayQuestion().apply(FakeRequest())

      // status(result) mustBe SEE_OTHER
      //          ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 303 for submitPayTodayQuestion when there are no debits" in {

      val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                           Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                           Some(taxPayer),
                                           Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission.copy(ttpSubmission._id)))

      val result = controller.getPayTodayQuestion().apply(FakeRequest())

      status(result) mustBe SEE_OTHER
      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 200 for submitPayTodayQuestion if there are debits and valid eligibility answers" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIOver10k))

      val result = controller.getPayTodayQuestion().apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.payment_today_question.title"))
    }

    "successfully display payment summary page" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIOver10k))
      val response = controller.getPaymentSummary().apply(fakeRequest)

      status(response) mustBe OK

      contentAsString(response) must include(getMessages(fakeRequest)("ssttp.calculator.form.payment_summary.title"))
    }

    "successfully redirect to start page when trying to access what you owe review page if there are no debits" in {
      when(mockSessionCache.getJourney())
        //.thenReturn(Future.successful(ttpSubmissionNLIOver10k.copy(calculatorData = CalculatorInput.initial)))
        .thenReturn(Future.successful(ttpSubmissionNLIOver10k))
      val response = controller.getPaymentSummary().apply(FakeRequest())

      status(response) mustBe SEE_OTHER

      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "successfully redirect to start page when there are invalid eligibility questions" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      val response = controller.getPaymentSummary().apply(FakeRequest())

      status(response) mustBe SEE_OTHER

      ssttpeligibility.routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "submitSignIn should redirect with a good session " in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))

      //TODO this method doesn't exist anymore not sure what to replace with

      //val response = controller.submitSignIn().apply(FakeRequest())

      //status(response) mustBe SEE_OTHER
    }

    "getPaymentToday should redirect with a good session " in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      val response = controller.getPaymentToday().apply(FakeRequest())

      status(response) mustBe SEE_OTHER
    }

    "getPaymentPlanCalculator should load the Payment Plan Calculator Start" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))

      //TODO this method doesn't exist anymore not sure what to replace with
      //          val response = controller.getPaymentPlanCalculator().apply(request)
      //
      //          status(response) mustBe OK
      //          contentAsString(response) must include(getMessages(request)("ssttp.calculator.payment-plan-calculator.start.title"))
    }

    "getAmountDue should load the amount due page" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      val response = controller.getAmountDue().apply(fakeRequest)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(fakeRequest)("ssttp.calculator.amount-due.start.title"))
    }

    "submitAmountDue should load the getAmountDue Page with a 400" in {
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      //TODO this method doesn't exist anymore not sure what to replace with
      //          val response = controller.submitAmountDue().apply(fakeRequest)
      //
      //          status(response) mustBe BAD_REQUEST
      //          contentAsString(response) must include(getMessages(request)("ssttp.calculator.amount-due.start.title"))
    }

    "submitAmountDue should update the session with amount submitted" in {
      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmissionNLIEmpty))
      when(mockSessionCache.saveJourney(any())(any()))
      //TODO this method doesn't exist anymore not sure what to replace with
      //          val response = controller.submitAmountDue().apply(fakeRequest.withFormUrlEncodedBody("amount" -> "500"))
      //
      //          status(response) mustBe SEE_OTHER
      //          ssttpcalculator.routes.CalculatorController.getCalculateInstalmentsUnAuth().url must endWith(redirectLocation(response).get)
      //          verify(mockSessionCache, times(1)).saveJourney(any())(any(), any())
    }

    "getCalculateInstalmentsUnAuth should load the getCalculateInstalmentsUnAuth if amountDue is in the session" in {
      val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                           Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                           Some(taxPayer),
                                           Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))
      when(mockCalculatorService.getInstalmentsSchedule(any(), any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      //TODO this method doesn't exist anymore not sure what to replace with
      //          val response = controller.getCalculateInstalmentsUnAuth().apply(fakeRequest)
      //
      //          status(response) mustBe OK
      //          contentAsString(response) must include(getMessages(fakeRequest)("ssttp.calculator.results.title"))
    }

    "getCheckCalculation should load the check calculation page if amountDue is in the session and the chosen shcedule is there" in {
      val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                           Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                           Some(taxPayer),
                                           Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      //TODO this method doesn't exist anymore not sure what to replace with
      //          val response = controller.getCheckCalculation().apply(fakeRequest)
      //
      //          status(response) mustBe OK
      //          contentAsString(response) must include(getMessages(fakeRequest)("ssttp.calculator.check-calculation.h1"))
    }

    "submitCalculateInstalmentsUnAuth should return a bad request if the data is bad " in {
      val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                           Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                           Some(taxPayer),
                                           Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))
      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))

      //TODO this method doesn't exist anymore not sure what to replace with
      //          when(mockCalculatorService.getInstalmentsScheduleUnAuth(any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      //          when(mockSessionCache.saveJourney(any())(any())).thenReturn(Future.successful(mock[CacheMap]))
      //          val response = controller.submitCalculateInstalmentsUnAuth().apply(fakeRequest)
      //
      //          status(response) mustBe BAD_REQUEST
      //          contentAsString(response) must include(getMessages(fakeRequest)("ssttp.calculator.results.title"))
    }

    "submitCalculateInstalmentsUnAuth should redirect a bad request if the data is bad " in {
      val ttpSubmission: Journey = Journey(journeyId, Some(123: Int), Some(calculatorPaymentSchedule),
                                           Some(BankDetails(Some("012131"), Some("1234567890"), None, None, None, Some("0987654321"))), None,
                                           Some(taxPayer),
                                           Some(calculatorInput.copy(initialPayment = BigDecimal.valueOf(300))), 3: Int, Some(EligibilityStatus(true, Seq.empty)))

      when(mockSessionCache.getJourney())
        .thenReturn(Future.successful(ttpSubmission))
      //TODO this method doesn't exist anymore not sure what to replace with
      //          when(mockCalculatorService.getInstalmentsScheduleUnAuth(any())(any())).thenReturn(Future.successful(calculatorPaymentScheduleMap))
      //          when(mockSessionCache.saveJourney(any())(any())).thenReturn(Future.successful(mock[CacheMap]))
      //          val fakeRequest = FakeRequest().withFormUrlEncodedBody("chosen-month" -> "2")
      //          val response = controller.submitCalculateInstalmentsUnAuth().apply(fakeRequest)
      //
      //          status(response) mustBe SEE_OTHER
      //          ssttpcalculator.routes.CalculatorController.getCheckCalculation().url must endWith(redirectLocation(response).get)
    }

    "submitPayTodayQuestion should redirect with a good session and good request and to the getPaymentToday if true is selected" in {
      val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))
      when(mockSessionCache.getJourney()).thenReturn(
        Future.successful(
          ttpSubmissionNLI
        )
      )
      val response = controller.submitPayTodayQuestion().apply(FakeRequest()
        .withFormUrlEncodedBody("paytoday" -> "true"))

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getPaymentToday().url must endWith(redirectLocation(response).get)
    }

    "submitPayTodayQuestion should redirect with a good session and good request and to the getCalculateInstalments if true is selected" in {
      val ttpSubmissionNLI: Journey = new Journey(_id                 = journeyId, schedule = Some(calculatorPaymentSchedule), maybeCalculatorData = Some(calculatorInput.copy(debits = Seq(debitInput))))
      when(mockSessionCache.getJourney()).thenReturn(
        Future.successful(
          ttpSubmissionNLI
        )
      )
      when(mockSessionCache.saveJourney(any())(any()))
      val response = controller.submitPayTodayQuestion().apply(FakeRequest()
        .withFormUrlEncodedBody("paytoday" -> "false"))

      status(response) mustBe SEE_OTHER
      ssttpcalculator.routes.CalculatorController.getCalculateInstalments().url must endWith(redirectLocation(response).get)
    }
  }

  private def requestWithCsrfToken(action: Action[AnyContent], amount: String)(implicit app: Application) = {
    val csrfAddToken = app.injector.instanceOf[play.filters.csrf.CSRFAddToken]
    csrfAddToken(action).apply(FakeRequest()
      .withFormUrlEncodedBody("amount" -> amount))
  }
}
