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
import play.api.http.Status
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier, HttpGet}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.TaxPayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.Future

object TaxPayerConnector extends TaxPayerConnector with ServicesConfig {
  val taxPayerURL = baseUrl("time-to-pay-eligibility")
  val serviceURL = "time-to-pay-eligibility"
  val http = WSHttp
}

trait TaxPayerConnector {
  val taxPayerURL: String
  val serviceURL: String
  val http: HttpGet

  def getTaxPayer(utr: String)(implicit hc: HeaderCarrier): Future[Option[TaxPayer]] = {
    http.GET[HttpResponse](s"$taxPayerURL/$serviceURL/tax-payer/$utr").map {
       response => response.status match {
         case Status.OK => Some(response.json.as[TaxPayer])
         case _ =>
           Logger.error(s"Failed to get taxpayer, HTTP Code: ${response.status}, HTTP Body ${response.body}")
           None
       }
    }
  }
}
