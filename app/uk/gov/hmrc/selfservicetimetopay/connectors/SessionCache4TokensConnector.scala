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
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.selfservicetimetopay.auth.{Token, TokenData}
import uk.gov.hmrc.selfservicetimetopay.config.{DefaultRunModeAppNameConfig, WSHttp}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

/**
  * This one ignores header carrier and sets key explicitly with token value
  */
@ImplementedBy(classOf[SessionCache4TokensConnectorImpl])
trait SessionCache4TokensConnector extends SessionCache with ServicesConfig {

  val formId: String = "ttp-token"

  def put(tokenData: TokenData)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = cache[TokenData](
    defaultSource,
    s"$formId-${tokenData.token.v}",
    formId,
    tokenData
  ).map(_ => println(s"ttp-token PUT $tokenData"))

  def getAndRemove(token: Token)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[TokenData]] = for {
    tokenData <- fetchAndGetEntry[TokenData](defaultSource, s"$formId-${token.v}", formId)
    _ <- delete(buildUri(defaultSource, s"$formId-${token.v}"))
    _ = println(s"ttp-token GETANDREMOVE $token $tokenData")
  } yield tokenData

}

@Singleton
class SessionCache4TokensConnectorImpl extends SessionCache4TokensConnector with AppName with ServicesConfig with DefaultRunModeAppNameConfig {

  override def defaultSource: String = appName

  override def baseUri: String = baseUrl("keystore")

  override def domain: String = getConfString("keystore.domain", throw new RuntimeException("Could not find config keystore.domain"))

  override def http: HttpGet with HttpPut with HttpDelete = WSHttp

}
