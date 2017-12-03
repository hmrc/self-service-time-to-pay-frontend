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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay._
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.resources._
import uk.gov.hmrc.selfservicetimetopay.util.TTPSessionId

import scala.concurrent.Future

class SelfServiceTimeToPayControllerSpec extends PlayMessagesSpec with MockitoSugar  {
  
  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]

  "SelfServiceTimeToPayController" should {
    val controller = new SelfServiceTimeToPayController(messagesApi) {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    }

    "return 200 and display the service start page" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      val request = FakeRequest().withSession(SessionKeys.userId -> "someUserId", TTPSessionId.newTTPSession())
      val result = controller.start.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.common.title"))
    }

    "redirect to the eligibility section if user selects start" in {
      val result = controller.submit.apply(FakeRequest().withSession(SessionKeys.userId -> "someUserId", TTPSessionId.newTTPSession()))

      status(result) mustBe SEE_OTHER

      controllers.routes.EligibilityController.start().url must endWith(redirectLocation(result).get)
    }

    "return 200 and display call us page successfully" in {
      val request = FakeRequest().withSession(SessionKeys.userId -> "someUserId", TTPSessionId.newTTPSession())
      val result = controller.getTtpCallUs.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.call-us.title"))
    }

    "return 200 and display you need to file page successfully" in {
      val request = FakeRequest().withSession(SessionKeys.userId -> "someUserId", TTPSessionId.newTTPSession())
      val result = controller.getYouNeedToFile.apply(request)

      status(result) mustBe OK

      contentAsString(result) must include(getMessages(request)("ssttp.you-need-to-file.check-account"))
    }
  }
}