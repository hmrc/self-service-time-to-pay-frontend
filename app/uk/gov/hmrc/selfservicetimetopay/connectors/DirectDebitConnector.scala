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

import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank, DirectDebitInstructionPaymentPlan, PaymentPlanRequest}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.Future

object DirectDebitConnector extends DirectDebitConnector with ServicesConfig {
  val directDebitURL = baseUrl("direct-debit")
  lazy val serviceURL = "direct-debit"
  val http = WSHttp
}

trait DirectDebitConnector {
  val directDebitURL: String
  val serviceURL: String
  val http: HttpGet with HttpPost

  def getBanksList(saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[DirectDebitBank] = {
    http.GET[DirectDebitBank](s"$directDebitURL/$serviceURL/$saUtr/banks")
  }

  def getBank(sortCode: String, accountNumber: String)(implicit hc: HeaderCarrier): Future[BankDetails] = {
    http.GET[BankDetails](s"$directDebitURL/$serviceURL/bank?sortCode=:$sortCode&accountNumber=:$accountNumber")
  }

  def createPaymentPlan(paymentPlan: PaymentPlanRequest, saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[DirectDebitInstructionPaymentPlan] = {
    http.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](s"$directDebitURL/$serviceURL/$saUtr/instructions/payment-plan", paymentPlan)
  }
}
