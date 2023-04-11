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

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Logger
import repo.Repo
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object JourneyRepo {
  def indexes(cacheTtlInSeconds: Long): Seq[IndexModel] = Seq(
    IndexModel(
      keys         = Indexes.ascending("createdTTL"),
      indexOptions = IndexOptions().name("createdTTLIdx").expireAfter(cacheTtlInSeconds, TimeUnit.SECONDS)
    )
  )
}

@Singleton
final class JourneyRepo @Inject() (
    mongoComponent: MongoComponent,
    config:         ServicesConfig)(implicit ec: ExecutionContext)
  extends Repo[JourneyId, Journey](
    collectionName = "journey-new-mongo",
    mongoComponent = mongoComponent,
    indexes        = {
      val logger = Logger(getClass)
      val ttl = config.getDuration("journey.ttl").toSeconds
      logger.info(s"Mongo collection 'journey-new-mongo' ttl=$ttl seconds")
      JourneyRepo.indexes(ttl)

    },
    replaceIndexes = true
  )
