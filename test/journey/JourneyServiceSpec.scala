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

package journey

import journey.JourneyService.{JourneyNotFound, NoJourneyIdForSessionFound}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.http.{HttpEntity, Status => HttpStatus}
import play.api.mvc.{AnyContentAsEmpty, ResponseHeader, Result}
import testsupport.ItSpec
import testsupport.testdata.TestJourney
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import scala.concurrent.Future

class JourneyServiceSpec extends ItSpec {

  val service: JourneyService = app.injector.instanceOf[JourneyService]

  "JourneyService" - {
    ".getJourney()" - {
      "when there is no journey id in the request's session" - {
        "returns failed future" in {
          val sessionId = UUID.randomUUID().toString
          val fakeRequest = FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
          val caught = intercept[Exception] {
            service.getJourney()(fakeRequest).futureValue
          }
          val cause = caught.getCause

          cause.getClass shouldBe classOf[NoJourneyIdForSessionFound]
          cause.getMessage shouldBe s"No journey Id found in session [Session Id: $sessionId]"
        }
      }
      "when no journey is found for the journey id in the request's session" - {
        "returns failed future" in {
          val journeyIdString = "62ce7631b7602426d74f83b0"
          val fakeRequest = FakeRequest().withSession("ssttp.journeyId" -> journeyIdString)

          val caught = intercept[Exception] {
            service.getJourney()(fakeRequest).futureValue
          }
          val cause = caught.getCause

          cause.getClass shouldBe classOf[JourneyNotFound]
          cause.getMessage shouldBe s"Journey for journey Id in session not found [Journey Id: $journeyIdString]"
        }
      }
      "when a journey is found for the journey id in the request's session" - {
        "returns successful future with the journey" in {
          val journeyIdString = "62ce7631b7602426d74f83b0"
          val journey = TestJourney.createJourney(JourneyId(journeyIdString))
          val fakeRequest = FakeRequest().withSession("ssttp.journeyId" -> journeyIdString)

          for {
            _ <- service.saveJourney(journey)(fakeRequest)
            fetchedJourney <- service.getJourney()(fakeRequest)
          } yield fetchedJourney shouldBe journey
        }
      }
    }
    ".authorizedForSsttp(block)" - {
      "redirects to /delete-answers if no journey id in the session is found" in new DummyAction {
        val result = service.authorizedForSsttp(dummyBlock)(fakeRequest)

        status(result) shouldBe HttpStatus.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TimeoutController.killSession.url)
      }
      "redirects to /delete-answers if no journey is found for the session's journey id" in new DummyAction {
        val unusedJourneyId = "51ba6742c7602426d74f84c0"
        val result = service.authorizedForSsttp(dummyBlock)(fakeRequest.withSession("ssttp.journeyId" -> unusedJourneyId))

        status(result) shouldBe HttpStatus.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TimeoutController.killSession.url)
      }

    }
  }

  class DummyAction {
    val dummyBlock: Journey => Future[Result] = (j: Journey) => Future(Result.apply(
      ResponseHeader(HttpStatus.OK),
      HttpEntity.NoEntity
    ))

    val fakeRequest: FakeRequest[Any] = FakeRequest()
  }

}
