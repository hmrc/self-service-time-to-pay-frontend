/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.connectors.{SessionCacheConnector => KeystoreConnector, _}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import com.google.inject._
import uk.gov.hmrc.play.config.AppName

import scala.concurrent.Future

@ImplementedBy(classOf[SessionCacheConnectorImpl])
trait SessionCacheConnector extends SessionCache with ServicesConfig {
  val sessionKey: String

  def ttpSessionCarrier()(implicit hc:HeaderCarrier) = hc.copy(sessionId = hc.extraHeaders.toMap.get(ttpSessionId).map(SessionId))

  def put(body: TTPSubmission)(implicit writes: Writes[TTPSubmission], hc: HeaderCarrier): Future[CacheMap] = {
    cache[TTPSubmission](sessionKey, body)(writes, ttpSessionCarrier)
  }

  def get(implicit hc: HeaderCarrier, reads: Reads[TTPSubmission]): Future[Option[TTPSubmission]] = {
    fetchAndGetEntry[TTPSubmission](sessionKey)(ttpSessionCarrier, reads)
  }

  override def remove()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    super.remove()(ttpSessionCarrier)
  }
}

@Singleton
class SessionCacheConnectorImpl extends KeystoreConnector with AppName with ServicesConfig {
  override val sessionKey: String = getConfString("keystore.sessionKey", throw new RuntimeException("Could not find session key"))

  override def defaultSource: String = appName

  override def baseUri: String = baseUrl("keystore")

  override def domain: String = getConfString("keystore.domain", throw new RuntimeException("Could not find config keystore.domain"))

  override def http: HttpGet with HttpPut with HttpDelete = WSHttp
}
