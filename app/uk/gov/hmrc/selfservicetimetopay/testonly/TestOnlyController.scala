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

package uk.gov.hmrc.selfservicetimetopay.testonly

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
import javax.inject._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action => PlayAction, _}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.DefaultRunModeAppNameConfig
import uk.gov.hmrc.selfservicetimetopay.connectors.TaxPayerConnector
import uk.gov.hmrc.selfservicetimetopay.controllers.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class TestOnlyController @Inject()(val messagesApi: MessagesApi, taxpayerConnector: TaxPayerConnector)
extends TimeToPayController with I18nSupport with ServicesConfig with DefaultRunModeAppNameConfig {

  def config(): PlayAction[AnyContent] = PlayAction { r =>
    val result: JsValue = Json.parse(
      ConfigFactory.load().root().render(ConfigRenderOptions.concise())
    )
    Results.Ok(result)
  }

  def getTaxpayer(): PlayAction[AnyContent] = authorisedSaUser { implicit authContext =>implicit request =>
    val utr = authContext.principal.accounts.sa.get.utr.utr
    val getTaxpayerF = taxpayerConnector.getTaxPayer(utr).map{
      case Some(t) => Json.toJson(t)
      case None => Json.obj("there is" -> "taxpayer for given UTR")
    }.recover{
      case e => Json.obj("exception" -> e.getMessage)
    }

    for {
      taxpayer <- getTaxpayerF
    } yield Ok(taxpayer)
  }

  def taxpayerConfig(): PlayAction[AnyContent] = PlayAction.async { implicit request =>
    val baseUrl = taxpayerConnector.taxPayerURL
    taxpayerConnector.http.GET[HttpResponse](s"$baseUrl/taxpayer/test-only/config")
      .map(r => Status(r.status)(r.json))
  }

  def taxpayerConnectorsConfig(): PlayAction[AnyContent] = PlayAction.async { implicit request =>
    val baseUrl = taxpayerConnector.taxPayerURL
    taxpayerConnector.http.GET[HttpResponse](s"$baseUrl/taxpayer/test-only/connectors-config")
      .map(r => Status(r.status)(r.json))
  }

}
