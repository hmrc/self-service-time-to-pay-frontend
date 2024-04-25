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

package controllers.action

import _root_.controllers.UnhappyPathResponses
import com.google.inject.{Inject, Singleton}
import config.ViewConfig
import play.api.mvc.Results._
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{Enrolments, _}
import util.Logging

import scala.concurrent.{ExecutionContext, Future}

final class AuthenticatedRequest[A](val request:     Request[A],
                                    val enrolments:  Enrolments,
                                    val maybeUtr:    Option[SaUtr],
                                    val credentials: Option[Credentials]
) extends WrappedRequest[A](request) {

  lazy val hasActiveSaEnrolment: Boolean = enrolments.enrolments.exists(e => e.key == "IR-SA" && e.isActivated)

}

@Singleton
class AuthenticatedAction @Inject() (
    af:           AuthorisedFunctions,
    viewConfig:   ViewConfig,
    badResponses: UnhappyPathResponses,
    cc:           MessagesControllerComponents)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[Request, AuthenticatedRequest] with Logging {

  import req.RequestSupport._

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request

    af.authorised().retrieve(
      Retrievals.allEnrolments and Retrievals.saUtr and Retrievals.credentials
    ).apply {
        case enrolments ~ utr ~ credentials =>
          Future.successful(
            Right(new AuthenticatedRequest[A](request, enrolments, utr.map(SaUtr.apply), credentials))
          )
      }
      .recover {
        case _: NoActiveSession =>
          Left(Redirect(viewConfig.loginUrl, Map("continue" -> Seq(viewConfig.frontendBaseUrl + request.uri), "origin" -> Seq("pay-online"))))
        case e: AuthorisationException =>
          appLogger.info(s"Authentication outcome: Failed. Unauthorised because of ${e.reason}, $e")
          Left(badResponses.unauthorised)
      }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}

