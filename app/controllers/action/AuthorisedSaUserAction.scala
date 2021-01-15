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
import config.AppConfig
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.ConfidenceLevel

import scala.concurrent.{ExecutionContext, Future}
import req.RequestSupport._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequest[A], val utr: SaUtr)
  extends WrappedRequest[A](request)

class AuthorisedSaUserAction @Inject() (
    appConfig: AppConfig,
    cc:        MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedSaUserRequest] {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {

    val hasActiveSaEnrolment: Boolean = request.hasActiveSaEnrolment
    val maybeUtr: Option[SaUtr] = request.maybeUtr
    val isConfident: Boolean = request.confidenceLevel >= ConfidenceLevel.L200

      def logFail(reason: String) = Logger.info(
        s"""
         |Authorisation failed, reason: $reason:
         |  [hasActiveSaEnrolment: $hasActiveSaEnrolment]
         |  [enrolments: ${request.enrolments}]
         |  [utr: $maybeUtr]
         |  [ConfidenceLevel: ${request.confidenceLevel}]
         |  """.stripMargin
      )

    val result: Either[Result, AuthorisedSaUserRequest[A]] =
      (hasActiveSaEnrolment, maybeUtr, isConfident) match {
        case (false, _, _) =>
          logFail("no active IR-SA enrolment")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (_, None, _) =>
          logFail("no present UTR")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (true, Some(utr), false) =>
          logFail("Confidence level is to low, redirecting to identity-verification")
          Left(redirectToUplift(request))
        case (true, Some(utr), true) =>
          Right(new AuthorisedSaUserRequest[A](request, utr))
      }

    Future.successful(result)
  }

  private def redirectToUplift(implicit request: Request[_]): Result = {
    Redirect(
      appConfig.mdtpUpliftUrl,
      Map(
        "origin" -> Seq("ssttpf"),
        "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
        "completionURL" -> Seq(ssttparrangement.routes.ArrangementController.determineEligibility().absoluteURL()),
        "failureURL" -> Seq(ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled().absoluteURL())
      )
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

