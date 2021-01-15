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

package ssttpeligibility

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import enrolforsa.AddTaxesConnector
import javax.inject._
import journey.JourneyService
import play.api.mvc._
import req.RequestSupport
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SelfServiceTimeToPayController @Inject() (
    mcc:               MessagesControllerComponents,
    submissionService: JourneyService,
    as:                Actions,
    views:             Views,
    requestSupport:    RequestSupport,
    clockProvider:     ClockProvider,
    addTaxConnector:   AddTaxesConnector)(implicit appConfig: AppConfig,
                                          ec: ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._

  def start: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.service_start(isSignedIn, mcc.messagesApi))
  }

  def submit: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Redirect(ssttparrangement.routes.ArrangementController.determineEligibility())
  }

  def actionCallUsInEligibility: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.call_us(isWelsh, loggedIn = isSignedIn))
  }

  def getTtpCallUs: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsTypeOfTax: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsExistingTTP: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsCalculatorInstalments: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsSignInQuestion: Action[AnyContent] = actionCallUsInEligibility
  def getIaCallUse: Action[AnyContent] = actionCallUsInEligibility

  def getDebtTooLarge: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.debt_too_large(isSignedIn, isWelsh))
  }

  def getYouNeedToFile: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.you_need_to_file(isSignedIn))
  }

  def getNotSaEnrolled: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.not_enrolled(isWelsh, isSignedIn))
  }

  def getAccessYouSelfAssessmentOnline: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Ok(views.you_need_to_request_access_to_self_assessment(isWelsh, isSignedIn))
  }

  def submitAccessYouSelfAssessmentOnline: Action[AnyContent] = as.authenticatedSaUser.async { implicit request =>
    val logMessage = s"Sending user to BTA for Identify Verification " +
      s"[ConfidenceLevel=${request.confidenceLevel}]" +
      s"[utr=${request.maybeUtr.map(_.obfuscate)}]"
    JourneyLogger.info(logMessage)
    val credentials = request.credentials.getOrElse(throw new RuntimeException("Missing 'credentials' auth credentials."))
    val resultF: Future[Result] = for {
      startIdentityVerificationJourneyResult <- addTaxConnector.startEnrolForSaJourney(request.maybeUtr, credentials)
      redirectUrl = startIdentityVerificationJourneyResult.redirectUrl
    } yield Redirect(redirectUrl)

    resultF.recover {
      case NonFatal(ex) =>
        JourneyLogger.error(s"[Failed] $logMessage", ex)
        throw ex
    }
  }

  def signOut(continueUrl: Option[String]): Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"$request")
    Redirect(appConfig.logoutUrl).withNewSession
  }
}
