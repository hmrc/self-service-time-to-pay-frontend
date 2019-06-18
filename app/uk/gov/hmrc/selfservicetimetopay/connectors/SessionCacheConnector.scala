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

package uk.gov.hmrc.selfservicetimetopay.connectors

import com.google.inject._
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.selfservicetimetopay.config.{DefaultRunModeAppNameConfig, WSHttp}
import uk.gov.hmrc.selfservicetimetopay.connectors.{SessionCacheConnector => KeystoreConnector}
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.util.TTPSessionId.ttpSessionId

import scala.concurrent.{ExecutionContext, Future}

/**
 * Talks to the keystore service to save and retrieve TTPSubmission data
 */
@ImplementedBy(classOf[SessionCacheConnectorImpl])
trait SessionCacheConnector extends SessionCache with ServicesConfig {
  val sessionKey: String

  def ttpSessionCarrier()(implicit hc: HeaderCarrier): HeaderCarrier = hc.copy(sessionId = hc.extraHeaders.toMap.get(ttpSessionId).map(SessionId))

  def putTtpSessionCarrier(body: TTPSubmission)(implicit writes: Writes[TTPSubmission], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    cache[TTPSubmission](sessionKey, body)(writes, ttpSessionCarrier, ec)
  }

  def getTtpSessionCarrier(implicit hc: HeaderCarrier, reads: Reads[TTPSubmission], ec: ExecutionContext): Future[Option[TTPSubmission]] = {
    fetchAndGetEntry[TTPSubmission](sessionKey)(ttpSessionCarrier, reads, ec)
  }

  def putAmount(amount: BigDecimal)(implicit writes: Writes[BigDecimal], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    cache[BigDecimal]("amount", amount)(writes, ttpSessionCarrier, ec)
  }

  def getAmount()(implicit hc: HeaderCarrier, reads: Reads[BigDecimal], ec: ExecutionContext): Future[Option[BigDecimal]] = {
    fetchAndGetEntry[BigDecimal]("amount")(ttpSessionCarrier, reads, ec)
  }

  def putIsBPath(isBpath: Boolean)(implicit writes: Writes[Boolean], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    cache[Boolean]("isBPath", isBpath)(writes, ttpSessionCarrier, ec)
  }

  def getIsBpath()(implicit hc: HeaderCarrier, reads: Reads[Boolean], ec: ExecutionContext): Future[Option[Boolean]] = {
    fetchAndGetEntry[Boolean]("isBPath")(ttpSessionCarrier, reads, ec)
  }

  override def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    super.remove()(ttpSessionCarrier, ec)
  }
}

@Singleton
class SessionCacheConnectorImpl extends KeystoreConnector with AppName with ServicesConfig with DefaultRunModeAppNameConfig {
  override val sessionKey: String = getConfString("keystore.sessionKey", throw new RuntimeException("Could not find session key"))

  override def defaultSource: String = appName

  override def baseUri: String = baseUrl("keystore")

  override def domain: String = getConfString("keystore.domain", throw new RuntimeException("Could not find config keystore.domain"))

  override def http: HttpGet with HttpPut with HttpDelete = WSHttp
}
