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

package ssttparrangement

import akka.util.Timeout
import journey.{JourneyId, JourneyService}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import testsupport.RichMatchers.eventually
import testsupport.{RichMatchers, WireMockSupport}
import testsupport.stubs.{ArrangementStub, AuthStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.{TdRequest, TestJourney}
import uk.gov.hmrc.http.SessionKeys
import java.util.UUID

class ArrangementControllerSpec extends PlaySpec with GuiceOneAppPerTest with WireMockSupport {
  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  val testPort: Int = 19001

  protected lazy val configMap: Map[String, Any] = Map(
    "microservice.services.direct-debit.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port,
    "microservice.services.campaign-manager.port" -> WireMockSupport.port,
    "microservice.services.ia.port" -> WireMockSupport.port,
    "microservice.services.auth.port" -> WireMockSupport.port,
    "microservice.services.company-auth.url" -> s"http://localhost:${WireMockSupport.port}",
    "microservice.services.auth.login-callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.add-taxes.port" -> WireMockSupport.port,
    "microservice.services.bars.port" -> WireMockSupport.port,
    "microservice.services.identity-verification-frontend.uplift-url" -> s"http://localhost:${WireMockSupport.port}/mdtp/uplift",
    "microservice.services.identity-verification-frontend.callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.identity-verification-frontend.callback.complete-path" -> "/pay-what-you-owe-in-instalments/arrangement/determine-eligibility",
    "microservice.services.identity-verification-frontend.callback.reject-path" -> "/pay-what-you-owe-in-instalments/eligibility/not-enrolled")

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(configMap)
    .build()

  "ArrangementController" should {
    "with a normal single event" in {
      AuthStub.authorise()

      DirectDebitStub.postPaymentPlan
      ArrangementStub.postTtpArrangement
      TaxpayerStub.getTaxpayer()

      val journeyId = JourneyId("62ce7631b7602426d74f83b0")
      val sessionId = UUID.randomUUID().toString
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

      val journey = TestJourney.createJourney(journeyId)
      val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
      journeyService.saveJourney(journey)(fakeRequest)

      val controller: ArrangementController = app.injector.instanceOf[ArrangementController]

      eventually(RichMatchers.timeout(Span(5, Seconds))) {
        val res = controller.submit()(fakeRequest)
        status(res) mustBe Status.SEE_OTHER
        res.value.get.get.header.headers("Location") mustBe "/pay-what-you-owe-in-instalments/arrangement/summary"
      }
    }
  }

}
