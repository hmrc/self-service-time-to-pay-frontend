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

package ssttpdirectdebit

import com.google.inject._
import play.api.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.mvc.Request
import ssttparrangement.SubmissionError
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClient)(
    implicit
    ec: ExecutionContext
) {
  type DDSubmissionResult = Either[SubmissionError, DirectDebitInstructionPaymentPlan]

  import req.RequestSupport._

  val baseUrl: String = servicesConfig.baseUrl("direct-debit")

  def createPaymentPlan(paymentPlan: PaymentPlanRequest, saUtr: SaUtr)(implicit request: Request[_]): Future[DDSubmissionResult] = {
    JourneyLogger.info(s"DirectDebitConnector.createPaymentPlan")

    httpClient.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](s"$baseUrl/direct-debit/${saUtr.value}/instructions/payment-plan", paymentPlan).map {
      Result => Right(Result)
    }.recover {
      case e: Throwable =>
        JourneyLogger.info(s"DirectDebitConnector.createPaymentPlan: Error, $e")
        onError(e)
    }
  }

  trait BankAccountHttpReads extends HttpReads[Either[BankDetails, DirectDebitBank]] with HttpErrorFunctions

  implicit val readValidateOrRetrieveAccounts: BankAccountHttpReads = new BankAccountHttpReads {
    override def read(method: String, url: String, response: HttpResponse): Either[BankDetails, DirectDebitBank] = response.status match {
      case OK => Left(response.json.as[BankDetails])
      case NOT_FOUND if response.body.contains("BP not found") => Right(DirectDebitBank.none)
      case NOT_FOUND => Right(response.json.as[DirectDebitBank])
      case _ => handleResponse(method, url)(response) match {
        case _ => Right(DirectDebitBank.none)
      }
    }
  }

  /**
   * Checks if the given bank details are valid by checking against the Bank Account Reputation Service via Direct Debit service
   */
  def getBank(sortCode: String, accountNumber: String)(implicit request: Request[_]): Future[Option[BankDetails]] = {
    JourneyLogger.info(s"DirectDebitConnector.getBank")
    val queryString = s"sortCode=$sortCode&accountNumber=$accountNumber"
    httpClient.GET[Option[BankDetails]](s"$baseUrl/direct-debit/bank?$queryString")
      .recover {
        case e: Exception =>
          JourneyLogger.info(s"DirectDebitConnector.getBank: Error, $e")
          Logger.error("Direct debit returned unexpected response", e)
          throw new RuntimeException("Direct debit returned unexpected response")
      }
  }

  /**
   * Retrieves stored bank details associated with a given saUtr
   */
  def getBanks(saUtr: SaUtr)(implicit request: Request[_]): Future[DirectDebitBank] = {
    JourneyLogger.info(s"DirectDebitConnector.getBanks")

    httpClient.GET[DirectDebitBank](s"$baseUrl/direct-debit/${saUtr.value}/banks").map { response => response }
      .recover {
        case e: Exception =>
          JourneyLogger.info(s"DirectDebitConnector.getBanks: Error, $e")
          Logger.error(e.getMessage)
          throw new RuntimeException("GETBANKS threw unexpected error")
      }
  }

  private def onError(ex: Throwable) = {
    val (code, message) = ex match {
      case e: HttpException       => (e.responseCode, e.getMessage)

      case e: Upstream4xxResponse => (e.reportAs, e.getMessage)
      case e: Upstream5xxResponse => (e.reportAs, e.getMessage)

      case e: Throwable           => (Status.INTERNAL_SERVER_ERROR, e.getMessage)
    }

    Logger.error(s"Failure from DES, code $code and body $message")
    Left(SubmissionError(code, message))
  }
}

