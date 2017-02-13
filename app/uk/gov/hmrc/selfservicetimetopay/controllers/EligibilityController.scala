/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.mvc._
import play.api.Logger
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.EligibilityForm
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityExistingTTP, EligibilityTypeOfTax, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.core.sign_in_question
import views.html.selfservicetimetopay.eligibility._

import scala.concurrent.Future

class EligibilityController extends TimeToPayController {

  def start: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.EligibilityController.getTypeOfTax())
  }

  def getTypeOfTax: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, _, typeOfTax@Some(_), _, _, _, _)) =>
        Ok(type_of_tax_form(EligibilityForm.typeOfTaxForm.fill(typeOfTax.get), ttpData.taxpayer.isDefined))
      case _ => Ok(type_of_tax_form(EligibilityForm.typeOfTaxForm))
    }
  }

  def submitTypeOfTax: Action[AnyContent] = Action.async { implicit request =>
    EligibilityForm.typeOfTaxForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.selfservicetimetopay.eligibility.type_of_tax_form(formWithErrors))),
      validFormData => {
        updateOrCreateInCache(found => found.copy(eligibilityTypeOfTax = Some(validFormData), durationMonths = Some(3)),
          () => TTPSubmission(eligibilityTypeOfTax = Some(validFormData), durationMonths = Some(3))).map(_ => {
          validFormData match {
            case EligibilityTypeOfTax(true, false) => Redirect(routes.EligibilityController.getExistingTtp())
            case _ => Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs())
          }
        })
      }
    )
  }

  def getExistingTtp: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, _, _, existingTtp@Some(_), _, _, _)) =>
        Ok(existing_ttp(EligibilityForm.existingTtpForm.fill(existingTtp.get), ttpData.taxpayer.isDefined))
      case _ => Ok(existing_ttp(EligibilityForm.existingTtpForm))
    }
  }

  def submitExistingTtp: Action[AnyContent] = Action.async { implicit request =>
    EligibilityForm.existingTtpForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.selfservicetimetopay.eligibility.existing_ttp(formWithErrors))),
      validFormData => {
        updateOrCreateInCache(found => found.copy(eligibilityExistingTtp = Some(validFormData), durationMonths = Some(3)),
          () => TTPSubmission(eligibilityExistingTtp = Some(validFormData), durationMonths = Some(3))).flatMap[Result](_ => {
          validFormData match {
            case EligibilityExistingTTP(Some(false)) =>
              authConnector.currentAuthority.map[Result] { authority =>
                //todo Do I leave this here?
                Redirect(routes.ArrangementController.determineMisalignment())
              }.recover { case e: Throwable =>
                Logger.info(s"${e.getMessage}")
                Redirect(routes.SelfServiceTimeToPayController.getSignInQuestion())
              }
            case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs()))
          }
        })
      })
  }
}
