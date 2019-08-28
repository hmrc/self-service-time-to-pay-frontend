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

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import ssttpeligibility.SelfServiceTimeToPayController
import sttpsubmission.SubmissionService
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.resources._
import _root_.controllers.action._
import config.AppConfig
import play.api.mvc.MessagesControllerComponents
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SelfServiceTimeToPayControllerSpec extends PlayMessagesSpec with MockitoSugar {

  val mockSessionCache: SubmissionService = mock[SubmissionService]
  val mcc: MessagesControllerComponents = mock
  implicit val appConfig: AppConfig = mock[AppConfig]

  "SelfServiceTimeToPayController" should {

    val controller = new SelfServiceTimeToPayController(
      mcc               = mcc,
      submissionService = mockSessionCache,
      as                = mock[Actions]
    )

    "return 200 and display the service start page" in {
      when(mockSessionCache.getTtpSubmission(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      val request = FakeRequest().withSession(goodSession: _*)
      val result = controller.start.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.common.title"))
    }

    "submit redirect to the determine Eligibility" in {
      val result = controller.submit.apply(FakeRequest().withSession(goodSession: _*))

      status(result) mustBe SEE_OTHER

      ssttparrangement.routes.ArrangementController.determineEligibility().url must endWith(redirectLocation(result).get)
    }

    "return 200 and display call us page successfully" in {
      val request = FakeRequest().withSession(goodSession: _*)
      val result = controller.getTtpCallUs.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.call-us.title"))
    }

    "return 200 and display you need to file page successfully" in {
      val request = FakeRequest().withSession(goodSession: _*)
      val result = controller.getYouNeedToFile.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.you-need-to-file.check-account"))
    }
  }
}
