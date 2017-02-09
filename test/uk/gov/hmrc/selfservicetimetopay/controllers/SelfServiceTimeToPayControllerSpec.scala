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
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.i18n.Messages
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class SelfServiceTimeToPayControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]

  "SelfServiceTimeToPayController" should {
    val controller = new SelfServiceTimeToPayController() {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    }

    "return 200 and display the service start page" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))

      val result:Result = controller.start.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.common.title"))
    }

    "redirect to the eligibility section if user selects start" in {
      val result:Result = controller.submit.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe controllers.routes.EligibilityController.start().url
    }

    "return 200 and display call us page successfully" in {
      val result:Result = controller.getTtpCallUs.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.call-us.title"))
    }

    "return 200 and display you need to file page successfully" in {
      val result: Result = controller.getYouNeedToFile.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.you-need-to-file.title"))
    }
    "successfully display the sign in option page" in {
       when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
            val response: Result = controller.getSignInQuestion.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

              status(response) shouldBe OK

              bodyOf(response) should include(Messages("ssttp.core.form.sign_in_question.title"))
          }
  }

}
