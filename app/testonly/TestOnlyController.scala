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

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import controllers.FrontendController
import controllers.action.Actions
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import sstttaxpayer.TaxPayerConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class TestOnlyController @Inject() (
    taxpayerConnector: TaxPayerConnector,
    i18nSupport:       I18nSupport,
    as:                Actions,
    httpClient:        HttpClient
)
  extends FrontendController {

  import i18nSupport._

  def config(): Action[AnyContent] = Action { r =>
    val result: JsValue = Json.parse(
      ConfigFactory.load().root().render(ConfigRenderOptions.concise())
    )
    Results.Ok(result)
  }

  def getTaxpayer(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    val utr = request.utr
    val getTaxpayerF = taxpayerConnector.getTaxPayer(utr).map{
      case Some(t) => Json.toJson(t)
      case None    => Json.obj("there is" -> "taxpayer for given UTR")
    }.recover{
      case e => Json.obj("exception" -> e.getMessage)
    }

    for {
      taxpayer <- getTaxpayerF
    } yield Ok(taxpayer)
  }

  def taxpayerConfig(): Action[AnyContent] = Action.async { implicit request =>
    val baseUrl = taxpayerConnector.taxPayerURL
    httpClient
      .GET[HttpResponse](s"$baseUrl/taxpayer/test-only/config")
      .map(r => Status(r.status)(r.json))
  }

  def taxpayerConnectorsConfig(): Action[AnyContent] = Action.async { implicit request =>
    val baseUrl = taxpayerConnector.taxPayerURL
    httpClient
      .GET[HttpResponse](s"$baseUrl/taxpayer/test-only/connectors-config")
      .map(r => Status(r.status)(r.json))
  }

}
