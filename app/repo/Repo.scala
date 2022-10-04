/*
 * Copyright 2022 HM Revenue & Customs
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

package repo

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.{Filters, IndexModel, ReplaceOptions}
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.{ExecutionContext, Future}

trait Id {
  def value: ObjectId

  def toHexString: String = value.toHexString
}

trait HasId[ID <: Id] {
  def _id: ID

  def id: ID = _id
}

abstract class Repo[ID <: Id, A <: HasId[ID]](
    collectionName: String,
    mongoComponent: MongoComponent,
    indexes:        Seq[IndexModel],
    replaceIndexes: Boolean         = false
)(implicit manifest: Manifest[A],
  mid:              Manifest[ID],
  domainFormat:     OFormat[A],
  idFormat:         Format[ID],
  executionContext: ExecutionContext)
  extends PlayMongoRepository[A](
    mongoComponent = mongoComponent,
    collectionName = collectionName,
    domainFormat   = domainFormat,
    indexes        = indexes,
    replaceIndexes = replaceIndexes) {

  /**
   * Update or Insert (UpSert)
   */
  def upsert(a: A): Future[Unit] = collection
    .replaceOne(
      filter      = Filters.eq("_id", a.id.value),
      replacement = a,
      options     = ReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => ())

  def findById(id: ID): Future[Option[A]] = collection
    .find(
      filter = Filters.eq("_id", id.value)
    )
    .headOption()

}

