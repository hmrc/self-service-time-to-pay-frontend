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
import play.api.http.Status._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import com.google.inject._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DirectDebitConnectorImpl])
trait DirectDebitConnector {
  type DDSubmissionResult = Either[SubmissionError, DirectDebitInstructionPaymentPlan]

  val directDebitURL: String
  val serviceURL: String
  val http: HttpGet with HttpPost

  def createPaymentPlan(paymentPlan: PaymentPlanRequest, saUtr: SaUtr)(implicit hc: HeaderCarrier): Future[DDSubmissionResult] = {
    http.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](s"$directDebitURL/$serviceURL/$saUtr/instructions/payment-plan", paymentPlan).map { Result =>
      Right(Result)
    }.recover {
      case e: Throwable => onError(e)
    }
  }

  trait BankAccountHttpReads extends HttpReads[Either[BankDetails, DirectDebitBank]] with HttpErrorFunctions

  implicit val readValidateOrRetrieveAccounts = new BankAccountHttpReads {
    override def read(method: String, url: String, response: HttpResponse) = response.status match {
      case OK => Left(response.json.as[BankDetails])
      case NOT_FOUND if response.body.contains("BP not found") => Right(DirectDebitBank.none)
      case NOT_FOUND => Right(response.json.as[DirectDebitBank])
      case _ => handleResponse(method, url)(response) match {
        case _ => Right(DirectDebitBank.none)
      }
    }
  }

  def getBank(sortCode: String, accountNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BankDetails]] = {
    val queryString = s"sortCode=$sortCode&accountNumber=$accountNumber"
    http.GET[HttpResponse](s"$directDebitURL/$serviceURL/bank?$queryString").map {
      response => Some(response.json.as[BankDetails])
    }.recover {
      case e: uk.gov.hmrc.play.http.NotFoundException => None
      case e: Exception => Logger.error(e.getMessage)
        throw new RuntimeException("Direct debit returned unexpected response")
    }
  }

  def getBanks(saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DirectDebitBank] = {
    http.GET[DirectDebitBank](s"$directDebitURL/$serviceURL/$saUtr/banks").map { response => response }
      .recover {
        case e: uk.gov.hmrc.play.http.NotFoundException if e.message.contains("BP not found") => DirectDebitBank.none
        case e: Exception => Logger.error(e.getMessage)
          throw new RuntimeException("GETBANKS threw unexpected error")
      }
  }

  def validateOrRetrieveAccounts(sortCode: String, accountNumber: String, saUtr: SaUtr)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[BankDetails, DirectDebitBank]] = {
    val queryString = s"sortCode=$sortCode&accountNumber=$accountNumber"
    http.GET[Either[BankDetails, DirectDebitBank]](s"$directDebitURL/$serviceURL/$saUtr/bank?$queryString")
  }

  private def onError(ex: Throwable) = {
    val (code, message) = ex match {
      case e: HttpException => (e.responseCode, e.getMessage)

      case e: Upstream4xxResponse => (e.reportAs, e.getMessage)
      case e: Upstream5xxResponse => (e.reportAs, e.getMessage)

      case e: Throwable => (Status.INTERNAL_SERVER_ERROR, e.getMessage)
    }

    Logger.error(s"Failure from DES, code $code and body $message")
    Left(SubmissionError(code, message))
  }
}

class DirectDebitConnectorImpl extends DirectDebitConnector with ServicesConfig {
  lazy val directDebitURL: String = baseUrl("direct-debit")
  lazy val serviceURL = "direct-debit"
  lazy val http = WSHttp
}
