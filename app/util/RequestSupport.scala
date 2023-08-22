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

package util

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.Future

class RequestSupport @Inject() (override val messagesApi: MessagesApi) extends I18nSupport {

  implicit def hc(implicit request: RequestHeader): HeaderCarrier = RequestSupport.hc
}

object RequestSupport {

  implicit def hc(implicit request: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

  def getSessionId()(implicit request: Request[_]): Future[SessionId] = Future.successful{
    //HINT: We wrapp it into future so it can throw exception inside for comprehension and propagate
    // nicely failed result
    hc
      .sessionId
      .map(s => SessionId(s.value))
      .getOrElse(
        throw UpstreamErrorResponse("Session id must be provided", play.mvc.Http.Status.BAD_REQUEST)
      )
  }
}
