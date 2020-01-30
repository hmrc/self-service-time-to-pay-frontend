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

package ssttparrangement

import com.google.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.mvc.Request
import uk.gov.hmrc.http.{HttpException, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.Views
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.{ExecutionContext, Future}

class ArrangementConnector @Inject() (
                                       servicesConfig: ServicesConfig,
                                       httpClient:     HttpClient,
                                       views:          Views)(
                                       implicit
                                       ec: ExecutionContext
                                     ) {

  import req.RequestSupport._

  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val arrangementURL: String = servicesConfig.baseUrl("time-to-pay-arrangement")

  def submitArrangements(ttpArrangement: TTPArrangement)(implicit request: Request[_]): Future[SubmissionResult] = {
    JourneyLogger.info(s"ArrangementConnector.submitArrangements")

    httpClient.POST[TTPArrangement, HttpResponse](s"$arrangementURL/ttparrangements", ttpArrangement).map { _ =>
      Right(SubmissionSuccess()) //todo OPS-3930
    }.recover {
      case e: Throwable =>
        JourneyLogger.info(s"ArrangementConnector.submitArrangements: Error, $e", ttpArrangement)
        onError(e)
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
