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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
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
    http.POST[TTPArrangement, HttpResponse](s"$arrangementURL/$serviceURL", ttpArrangement).map { response =>
      response.status match {
        case CREATED => Right(SubmissionSuccess())
        case _ => Left(SubmissionError(response.status, response.body))
      }
    }
  }
}
