/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import timetopaytaxpayer.cor.TaxpayerConnector
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext
import req.RequestSupport._

class TestOnlyController @Inject() (
    taxpayerConnector: TaxpayerConnector,
    as:                Actions,
    httpClient:        HttpClient,
    cc:                MessagesControllerComponents)(implicit ec: ExecutionContext) extends FrontendBaseController(cc) {

  def config(): Action[AnyContent] = Action { r =>
    val result: JsValue = Json.parse(
      ConfigFactory.load().root().render(ConfigRenderOptions.concise())
    )
    Results.Ok(result)
  }

  def getTaxpayer(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    val utr: SaUtr = model.asTaxpayersSaUtr(request.utr)
    val getTaxpayerF = taxpayerConnector.getReturnsAndDebits(utr).map(Json.toJson(_)).recover{
      case e => Json.obj("exception" -> e.getMessage)
    }

    for {
      taxpayer <- getTaxpayerF
    } yield Ok(taxpayer)
  }

}
