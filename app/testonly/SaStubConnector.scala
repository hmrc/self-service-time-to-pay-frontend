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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import req.RequestSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import util.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SaStubConnector @Inject() (
    httpClient:     HttpClient,
    servicesConfig: ServicesConfig,
    requestSupport: RequestSupport)(
    implicit
    ec: ExecutionContext) extends Logging {

  import requestSupport._

  def setTaxpayerResponse(tu: TestUser)(implicit request: Request[_]): Future[Unit] = {

    val predefinedResponse: JsValue = Json.obj(
      "status" -> tu.saTaxpayerResponseStatusCode,
      "body" -> tu.saTaxpayer
    )
    val setTaxpayerUrl = s"$baseUrl/sa/individual/${tu.utr.v}/designatory-details/taxpayer"

    httpClient
      .POST[JsValue, HttpResponse](setTaxpayerUrl, predefinedResponse)
      .map{
        r =>
          if (r.status != 200) throw new RuntimeException(s"Could not set up taxpayer in PAYMENT-STUBS-PROTECTED: ${tu.utr}")
          stubsConnectionsLogger.debug(s"Set up a predefined response in PAYMENT-STUBS-PROTECTED for ${tu.utr}")
      }
  }

  private lazy val baseUrl: String = servicesConfig.baseUrl("payment-stubs-protected")

}
