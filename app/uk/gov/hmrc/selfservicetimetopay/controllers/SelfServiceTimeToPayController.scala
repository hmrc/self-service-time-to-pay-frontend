/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.controllers
import javax.inject._

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.core._

class SelfServiceTimeToPayController @Inject() (val messagesApi: play.api.i18n.MessagesApi) extends TimeToPayController with play.api.i18n.I18nSupport {

  def start: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData: TTPSubmission) => Ok(service_start(isSignedIn))
      case _ => Ok(service_start(isSignedIn))
    }
  }

  def submit: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.determineEligibility())
  }

  def actionCallUsInEligibily: Action[AnyContent] = Action { implicit request =>
        Ok(call_us(loggedIn = isSignedIn))
  }

  def getTtpCallUs: Action[AnyContent] = actionCallUsInEligibily
  def getTtpCallUsTypeOfTax: Action[AnyContent] = actionCallUsInEligibily
  def getTtpCallUsExistingTTP: Action[AnyContent] = actionCallUsInEligibily
  def getTtpCallUsCalculatorInstalments: Action[AnyContent] = actionCallUsInEligibily
  def getTtpCallUsSignInQuestion: Action[AnyContent] = actionCallUsInEligibily
  def getIaCallUse: Action[AnyContent] = actionCallUsInEligibily


  def getOverTenThousandCallUs: Action[AnyContent] =  Action { implicit request =>
    Ok(over_ten_thosand(isSignedIn))
  }

  def getYouNeedToFile: Action[AnyContent] = Action { implicit request =>
    Ok(you_need_to_file(isSignedIn))
  }

  def getNotSaEnrolled: Action[AnyContent] = Action { implicit request =>
    Ok(not_enrolled(isSignedIn))
  }

  def signOut(continueUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.remove().map(_ => Redirect(SsttpFrontendConfig.logoutUrl).withNewSession)
  }
}
