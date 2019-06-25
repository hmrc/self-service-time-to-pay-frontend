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

package testonly

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesStubConnector @Inject() (
    httpClient:     HttpClient,
    servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext) {

  def setReturns(tu: TestUser)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.returnsResponseStatusCode,
      "body" -> tu.returns
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/returns"

    httpClient
      .PATCH(setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's return in DES-STUB: $tu")
          Logger.debug(s"Set up a predefined return in DES-STUB for $tu")
      }
  }

  def setDebits(tu: TestUser)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.debitsResponseStatusCode,
      "body" -> tu.debits
    )

    val setReturnsUrl = s"$baseUrl/sa/taxpayer/${tu.utr.v}/debits"

    httpClient
      .PATCH(setReturnsUrl, predefinedResponse)
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not set up taxpayer's debit in DES-STUB: $tu")
          Logger.debug(s"Set up a predefined debit in DES-STUB for $tu")
      }
  }

  private lazy val baseUrl: String = servicesConfig.baseUrl("des-services")

}
