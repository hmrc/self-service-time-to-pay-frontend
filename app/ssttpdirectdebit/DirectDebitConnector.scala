/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{Request, RequestHeader}
import ssttparrangement.SubmissionError
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.{HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.selfservicetimetopay.models._
import _root_.util.Logging

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClient)(
    implicit
    ec: ExecutionContext
) extends Logging {

  type DDSubmissionResult = Either[SubmissionError, DirectDebitInstructionPaymentPlan]

  import req.RequestSupport._

  val baseUrl: String = servicesConfig.baseUrl("direct-debit")

  def submitPaymentPlan(paymentPlan: PaymentPlanRequest, saUtr: SaUtr)(implicit request: Request[_]): Future[DDSubmissionResult] = {
    connectionsLogger.info("Submit payment plan to direct-debit service")

    httpClient.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](s"$baseUrl/direct-debit/${saUtr.value}/instructions/payment-plan", paymentPlan).map {
      Result => Right(Result)
    }.recover {
      case e: Throwable =>
        connectionsLogger.warn("Submit payment plan to direct-debit service - outcome: Error", e)
        onError(e)
    }
  }

  /**
   * Retrieves stored bank details associated with a given saUtr
   */
  def getBanks(saUtr: SaUtr)(implicit request: Request[_]): Future[DirectDebitInstructions] = {
    connectionsLogger.info("Get bank details from direct-debit service")

    httpClient.GET[DirectDebitInstructions](s"$baseUrl/direct-debit/${saUtr.value}/banks").map {
      response => response
    }.recover {
      case e: RuntimeException =>
        connectionsLogger.warn("Get bank details from direct-debit service - outcome: Error", e)
        throw new RuntimeException("GETBANKS threw unexpected error")
    }
  }

  private def onError(ex: Throwable)(implicit rh: RequestHeader) = {
    val (code, message) = ex match {
      case e: HttpException         => (e.responseCode, e.getMessage)

      case e: UpstreamErrorResponse => (e.reportAs, e.getMessage)

      case e: Throwable             => (Status.INTERNAL_SERVER_ERROR, e.getMessage)
    }

    val submissionError = SubmissionError(code, message)
    connectionsLogger.warn("Failure from DES", submissionError)
    Left(submissionError)
  }
}

