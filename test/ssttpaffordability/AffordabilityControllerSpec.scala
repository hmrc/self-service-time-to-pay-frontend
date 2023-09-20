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

package ssttpaffordability

import akka.util.Timeout
import journey.{JourneyId, JourneyService}
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import testsupport.stubs.{AuditStub, AuthStub}
import testsupport.testdata.TdRequest.FakeRequestOps
import testsupport.{ItSpec, RichMatchers, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.libs.json.{JsObject, Json}
import testsupport.testdata.TestJourney

import java.util.UUID

class AffordabilityControllerSpec extends ItSpec with WireMockSupport {

  override val overrideConfig: Map[String, Any] = Map(
    "auditing.enabled" -> true
  )

  val requestTimeOut = 5
  implicit val timeout: Timeout = Timeout(requestTimeOut.seconds)

  "AffordabilityController" - {
    ".getSetUpPlanWithAdviser" - {
      "audits and returns 200 OK" in {
        AuditStub.audit()
        AuthStub.authorise()

        val journeyId = JourneyId("62ce7631b7602426d74f83b0")
        val sessionId = UUID.randomUUID().toString
        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

        val journey = TestJourney.createJourney(journeyId)
        val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
        journeyService.saveJourney(journey)(fakeRequest)

        val controller: AffordabilityController = app.injector.instanceOf[AffordabilityController]

        eventually(RichMatchers.timeout(Span(requestTimeOut, Seconds))) {
          val res = controller.getSetUpPlanWithAdviser(fakeRequest)

          status(res) shouldBe Status.OK

          AuditStub.verifyEventAudited(
            "ManualAffordabilityCheckFailed",
            Json.parse(
              s"""
                 |{
                 |  "totalDebt" : "4900",
                 |  "halfDisposableIncome" : "750",
                 |  "income" : {
                 |   "monthlyIncomeAfterTax" : "2,000",
                 |   "benefits" : "0",
                 |   "otherMonthlyIncome" : "0",
                 |   "totalIncome" : "2,000"
                 |  },
                 |  "outgoings" : {
                 |   "housing" : "500",
                 |   "pensionContributions" : "0",
                 |   "councilTax" : "0",
                 |   "utilities" : "0",
                 |   "debtRepayments" : "0",
                 |   "travel" : "0",
                 |   "childcareCosts" : "0",
                 |   "insurance" : "0",
                 |   "groceries" : "0",
                 |   "health" : "0",
                 |   "totalOutgoings" : "500"
                 |  },
                 |  "status" : "Interest greater than or equal to regular payment",
                 |  "utr" : "6573196998"
                 |}""".stripMargin
            ).as[JsObject]

          )
        }
      }
    }
  }

}
