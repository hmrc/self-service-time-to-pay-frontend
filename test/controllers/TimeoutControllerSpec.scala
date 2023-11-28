/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testsupport.{ItSpec, WireMockSupport}

import java.net.URLEncoder.encode

class TimeoutControllerSpec extends ItSpec {
  val controller: TimeoutController = app.injector.instanceOf[TimeoutController]

  "TimeoutController" - {
    ".killSession" - {
      "should display 'delete-answers' page with sign in again url to TimeoutController.signInAgain" in {
        val result = controller.killSession(FakeRequest())

        status(result) shouldBe Status.OK
      }
    }
    ".signInAgain" - {
      "should redirect to GG login" in {
        val result = controller.signInAgain(FakeRequest())
        val loginUrl = WireMockSupport.baseUrl.value + "/gg/sign-in"
        val contineUrlBase = encode(s"http://localhost:$testPort/", "UTF-8")
        val continueUrlEndPoint = ssttpeligibility.routes.SelfServiceTimeToPayController.start.url.stripPrefix("/")
        val originArg = "pay-online"

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(
          loginUrl + "?continue=" + contineUrlBase + continueUrlEndPoint + "&origin=" + originArg
        )
      }
    }
  }

}
