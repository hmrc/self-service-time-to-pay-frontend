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
import play.api.http.Status
import play.api.http.Status._
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SubmissionSuccess()

case class SubmissionError(code: Int, message: String)

trait ArrangementConnector {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val arrangementURL: String
  val serviceURL: String
  val http: HttpGet with HttpPost

  def submitArrangements(ttpArrangement: TTPArrangement)(implicit hc: HeaderCarrier): Future[SubmissionResult] = {
    http.POST[TTPArrangement, HttpResponse](s"$arrangementURL/$serviceURL", ttpArrangement).map { _ =>
      Right(SubmissionSuccess())
    }.recover {
      case e: Throwable => onError(e)
    }
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
