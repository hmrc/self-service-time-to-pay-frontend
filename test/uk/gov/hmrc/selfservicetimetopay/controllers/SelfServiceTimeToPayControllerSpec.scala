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

import org.scalatest.mock.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.i18n.Messages
import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay._

class SelfServiceTimeToPayControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  private val gaToken = "GA-TOKEN"
  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map("google-analytics.token" -> gaToken))

  "SelfServiceTimeToPayController" should {

    "return 200 and display the service start page" in {
      val result:Result = SelfServiceTimeToPayController.present.apply(FakeRequest())

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.common.title"))
    }

    "redirect to the eligibility section if user selects start" in {
      val result:Result = SelfServiceTimeToPayController.submit.apply(FakeRequest())

      status(result) shouldBe SEE_OTHER

      redirectLocation(result).get shouldBe controllers.routes.EligibilityController.present().url
    }

    "return 200 and display call us page successfully" in {
      val result:Result = SelfServiceTimeToPayController.ttpCallUsPresent.apply(FakeRequest())

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.ttp-call-us.title"))
    }

    "return 200 and display you need to file page successfully" in {
      val result: Result = SelfServiceTimeToPayController.youNeedToFilePresent.apply(FakeRequest())

      status(result) shouldBe OK

      bodyOf(result) should include(Messages("ssttp.you-need-to-file.title"))
    }
  }

}
