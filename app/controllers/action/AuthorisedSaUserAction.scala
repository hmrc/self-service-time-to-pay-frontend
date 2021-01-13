/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr

import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A], val utr: SaUtr)
  extends WrappedRequest[A](request)

class AuthorisedSaUserAction @Inject() (cc: MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {
      def ineligibleRequest: Either[Result, AuthorisedSaUserRequest[A]] = {
          def logFail(reason: String) = Logger.info(
            s"""
           |Authorisation failed, reason: $reason:
           |  [hasActiveSaEnrolment: ${request.hasActiveSaEnrolment}]
           |  [enrolments: ${request.enrolments}]
           |  [utr: ${request.maybeUtr}]
           |  [ConfidenceLevel: ${request.confidenceLevel}]
           |  """.stripMargin)

        val call = if (request.needsEnrolment) {
          logFail("no active IR-SA enrolment or UTR")
          ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()
        } else {
          logFail("active IR-SA enrolment and UTR but needs CL uplift")
          ssttpeligibility.routes.SelfServiceTimeToPayController.confidenceUplift()
        }

        Left(Redirect(call))
      }

    Future.successful(
      request.maybeUtr.fold(ineligibleRequest) { utr =>
        if (request.eligible) {
          Right(new AuthorisedSaUserRequest[A](request, utr))
        } else ineligibleRequest
      }
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

