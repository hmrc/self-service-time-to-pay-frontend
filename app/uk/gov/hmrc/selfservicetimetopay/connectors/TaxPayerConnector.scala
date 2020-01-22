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

package uk.gov.hmrc.selfservicetimetopay.connectors

import java.time.Clock

import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import com.google.inject._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.{DefaultRunModeAppNameConfig, WSHttp}
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TaxPayerConnectorImpl])
trait TaxPayerConnector {
  val taxPayerURL: String
  val serviceURL: String
  val http: HttpGet

  def getTaxPayer(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, clock: Clock): Future[Option[Taxpayer]] = {
    JourneyLogger.info("TaxPayerConnector.getTaxPayer")
    http.GET[HttpResponse](s"$taxPayerURL/$serviceURL/$utr").map {
      response =>
        val taxpayer = response.json.as[Taxpayer].fixReturns
        Some(taxpayer)
    }.recover {
      case e: uk.gov.hmrc.http.NotFoundException =>
        JourneyLogger.info("TaxPayerConnector.getTaxPayer: taxpayer not found")
        Logger.error("Taxpayer not found", e)
        None
      case e: Exception =>
        JourneyLogger.info(s"TaxPayerConnector.getTaxPayer: ERROR, $e")
        Logger.error(e.getMessage, e)
        throw new RuntimeException(e)
    }
  }
}

@Singleton
class TaxPayerConnectorImpl extends TaxPayerConnector with ServicesConfig with DefaultRunModeAppNameConfig {
  val taxPayerURL: String = baseUrl("time-to-pay-taxpayer")
  val serviceURL = "taxpayer"
  val http: WSHttp.type = WSHttp
}
