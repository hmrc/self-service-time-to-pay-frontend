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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength.Strong
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class EligibilityControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with ScalaFutures with BeforeAndAfterEach {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val typeOfTaxForm = Seq(
    "type_of_tax.hasSelfAssessmentDebt" -> "true",
    "type_of_tax.hasOtherDebt" -> "false"
  )

  val bothTypeOfTaxForm = Seq(
    "type_of_tax.hasSelfAssessmentDebt" -> "true",
    "type_of_tax.hasOtherDebt" -> "true"
  )

  val otherTaxForm = Seq(
    "type_of_tax.hasSelfAssessmentDebt" -> "false",
    "type_of_tax.hasOtherDebt" -> "true"
  )

  val falseExistingTtpForm = Seq(
    "hasExistingTTP" -> "false"
  )

  val trueExistingTtpForm = Seq(
    "hasExistingTTP" -> "true"
  )

  val mockEligibilityController = new EligibilityController() {
    override lazy val sessionCache = mockSessionCache
    override lazy val authConnector = mockAuthConnector
  }

  override def beforeEach() {
    reset(mockAuthConnector, mockSessionCache)
  }

  "EligibilityController" should {
    "redirect successfully to the type of tax page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val response: Result = mockEligibilityController.start.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.getTypeOfTax().url
    }

    "successfully display the type of tax page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(mockEligibilityController.getTypeOfTax
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe OK
      bodyOf(result) should include(Messages("ssttp.eligibility.form.type_of_tax.title"))
    }

    "successfully display the existing ttp page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.getExistingTtp
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe OK
      bodyOf(result) should include(Messages("ssttp.eligibility.form.existing_ttp.title"))
    }

    "successfully submit type of tax page with valid form data" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))
      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(mockEligibilityController.submitTypeOfTax()
        .apply(FakeRequest().withFormUrlEncodedBody("type_of_tax.hasSelfAssessmentDebt" -> "true", "type_of_tax.hasOtherDebt" -> "false")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.EligibilityController.getExistingTtp().url
    }

    "submit type of tax given both types of tax and redirect to call us page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(mockEligibilityController.submitTypeOfTax
        .apply(FakeRequest().withFormUrlEncodedBody(bothTypeOfTaxForm: _*)
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "submit type of tax given other types of tax and redirect to call us page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(mockEligibilityController.submitTypeOfTax
        .apply(FakeRequest().withFormUrlEncodedBody(otherTaxForm: _*)
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "submit existing ttp given no existing ttp data and logged in user and redirect to amount you owe page via misalignment page" in {
      implicit val hc = new HeaderCarrier

      when(mockAuthConnector.currentAuthority(Matchers.any()))
        .thenReturn(Some(Authority("", Accounts(), None, None, Strong, ConfidenceLevel.L200, None, None, None)))
      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(mockEligibilityController.submitExistingTtp
        .apply(FakeRequest().withFormUrlEncodedBody(falseExistingTtpForm: _*)
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe
        uk.gov.hmrc.selfservicetimetopay.controllers.routes.ArrangementController.determineMisalignment().url
    }

    "submit existing ttp given existing ttp data and redirect to call us page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))
      when(mockSessionCache.put(any())(any(), any()))
        .thenReturn(mock[CacheMap])

      val result = await(mockEligibilityController.submitExistingTtp
        .apply(FakeRequest().withFormUrlEncodedBody(trueExistingTtpForm: _*)
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "redirect to self on type of tax page and display errors when invalid data is submitted" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.submitTypeOfTax
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to self on existing ttp page and display errors when invalid data is submitted" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.submitExistingTtp
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe BAD_REQUEST
    }

    "Successfully display the sign in question page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.getSignInQuestion
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      bodyOf(result) should include(Messages("ssttp.eligibility.form.sign_in_question.title"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully redirect to the start page when missing submission data for the sign in question page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNLIEmpty)))

      val result = await(mockEligibilityController.getSignInQuestion
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.SelfServiceTimeToPayController.start().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Successfully submit the sign in question page with valid form data" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.submitSignInQuestion
        .apply(FakeRequest()
        .withFormUrlEncodedBody("signInOption.signIn" -> "false", "signInOption.enterInManually" -> "true")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).get shouldBe routes.CalculatorController.getDebitDate().url
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit no options selected in form data and return errors for the sign in question page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.submitSignInQuestion
        .apply(FakeRequest()
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      bodyOf(result) should include(Messages("ssttp.eligibility.form.sign_in_question.required"))
      verify(mockSessionCache, times(1)).get(any(), any())
    }

    "Submit both options selected in form data and return errors for the sign in question page" in {
      implicit val hc = new HeaderCarrier

      when(mockSessionCache.get(any(), any()))
        .thenReturn(Future.successful(Some(ttpSubmissionNoAmounts)))

      val result = await(mockEligibilityController.submitSignInQuestion
        .apply(FakeRequest()
        .withFormUrlEncodedBody("signInOption.signIn" -> "true", "signInOption.enterInManually" -> "true")
        .withCookies(sessionProvider.createTtpCookie())))

      status(result) shouldBe Status.BAD_REQUEST
      verify(mockSessionCache, times(1)).get(any(), any())
    }

  }

}
