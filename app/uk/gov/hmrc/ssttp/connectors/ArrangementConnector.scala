/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.ssttp.connectors

import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import uk.gov.hmrc.ssttp.config.WSHttp
import uk.gov.hmrc.ssttp.models.TTPArrangement
import uk.gov.hmrc.ssttp.modelsFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ArrangementConnector extends ArrangementConnector with ServicesConfig {
  val arrangementURL = baseUrl("time-to-pay-arrangement")
  val serviceURL = "ttparrangements"
  val http = WSHttp
}

trait ArrangementConnector {
  val arrangementURL: String
  val serviceURL: String
  val http: HttpGet with HttpPost

  def submitArrangements(ttpArrangement: TTPArrangement)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val requestJson = Json.toJson(ttpArrangement)
    http.POST[JsValue, HttpResponse](s"$arrangementURL/$serviceURL", requestJson).map { response =>
      response.status match {
        case Status.CREATED => response
        case _ => response
      }
    }
  }
}
