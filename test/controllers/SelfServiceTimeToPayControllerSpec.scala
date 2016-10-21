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

package controllers

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Result
import play.api.test.{FakeApplication, FakeRequest, Helpers}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SelfServiceTimeToPayControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  "SelfServiceTimeToPayController" should {

    "return 200 Ok showing the service start page" in {
      val result: Future[Result] = SelfServiceTimeToPayController.present.apply(FakeRequest())
      val r: Result = await(result)
      status(r) shouldBe 200
    }

    "display the service start page title" in {
      val result: Future[Result] = SelfServiceTimeToPayController.present.apply(FakeRequest())
      val r: Result = await(result)
      bodyOf(r) should include ("Pay what you owe in instalments")
    }
  }

}
