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

package testonly

import com.google.inject.Singleton
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import req.RequestSupport
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesStubConnector @Inject() (
    httpClient:     HttpClient,
    servicesConfig: ServicesConfig,
    requestSupport: RequestSupport
)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)

  import requestSupport._

  def setReturns(tu: TestUser)(implicit request: Request[_]): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.returnsResponseStatusCode,
      "body" -> tu.returns
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/returns"

    httpClient
      .PATCH[JsValue, HttpResponse](setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's return in DES-STUB: $tu")
          logger.debug(s"Set up a predefined return in DES-STUB for $tu")
      }
  }

  def setDebits(tu: TestUser)(implicit request: Request[_]): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.debitsResponseStatusCode,
      "body" -> tu.debits
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/debits"

    httpClient
      .PATCH[JsValue, HttpResponse](setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's debit in DES-STUB: $tu")
          logger.debug(s"Set up a predefined debit in DES-STUB for $tu")
      }
  }

  private lazy val baseUrl: String = servicesConfig.baseUrl("des-services")

}
