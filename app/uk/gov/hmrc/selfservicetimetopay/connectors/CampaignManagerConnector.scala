/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait CampaignManagerConnector {
  val campaignURL: String
  val serviceURL: String
  val http: HttpGet

  def isAuthorisedWhitelist(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    http.GET[HttpResponse](s"$campaignURL/$serviceURL/$utr").map {
      response => response.status match {
        case Status.OK => true
        case _ =>
          Logger.error(s"User is not authorised to access the service. Utr is not white-listed: ${response.status}, HTTP Body ${response.body}")
          false
      }
    }
  }
}