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

import akka.actor.ActorSystem
import akka.stream._
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class CalculatorControllerSpec extends PlayMessagesSpec with MockitoSugar with BeforeAndAfterEach {

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]

  val controller = new CalculatorController(messagesApi, mockCalculatorConnector) {
    override lazy val sessionCache = mockSessionCache
    override lazy val authConnector = mockAuthConnector
  }

  implicit val system = ActorSystem("QuickStart")
  implicit val mat: akka.stream.Materializer = ActorMaterializer()


  override def beforeEach() {
    reset(mockAuthConnector, mockSessionCache, mockCalculatorConnector)
  }

  "CalculatorControllerSpec" should {
    "Return OK for non-logged-in calculation submission with valid start date" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI.copy(schedule = Some(calculatorPaymentSchedule.copy(startDate = Some(LocalDate.now)))))))

      val result = controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.OK
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Update the schedule in the cache if the startDate is out of date" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
      verify(mockCalculatorConnector, times(1)).calculatePaymentSchedule(any())(any())
      verify(mockSessionCache, times(1)).put(any())(any(), any())
    }

    "if no schedule is present, generate a schedule and populate it in the session" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLI.copy(schedule = None))))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.CalculatorController.getCalculateInstalments(None).url must endWith(redirectLocation(result).get)
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax = Some(EligibilityTypeOfTax(true, false)),
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "300.00")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return BadRequest if the form value = total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax =
        Some(EligibilityTypeOfTax(true, false)),
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      val result = controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.999")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 if the form value < total amount due (via rounding)" in {
      implicit val hc = new HeaderCarrier

      val submission = ttpSubmissionNLI.copy(eligibilityTypeOfTax =
        Some(EligibilityTypeOfTax(true, false)),
        eligibilityExistingTtp = Some(EligibilityExistingTTP(Some(false))),
        calculatorData = ttpSubmissionNLI.calculatorData.copy(debits = Seq(Debit(amount = 300.0, dueDate = LocalDate.now()))))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(submission)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.submitPaymentToday().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "299.994")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 for non-logged-in when schedule is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLINoSchedule)))

      when(mockCalculatorConnector.calculatePaymentSchedule(any())(any()))
        .thenReturn(Future.successful(Seq(calculatorPaymentSchedule)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.getCalculateInstalments(Some(3)).apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.CalculatorController.getCalculateInstalments(None).url must endWith(redirectLocation(result).get)
    }

    "Return 303 for non-logged-in when TTPSubmission is missing for submitPaymentToday" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = controller.submitPaymentToday().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Return 303 for submitPayTodayQuestion when TTPSubmission is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 303 for submitPayTodayQuestion when eligibilityExistingTtp is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(eligibilityExistingTtp = None))))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 303 for submitPayTodayQuestion when eligibilityTypeOfTax is missing" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(eligibilityTypeOfTax = None))))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 303 for submitPayTodayQuestion when there are no debits" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmission.copy(calculatorData = CalculatorInput.initial.copy(debits = Seq.empty)))))

      val result = controller.getPayTodayQuestion().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
    }

    "Return 200 for submitPayTodayQuestion if there are debits and valid eligibility answers" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))
      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.getPayTodayQuestion().apply(request)

      status(result) mustBe Status.OK
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.payment_today_question.title"))
    }

    "Successfully display the what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.getDebitDate().apply(request)

      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-date.example"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully redirect to the start page when missing submission data for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = controller.getDebitDate().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully submit the what you owe date page with valid form data" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(Future.successful(mock[CacheMap]))

      val result = controller.submitDebitDate().apply(FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.CalculatorController.getAmountOwed().url must endWith(redirectLocation(result).get)
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid year lower than minimum value in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "1900",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitDebitDate().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-year-too-low"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid year greater than maximum value in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2111",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitDebitDate().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-year-too-high"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid month in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "13",
          "dueBy.dueByDay" -> "31")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitDebitDate().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-month-number"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit invalid day in form data and return errors for what you owe date page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("dueBy.dueByYear" -> "2017",
          "dueBy.dueByMonth" -> "1",
          "dueBy.dueByDay" -> "32")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitDebitDate().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-date.due_by.not-valid-day-number"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully display the what you owe amount page" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))
      val request = FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.getAmountOwed().apply(request)

      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-amount.title1"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully redirect to the start page when missing submission data for what you owe amount page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = controller.getAmountOwed().apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(result).get)
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully submit the what you owe amount page with valid form data" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))

      val result = controller.submitAmountOwed().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "5000")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.SEE_OTHER
      routes.CalculatorController.getWhatYouOweReview().url must endWith(redirectLocation(result).get)
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit amount less than 0 in form data for the what you owe amount page and return errors" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("amount" -> "-500")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitAmountOwed().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-amount.amount.required"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }


    "Submit amount greater than or equal to 1000000000000 in form data for the what you owe amount page and return errors" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("amount" -> "1000000000000")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitAmountOwed().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-amount.amount.less-than-maxval"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit empty amount in form data for the what you owe amount page and return errors" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))
      val request = FakeRequest()
        .withFormUrlEncodedBody("amount" -> "")
        .withCookies(sessionProvider.createTtpCookie())
      val result = controller.submitAmountOwed().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(getMessages(request)("ssttp.calculator.form.what-you-owe-amount.amount.required"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "the what you owe amount page must format the date correctly on a bad subbmission in" in {
      implicit val hc = new HeaderCarrier

      val requiredSubmission = ttpSubmissionNoAmounts.copy(debitDate = Some(LocalDate.parse("2017-01-31")))
      val formattedDateString =  "31 January 2017?"
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(requiredSubmission)))
      val result = controller.submitAmountOwed().apply(FakeRequest()
        .withFormUrlEncodedBody("amount" -> "-500")
        .withCookies(sessionProvider.createTtpCookie()))

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include(formattedDateString)
    }

    "successfully display the what you owe review page" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k)))
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie())
      val response = controller.getWhatYouOweReview().apply(request)

      status(response) mustBe OK

      contentAsString(response) must include(getMessages(request)("ssttp.calculator.form.entered_all_amounts_question.title"))
    }

    "successfully redirect to start page when trying to access what you owe review page if there are no debits" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIOver10k.copy(calculatorData = CalculatorInput.initial))))
      val response = controller.getWhatYouOweReview().apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) mustBe Status.SEE_OTHER

      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }

    "successfully redirect  load page with invalid eligibility questions must go to start" in {
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      val response = controller.getWhatYouOweReview().apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) mustBe Status.SEE_OTHER

      routes.SelfServiceTimeToPayController.start().url must endWith(redirectLocation(response).get)
    }
  }
}
