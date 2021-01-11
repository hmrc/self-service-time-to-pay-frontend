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
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200

import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A], val utr: SaUtr)
  extends WrappedRequest[A](request)

class AuthorisedSaUserAction @Inject() (cc: MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {
      def notEnrolled: Either[Result, AuthorisedSaUserRequest[A]] = {
        Logger.info(
          s"""
           |Authorisation failed:
           |  [hasActiveSaEnrolment: ${request.hasActiveSaEnrolment}]
           |  [enrolments: ${request.enrolments}]
           |  [utr: ${request.maybeUtr}]
           |  [ConfidenceLevel: ${request.confidenceLevel}]
           |  """.stripMargin)
        Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
      }

    Future.successful(
      request.maybeUtr.fold(notEnrolled) { utr =>
        if (request.hasActiveSaEnrolment && request.confidenceLevel >= L200)
          Right(new AuthorisedSaUserRequest[A](request, utr))
        else notEnrolled
      }
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

