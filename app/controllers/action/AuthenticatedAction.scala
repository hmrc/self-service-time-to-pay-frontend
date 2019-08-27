/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.action

import com.google.inject.Inject
import config.ViewConfig
import _root_.controllers.UnhappyPathResponses
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

final class AuthenticatedRequest[A](val request:         MessagesRequest[A],
                                    val enrolments:      Enrolments,
                                    val confidenceLevel: ConfidenceLevel,
                                    val maybeUtr:        Option[String]
) extends MessagesRequest[A](request, request.messagesApi) {

  private val sa = core.Enrolment("IR-SA")
  lazy val hasActiveSaEnrolment: Boolean = enrolments.enrolments.contains(sa)
}

class AuthenticatedAction @Inject() (
    af:           AuthorisedFunctions,
    viewConfig:   ViewConfig,
    badResponses: UnhappyPathResponses,
    cc:           MessagesControllerComponents)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[MessagesRequest, AuthenticatedRequest] {

  override protected def refine[A](request: MessagesRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val r: Request[A] = request
    implicit val mr: MessagesRequest[A] = request

    af.authorised.retrieve(
      Retrievals.allEnrolments and Retrievals.confidenceLevel and Retrievals.saUtr
    ).apply {
        case enrolments ~ confidenceLevel ~ utr =>
          Future.successful(
            Right(new AuthenticatedRequest[A](request, enrolments, confidenceLevel, utr))
          )
      }
      .recover {
        case _: NoActiveSession =>
          //TODO: what is a proper value to origin
          Left(Redirect(viewConfig.loginUrl, Map("continue" -> Seq(viewConfig.frontendBaseUrl + request.uri), "origin" -> Seq("pay-online"))))
        case e: AuthorisationException =>
          Logger.debug(s"Unauthorised because of ${e.reason}, $e")
          Left(badResponses.unauthorised)
      }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}

