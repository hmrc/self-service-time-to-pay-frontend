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
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.CredentialStrength.Strong
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class EligibilityControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with ScalaFutures {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockAuthConnector: AuthConnector= mock[AuthConnector]

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

  "EligibilityController" should {
    val controller = new EligibilityController(mockAuthConnector) {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    }

    "redirect successfully to the type of tax page" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val response: Result = controller.start.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.getTypeOfTax().url
    }

    "successfully display the type of tax page" in {
      val response: Result = controller.getTypeOfTax.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.type_of_tax.title"))
    }

    "successfully display the existing ttp page" in {
      val response = await(controller.getExistingTtp.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie())))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.existing_ttp.title"))
    }

    "submit type of tax given valid data and redirect to existing ttp page" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie()).withFormUrlEncodedBody(typeOfTaxForm: _*)

      val response: Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.getExistingTtp().url
    }

    "submit type of tax given both types of tax and redirect to call us page" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie()).withFormUrlEncodedBody(bothTypeOfTaxForm: _*)

      val response: Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "submit type of tax given other types of tax and redirect to call us page" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie()).withFormUrlEncodedBody(otherTaxForm: _*)

      val response: Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "submit existing ttp given no existing ttp data and logged in user and redirect to amount you owe page via misalignment page" in {
      when(mockAuthConnector.currentAuthority(Matchers.any()))
        .thenReturn(Some(Authority("", Accounts(), None, None, Strong, ConfidenceLevel.L200, None, None, None)))

      val request = FakeRequest().withFormUrlEncodedBody(falseExistingTtpForm: _*).withCookies(sessionProvider.createTtpCookie())

      val response: Future[Result] = await(controller.submitExistingTtp.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe
        uk.gov.hmrc.selfservicetimetopay.controllers.routes.ArrangementController.determineMisalignment().url
    }

    "submit existing ttp given existing ttp data and redirect to call us page" in {
      val request = FakeRequest().withFormUrlEncodedBody(trueExistingTtpForm: _*).withCookies(sessionProvider.createTtpCookie())

      val response: Future[Result] = await(controller.submitExistingTtp.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.getTtpCallUs().url
    }

    "redirect to self on type of tax page and display errors when invalid data is submitted" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie())

      val response: Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe BAD_REQUEST
    }

    "redirect to self on existing ttp page and display errors when invalid data is submitted" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie())

      val response: Future[Result] = await(controller.submitExistingTtp.apply(request))

      status(response) shouldBe BAD_REQUEST
    }
  }

}
