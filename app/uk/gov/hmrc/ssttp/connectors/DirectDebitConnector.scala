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

import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.ssttp.config.WSHttp
import uk.gov.hmrc.ssttp.models.{DirectDebitBank, DirectDebitInstructionPaymentPlan}
import uk.gov.hmrc.ssttp.modelsFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DirectDebitConnector extends DirectDebitConnector with ServicesConfig {
  val directDebitURL = baseUrl("direct-debit")
  val serviceURL = "direct-debits"
  val http = WSHttp
}

trait DirectDebitConnector {
  val directDebitURL: String
  val serviceURL: String
  val http: HttpGet

  def getBanksList(saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[DirectDebitBank] = {
    http.GET[HttpResponse](s"$directDebitURL/$serviceURL/$saUtr/banks").map { response =>
      response.json.as[DirectDebitBank]
    }
  }

  //URL will need to be changed once Direct-Debit routes has been updated
  def getInstructionPaymentPlan()(implicit hc: HeaderCarrier): Future[DirectDebitInstructionPaymentPlan] = {
    http.GET[HttpResponse](s"$directDebitURL/$serviceURL/payment-plan").map { response =>
      response.json.as[DirectDebitInstructionPaymentPlan]
    }
  }
}
