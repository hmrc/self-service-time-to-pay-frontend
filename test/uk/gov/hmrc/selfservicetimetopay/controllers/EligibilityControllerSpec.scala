/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class EligibilityControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with ScalaFutures {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]

  val typeOfTaxForm = Seq(
    "type_of_tax.hasSelfAssessmentDebt" -> "true",
    "type_of_tax.hasOtherDebt" -> "false"
  )

  val existingTtpForm = Seq(
    "hasExistingTTP" -> "false"
  )

  "EligibilityController" should {
    val controller = new EligibilityController(){
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    }

    "redirect successfully to the type of tax page" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val response:Result = controller.start.apply(FakeRequest().withSession(sessionProvider.createSessionId()))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.getTypeOfTax().url
    }

    "successfully display the type of tax page" in {
      val response:Result = controller.getTypeOfTax.apply(FakeRequest().withSession(sessionProvider.createSessionId()))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.type_of_tax.title"))
    }

    "successfully display the existing ttp page" in {
      val response = await(controller.getExistingTtp.apply(FakeRequest().withSession(sessionProvider.createSessionId())))

      status(response) shouldBe OK

      bodyOf(response) should include(Messages("ssttp.eligibility.form.existing_ttp.title"))
    }

    "submit type of tax given valid data and redirect to existing ttp page" in {
      val request = FakeRequest().withSession(sessionProvider.createSessionId()).withFormUrlEncodedBody(typeOfTaxForm:_*)

      val response:Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe controllers.routes.EligibilityController.getExistingTtp().url
    }

    "submit existing ttp given valid data and redirect to amount you owe page" in {
      val request = FakeRequest().withFormUrlEncodedBody(existingTtpForm:_*).withSession(sessionProvider.createSessionId())

      val response:Future[Result] = await(controller.submitExistingTtp.apply(request))

      status(response) shouldBe SEE_OTHER

      redirectLocation(response).get shouldBe
        uk.gov.hmrc.selfservicetimetopay.controllers.calculator.routes.AmountsDueController.start().url
    }

    "redirect to self on type of tax page and display errors when invalid data is submitted" in {
      val request = FakeRequest().withSession(sessionProvider.createSessionId())

      val response:Future[Result] = await(controller.submitTypeOfTax.apply(request))

      status(response) shouldBe BAD_REQUEST
    }

    "redirect to self on existing ttp page and display errors when invalid data is submitted" in {
      val request = FakeRequest().withSession(sessionProvider.createSessionId())

      val response:Future[Result] = await(controller.submitExistingTtp.apply(request))

      status(response) shouldBe BAD_REQUEST
    }
  }

}
