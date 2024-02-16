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

package ssttparrangement

import com.google.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.http.Status.CREATED
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.http.{HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement
import util.Logging
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ArrangementConnector @Inject() (
    servicesConfig: ServicesConfig,
    httpClient:     HttpClient)(
    implicit
    ec: ExecutionContext
) extends Logging {

  import req.RequestSupport._

  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val arrangementURL: String = servicesConfig.baseUrl("time-to-pay-arrangement")

  def submitArrangement(ttpArrangement: TTPArrangement)(implicit request: Request[_]): Future[SubmissionResult] = {
    connectionsLogger.info(s"Submit arrangement to time-to-pay-arrangement service")

    httpClient.POST[TTPArrangement, HttpResponse](s"$arrangementURL/ttparrangements", ttpArrangement).map { response =>
      response.status match {
        case CREATED =>
          connectionsLogger.info("Submit arrangement to time-to-pay-arrangement service - outcome: Success")
          Right(SubmissionSuccess())
        case otherCode: Int =>
          val submissionError = SubmissionError(otherCode, response.body)
          connectionsLogger.warn(
            s"Submit arrangement to time-to-pay-arrangement service - outcome: Error" +
              s"[Direct debit reference: ${ttpArrangement.directDebitReference}]",
            submissionError
          )

          Left(submissionError)
      }
    }.recover {
      case e: Throwable =>
        connectionsLogger.warn(
          s"Submit arrangement to time-to-pay-arrangement - outcome: Error" +
            s"[Direct debit reference: ${ttpArrangement.directDebitReference}]",
          e
        )

        onError(e)
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
