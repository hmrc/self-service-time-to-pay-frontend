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

package uk.gov.hmrc.selfservicetimetopay.connectors

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorInput, CalculatorPaymentSchedule}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CalculatorConnector {
  val calculatorURL: String
  val serviceURL: String
  val http: HttpPost

  def submitLiabilities(liabilities: CalculatorInput)(implicit hc: HeaderCarrier): Future[Option[Seq[CalculatorPaymentSchedule]]] = {
    http.POST[CalculatorInput, Option[Seq[CalculatorPaymentSchedule]]](s"$calculatorURL/$serviceURL", liabilities).map {
      case Some(response) => Some(response)
      case _ =>
        Logger.error("No payment schedule retrieved")
        None
    }
  }
}
