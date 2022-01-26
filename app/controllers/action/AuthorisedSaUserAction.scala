/*
 * Copyright 2022 HM Revenue & Customs
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
import config.AppConfig
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr

import scala.concurrent.{ExecutionContext, Future}
import req.RequestSupport._

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A], val utr: SaUtr)
  extends WrappedRequest[A](request)

class AuthorisedSaUserAction @Inject() (
    cc: MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] {

  private val logger = Logger(getClass)

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {

    val hasActiveSaEnrolment: Boolean = request.hasActiveSaEnrolment
    val maybeUtr: Option[SaUtr] = request.maybeUtr

    val obfuscatedEnrolments = {
      val es = request.enrolments.enrolments
      es.map(e =>
        e.copy(identifiers =
          e.identifiers.map(i => i.copy(value = "***"))
        )
      )
    }

    val maybeObfuscatedUtr = maybeUtr.map(_ => "***")

      def logFail(reason: String) = {
        logger.info(
          s"""
           |Authorisation failed, reason: $reason:
           |  [hasActiveSaEnrolment: $hasActiveSaEnrolment]
           |  [enrolments: ${Json.prettyPrint(Json.toJson(obfuscatedEnrolments))}]
           |  [utr: $maybeObfuscatedUtr]
           |  """.stripMargin
        )
      }

    val result: Either[Result, AuthorisedSaUserRequest[A]] =
      (hasActiveSaEnrolment, maybeUtr) match {
        case (false, _) =>
          logFail("no active IR-SA enrolment")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (_, None) =>
          logFail("no present UTR")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (true, Some(utr)) =>
          Right(new AuthorisedSaUserRequest[A](request, utr))
      }

    Future.successful(result)
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

