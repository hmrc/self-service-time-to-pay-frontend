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

import audit.AuditService
import com.google.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr

import scala.concurrent.{ExecutionContext, Future}
import util.Logging

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A], val utr: SaUtr)
  extends WrappedRequest[A](request)

@Singleton
class AuthorisedSaUserAction @Inject() (
    auditService: AuditService,
    cc:           MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] with Logging {

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

      def logFail(reason: String)(implicit rh: RequestHeader) = {
        appLogger.info(
          s"""
           |Authorisation outcome: Failed. Reason: $reason:
           |  [hasActiveSaEnrolment: $hasActiveSaEnrolment]
           |  [enrolments: ${Json.prettyPrint(Json.toJson(obfuscatedEnrolments))}]
           |  [utr: $maybeObfuscatedUtr]
           |  """.stripMargin
        )
      }

    val result: Either[Result, AuthorisedSaUserRequest[A]] =
      (hasActiveSaEnrolment, maybeUtr) match {
        case (_, None) =>
          logFail("no present UTR")(request)
          auditService.sendEligibilityNotEnrolledEvent(request.credentials)(request.request)
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline))
        case (false, _) =>
          logFail("no active IR-SA enrolment")(request)
          auditService.sendEligibilityInactiveEnrolmentEvent(maybeUtr, request.credentials)(request.request)
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline))
        case (true, Some(utr)) =>
          Right(new AuthorisedSaUserRequest[A](request, utr))
      }

    Future.successful(result)
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

