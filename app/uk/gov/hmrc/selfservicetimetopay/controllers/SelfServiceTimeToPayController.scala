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

  def actionCallUs: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, taxpayer,  _, _, _, _)) =>
        Ok(call_us(typeOfTaxNumber = true, loggedIn = isSignedIn))
      case _ => Ok(call_us(isSignedIn))
    }
  }

  def getTtpCallUs: Action[AnyContent] = actionCallUs
  def getTtpCallUsTypeOfTax: Action[AnyContent] = actionCallUs
  def getTtpCallUsExistingTTP: Action[AnyContent] = actionCallUs
  def getTtpCallUsCalculatorInstalments: Action[AnyContent] = actionCallUs
  def getTtpCallUsSignInQuestion: Action[AnyContent] = actionCallUs

  def getYouNeedToFile: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _)) => Ok(you_need_to_file(sa.debits,isSignedIn))
      case _ => Ok(service_start(isSignedIn))
    }
  }

  def getUnavailable: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData: TTPSubmission) => Ok(unavailable(isSignedIn))
      case _ => Ok(unavailable(isSignedIn))
    }
  }

  def signOut(continueUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.remove().map(_ => Redirect(SsttpFrontendConfig.logoutUrl).withNewSession)
  }
}
