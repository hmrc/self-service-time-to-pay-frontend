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
import controllers.action.Actions
import controllers.{FrontendController, routes}
import javax.inject._
import play.api.i18n.I18nSupport
import play.api.mvc._
import sttpsubmission.SubmissionService
import views.html.selfservicetimetopay.core._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class SelfServiceTimeToPayController @Inject() (
    i18nSupport:       I18nSupport,
    submissionService: SubmissionService,
    as:                Actions

)(implicit appConfig: AppConfig)
  extends FrontendController {

  import i18nSupport._

  def start: Action[AnyContent] = as.checkSessionAction.async { implicit request =>
    submissionService.getTtpSessionCarrier.map { _ => Ok(service_start(isSignedIn)) }
  }

  def submit: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.determineEligibility())
  }

  def actionCallUsInEligibility: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Ok(call_us(isWelsh, loggedIn = isSignedIn))
  }

  def getTtpCallUs: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsTypeOfTax: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsExistingTTP: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsCalculatorInstalments: Action[AnyContent] = actionCallUsInEligibility
  def getTtpCallUsSignInQuestion: Action[AnyContent] = actionCallUsInEligibility
  def getIaCallUse: Action[AnyContent] = actionCallUsInEligibility

  def getDebtTooLarge: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Ok(debt_too_large(isSignedIn, isWelsh))
  }

  def getYouNeedToFile: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Ok(you_need_to_file(isSignedIn))
  }

  def getNotSaEnrolled: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Ok(not_enrolled(isWelsh, isSignedIn))
  }

  def signOut(continueUrl: Option[String]): Action[AnyContent] = as.checkSessionAction.async { implicit request =>
    submissionService.remove().map(_ => Redirect(appConfig.logoutUrl).withNewSession)
  }
}
