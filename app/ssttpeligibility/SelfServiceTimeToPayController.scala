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

package ssttpeligibility

import config.AppConfig
import controllers.FrontendController
import controllers.action.Actions
import javax.inject._
import play.api.mvc._
import sttpsubmission.SubmissionService
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.Views
import views.html.core._

import scala.concurrent.ExecutionContext

class SelfServiceTimeToPayController @Inject() (
    mcc:               MessagesControllerComponents,
    submissionService: SubmissionService,
    as:                Actions,
    views:             Views)(implicit appConfig: AppConfig,
                              ec: ExecutionContext
) extends FrontendController(mcc) {

  def start: Action[AnyContent] = as.checkSession.async { implicit request =>
    submissionService.getTtpSessionCarrier.map { _ => Ok(views.service_start(isSignedIn, mcc.messagesApi)) }
  }

  def submit: Action[AnyContent] = as.checkSession { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.determineEligibility())
  }

  def actionCallUsInEligibility: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.call_us(isWelsh, loggedIn = isSignedIn))
  }

  def getTtpCallUs: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsTypeOfTax: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsExistingTTP: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsCalculatorInstalments: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsSignInQuestion: Action[AnyContent] = actionCallUsInEligibility
  def getIaCallUse: Action[AnyContent] = actionCallUsInEligibility

  def getDebtTooLarge: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.debt_too_large(isSignedIn, isWelsh))
  }

  def getYouNeedToFile: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.you_need_to_file(isSignedIn))
  }

  def getNotSaEnrolled: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.not_enrolled(isWelsh, isSignedIn))
  }

  def signOut(continueUrl: Option[String]): Action[AnyContent] = as.checkSession.async { implicit request =>
    submissionService.remove().map(_ => Redirect(appConfig.logoutUrl).withNewSession)
  }
}
