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
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait DirectDebitConnector {
  val directDebitURL: String
  val serviceURL: String
  val http: HttpGet with HttpPost

  def createPaymentPlan(paymentPlan: PaymentPlanRequest, saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[DirectDebitInstructionPaymentPlan] = {
    http.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](s"$directDebitURL/$serviceURL/$saUtr/instructions/payment-plan", paymentPlan)
  }

  trait BankAccountHttpReads extends HttpReads[Either[BankDetails, DirectDebitBank]] with HttpErrorFunctions

  implicit val readValidateOrRetrieveAccounts = new BankAccountHttpReads {
    override def read(method: String, url: String, response: HttpResponse) = response.status match {
      case OK => Left(response.json.as[BankDetails])
      case NOT_FOUND => Right(response.json.as[DirectDebitBank])
      case _ => handleResponse(method, url)(response) match {
        case _ => Right(DirectDebitBank.none)
      }
    }
  }

  def getBanks(saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DirectDebitBank] = {
    http.GET[DirectDebitBank](s"$directDebitURL/$serviceURL/$saUtr/banks")
  }

  def validateOrRetrieveAccounts(sortCode: String, accountNumber: String, saUtr: SaUtr)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[BankDetails, DirectDebitBank]] = {
    val queryString = s"sortCode=$sortCode&accountNumber=$accountNumber"
    http.GET[Either[BankDetails, DirectDebitBank]](s"$directDebitURL/$serviceURL/$saUtr/bank?$queryString")
  }
}
