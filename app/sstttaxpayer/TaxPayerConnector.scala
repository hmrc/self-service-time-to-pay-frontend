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

package sstttaxpayer

import com.google.inject._
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

class TaxPayerConnector @Inject() (servicesConfig: ServicesConfig, http: HttpClient) {

  val taxPayerURL: String = servicesConfig.baseUrl("time-to-pay-taxpayer")
  val serviceURL: String = "taxpayer"

  def getTaxPayer(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Taxpayer]] = {
    http.GET[HttpResponse](s"$taxPayerURL/$serviceURL/$utr").map {
      response => Some(response.json.as[Taxpayer])
    }.recover {
      case e: uk.gov.hmrc.http.NotFoundException =>
        Logger.error("Taxpayer not found")
        None
      case e: Exception =>
        Logger.error(e.getMessage)
        throw new RuntimeException(e.getMessage)
    }
  }
}
