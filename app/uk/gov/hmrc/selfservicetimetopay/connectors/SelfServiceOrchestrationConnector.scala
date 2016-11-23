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

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpErrorFunctions, _}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.BankAccount
import views.html.helper

import scala.concurrent.{ExecutionContext, Future}

object SelfServiceOrchestrationConnector extends SelfServiceOrchestrationConnector with ServicesConfig {
  val serviceURL = baseUrl("self-service-time-to-pay")
  val http = WSHttp
}

trait SelfServiceOrchestrationConnector extends HttpErrorFunctions {
  val serviceURL: String
  val http: HttpGet

  implicit val readBankAccountSeq = new HttpReads[Seq[BankAccount]] {
    implicit val bankAccountReads = Json.format[BankAccount]

    def read(method: String, url: String, response: HttpResponse) = response.status match {
      case OK | NOT_FOUND => response.json.as[Seq[BankAccount]]
      case _              => handleResponse(method, url)(response) match {
        case _            => Seq.empty
      }
    }
  }

  def validateAccount(sortCode: String, accountNumber: String, accountName: Option[String])
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[BankAccount]] = {
    val queryString = s"sortCode=$sortCode&accountNumber=$accountNumber&accountName=${helper.urlEncode(accountName.getOrElse(""))}"
    http.GET[Seq[BankAccount]](s"$serviceURL/validateAccount?$queryString")
  }
}
