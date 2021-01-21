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
import journey.Journey
import play.api.mvc.Results.Redirect
import play.api.mvc._
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import req.RequestSupport._

import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedSaUserRequest[A](val request: AuthenticatedRequestWithJourney[A], val utr: SaUtr)
  extends WrappedRequest[A](request) {

  val journey: Journey = request.journey
}

class AuthorisedSaUserAction @Inject() (
    appConfig: AppConfig,
    cc:        MessagesControllerComponents)
  extends ActionRefiner[AuthenticatedRequestWithJourney, AuthorisedSaUserRequest] {

  override protected def refine[A](request: AuthenticatedRequestWithJourney[A]): Future[Either[Result, AuthorisedSaUserRequest[A]]] = {
    implicit val r: Request[_] = request
    implicit val ec: ExecutionContext = executionContext
    val hasActiveSaEnrolment: Boolean = request.hasActiveSaEnrolment
    val maybeUtr: Option[SaUtr] = request.maybeUtr
    val isConfident: Boolean = request.confidenceLevel >= ConfidenceLevel.L200
    import req.RequestSupport._

    //this is being updated by notification from add-taxes-frontend
    //at some point if user was sent to add-taxes-frontend for enrol-for-sa journey
    val enrolledForSa = request.journey.userEnrolledForSa.getOrElse(false)

      def logFail(reason: String) = JourneyLogger.info(
        s"""
         |Authorisation failed. $reason.
         |  [hasActiveSaEnrolment: $hasActiveSaEnrolment]
         |  [enrolments: ${request.enrolments}]
         |  [utr: $maybeUtr]
         |  [ConfidenceLevel: ${request.confidenceLevel}]
         |  """.stripMargin,
        request.journey
      )

    val result: Either[Result, AuthorisedSaUserRequest[A]] =
      (hasActiveSaEnrolment, maybeUtr, isConfident) match {
        case (false, _, _) =>
          logFail("no active IR-SA enrolment, sending user to add-taxes-frontend for enrol-for-sa journey")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (_, None, _) =>
          logFail("no present UTR, sending user to add-taxes-frontend for enrol-for-sa journey")
          Left(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getAccessYouSelfAssessmentOnline()))
        case (true, Some(utr), false) =>
          if (enrolledForSa) {
            JourneyLogger.info("Confidence level is to low, but user came back from add-taxes-frontend from enrol-for-sa journey thus allowing him to proceed.", request.journey)
            Right(new AuthorisedSaUserRequest[A](request, utr))
          } else {
            logFail("Confidence level is to low, redirecting to identity-verification")
            Left(redirectToUplift(request))
          }
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
        "completionURL" -> Seq(appConfig.mdtpUpliftCompleteUrl),
        "failureURL" -> Seq(appConfig.mdtpUpliftFailureUrl)
      )
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}

