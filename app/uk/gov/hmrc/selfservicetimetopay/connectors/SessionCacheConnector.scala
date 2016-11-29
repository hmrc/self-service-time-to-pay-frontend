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

package uk.gov.hmrc.selfservicetimetopay.connectors

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

trait SessionCacheConnector extends SessionCache with ServicesConfig {
  val sessionKey: String

  def put(body: TTPSubmission)(implicit writes: Writes[TTPSubmission], hc: HeaderCarrier) = cache[TTPSubmission](sessionKey, body)

  def get(implicit hc: HeaderCarrier, reads: Reads[TTPSubmission]) = fetchAndGetEntry[TTPSubmission](sessionKey)
}
