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

package util.util
import controllers.action.CheckSessionAction
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.selfservicetimetopay.resources.goodSession
class CheckSessionActionSpec extends PlaySpec with WithFakeApplication with ScalaFutures {

  "CheckSessionAction filter" should {
    "return None if the session is ok " in {
      val result = CheckSessionAction.filter(FakeRequest().withSession(goodSession: _*)).futureValue
      result.isEmpty mustBe true
    }

    "return a redirect if the session is not there and redirect to the start " in {
      val result = CheckSessionAction.filter(FakeRequest()).futureValue
      result.get.header.status mustBe SEE_OTHER
      result.get.header.headers("location") mustBe ssttpeligibility.routes.SelfServiceTimeToPayController.start().url
    }
    "return a redirect if the session is not there and redirect to the start of the unauth journey if url contains payment-plan-calculator " in {
      val result = CheckSessionAction.filter(FakeRequest("GET", "/payment-plan-calculator", FakeHeaders(), AnyContentAsEmpty)).futureValue
      result.get.header.status mustBe SEE_OTHER
      result.get.header.headers("location") mustBe ssttpcalculator.routes.CalculatorController.getPaymentPlanCalculator().url
    }
  }
}
