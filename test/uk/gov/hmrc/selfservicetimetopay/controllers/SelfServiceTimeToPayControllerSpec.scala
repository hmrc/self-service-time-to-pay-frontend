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

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import ssttpeligibility.SelfServiceTimeToPayController
import journey.JourneyService
import uk.gov.hmrc.selfservicetimetopay.resources._
import _root_.controllers.action._
import config.AppConfig
import play.api.mvc.MessagesControllerComponents
import req.RequestSupport
import testsupport.testdata.TdAll
import views.Views

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SelfServiceTimeToPayControllerSpec extends PlayMessagesSpec with MockitoSugar {

  implicit val appConfig: AppConfig = mock[AppConfig]
  implicit val request = TdAll.request

  val mockSessionCache: JourneyService = mock[JourneyService]
  val mockMessagesControllerComponents: MessagesControllerComponents = mock[MessagesControllerComponents]
  val mockActions: Actions = mock[Actions]
  val mockViews: Views = mock[Views]
  val mockRequestSupport: RequestSupport = mock[RequestSupport]
  val fakeRequest = FakeRequest().withSession(
    ("_*", "_*")
  )

  "SelfServiceTimeToPayController" should {

    val controller = new SelfServiceTimeToPayController(
      mcc               = mockMessagesControllerComponents,
      submissionService = mockSessionCache,
      as                = mockActions,
      views             = mockViews,
      requestSupport    = mockRequestSupport
    )

    "return 200 and display the service start page" in {
      when(mockSessionCache.getJourney()).thenReturn(Future.successful(ttpSubmission))

      val result = controller.start.apply(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(fakeRequest)("ssttp.common.title"))
    }

    "submit redirect to the determine Eligibility" in {
      val result = controller.submit.apply(FakeRequest().withSession(
        ("_*", "_*")
      ))

      status(result) mustBe SEE_OTHER

      ssttparrangement.routes.ArrangementController.determineEligibility().url must endWith(redirectLocation(result).get)
    }

    "return 200 and display call us page successfully" in {

      val result = controller.getTtpCallUs.apply(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(fakeRequest)("ssttp.call-us.title"))
    }

    "return 200 and display you need to file page successfully" in {

      val result = controller.getYouNeedToFile.apply(fakeRequest)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(fakeRequest)("ssttp.you-need-to-file.check-account"))
    }
  }
}
