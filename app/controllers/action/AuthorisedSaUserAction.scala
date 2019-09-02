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
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core.ConfidenceLevel

import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A],
                                       val utr:     String
) extends WrappedRequest[A](request)

class AuthorisedSaUserAction @Inject() (
    cc: MessagesControllerComponents
) extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {

    val result =
      if (request.hasActiveSaEnrolment &&
        request.confidenceLevel >= ConfidenceLevel.L200 &&
        request.maybeUtr.isDefined)
        Right(new AuthorisedSaUserRequest[A](request, request.maybeUtr.get))
      else {
        Logger.info(
          s"""
           |Authorisation failed:
           |  [hasActiveSaEnrolment: ${request.hasActiveSaEnrolment}]
           |  [enrolments: ${request.enrolments}]
           |  [utr: ${request.maybeUtr}]
           |  [ConfidenceLevel: ${request.confidenceLevel}]
           |  """.stripMargin)
        Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled()))
      }
    Future.successful(result)

  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

