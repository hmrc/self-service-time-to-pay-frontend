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

package token

import com.google.inject.Inject
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePut, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay._

import scala.concurrent.{ExecutionContext, Future}
import modelsFormat._
import play.api.mvc.Request

/**
 * This one ignores header carrier and sets key explicitly with token value
 */
class TokenService @Inject() (servicesConfig: ServicesConfig, httpClient: HttpClient)(implicit executionContext: ExecutionContext) {

  import req.RequestSupport._

  private val delegate = new SessionCache(servicesConfig, httpClient)

  def put(tokenData: TokenData)(implicit request: Request[_]): Future[Unit] =
    delegate.put(tokenData)

  def getAndRemove(token: Token)(implicit request: Request[_]): Future[Option[TokenData]] =
    delegate.getAndRemove(token)

  private class SessionCache(servicesConfig: ServicesConfig, httpClient: HttpClient) extends uk.gov.hmrc.http.cache.client.SessionCache {

    val formId: String = "ttp-token"

    override def defaultSource: String = servicesConfig.getString("appName")

    override def baseUri: String = servicesConfig.baseUrl("keystore")

    override def domain: String = servicesConfig.getConfString("keystore.domain", throw new RuntimeException("Could not find config keystore.domain"))

    override def http: CoreGet with CorePut with CoreDelete = httpClient

    def put(tokenData: TokenData)(implicit request: Request[_]): Future[Unit] = cache[TokenData](
      defaultSource,
      s"$formId-${tokenData.token.v}",
      formId,
      tokenData
    ).map(_ => ())

    def getAndRemove(token: Token)(implicit request: Request[_], executionContext: ExecutionContext): Future[Option[TokenData]] = for {
      tokenData <- fetchAndGetEntry[TokenData](defaultSource, s"$formId-${token.v}", formId)
      _ <- delete(buildUri(defaultSource, s"$formId-${token.v}"))
    } yield tokenData

  }

}
