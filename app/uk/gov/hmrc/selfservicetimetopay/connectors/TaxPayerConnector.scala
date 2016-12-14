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
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier, HttpGet}
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

trait TaxPayerConnector {
  val taxPayerURL: String
  val serviceURL: String
  val http: HttpGet

  def getTaxPayer(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Taxpayer]] = {
    http.GET[HttpResponse](s"$taxPayerURL/$serviceURL/$utr").map {
      response => Some(response.json.as[Taxpayer])
    }.recover {
      case e: uk.gov.hmrc.play.http.NotFoundException => Logger.error("Taxpayer not found")
        None
      case e: Exception => Logger.error(e.getMessage)
        throw new RuntimeException(e.getMessage)
    }
  }
}

