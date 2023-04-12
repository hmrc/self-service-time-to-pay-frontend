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

import testsupport.ItSpec
import org.bson.types.ObjectId
import play.api.libs.json.Json

import scala.concurrent.Future
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import testsupport.testdata.TestJourney
import java.time.LocalDateTime

class JourneyServiceSpec extends ItSpec {

  "when updating the mongodb Journey structure" - {

    "the createdTTL field should be added" in {
      val journeyId = JourneyId(ObjectId.get.toHexString)
      val fakeRequest = FakeRequest().withSession("ssttp.journeyId" -> journeyId.toHexString)
      val journey = TestJourney.createJourney(journeyId)
      val journeyService: JourneyService = app.injector.instanceOf[JourneyService]

      ScalaFutures.whenReady(insertJournetWithoutTTL(journey)) { _ =>
        ScalaFutures.whenReady(journeyService.getJourney()(fakeRequest)) { journeyResult =>
          val now = LocalDateTime.now
          journeyResult._id shouldBe journey._id
          journeyResult.createdTTL.getYear shouldBe now.getYear
          journeyResult.createdTTL.getMonth shouldBe now.getMonth
          journeyResult.createdTTL.getDayOfMonth shouldBe now.getDayOfMonth
        }
      }
    }
  }

  def insertJournetWithoutTTL(journey: Journey): Future[Option[InsertOneResult]] = {
    val journeyRepo: JourneyRepo = app.injector.instanceOf[JourneyRepo]

    val docCollection = journeyRepo.collection.withDocumentClass[Document]
    val jsonS = Json.toJson(journey).toString.replaceAll(""","createdTTL":\{"\$date":\{"\$numberLong":"\d+"}}""", "")

    docCollection.insertOne(Document(jsonS)).headOption()
  }
}
