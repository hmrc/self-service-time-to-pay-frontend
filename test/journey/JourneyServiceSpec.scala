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
import testsupport.ItSpec
import testsupport.testdata.TestJourney
import uk.gov.hmrc.http.SessionKeys
import scala.concurrent.ExecutionContext.Implicits.global

import java.util.UUID

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
  }

}
