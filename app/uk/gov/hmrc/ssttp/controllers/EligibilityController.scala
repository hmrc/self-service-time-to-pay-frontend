/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.ssttp.controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.eligibility._
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}

import scala.concurrent.Future
import uk.gov.hmrc.ssttp.models.{EligibilityDebtType, EligibilityExistingTTP}

object EligibilityController extends FrontendController{

  def present = Action.async { implicit request =>
    Future.successful(Redirect(routes.EligibilityController.debtTypePresent()))
  }

  def submit = Action.async { implicit request =>
    Future.successful(Redirect(routes.SelfServiceTimeToPayController.present()))
  }

  def createDebtTypeForm: Form[EligibilityDebtType] = {
    Form(mapping(
      "hasSelfAssessmentDebt" -> boolean,
      "hasOtherTaxDebt" -> boolean
    )(EligibilityDebtType.apply)(EligibilityDebtType.unapply))
  }

  def createExistingTtpForm: Form[EligibilityExistingTTP] = {
    Form(mapping(
      "hasExistingTTP" -> optional(boolean).verifying(has => has.nonEmpty)
    )(EligibilityExistingTTP.apply)(EligibilityExistingTTP.unapply))
  }

  def debtTypePresent =  Action.async { implicit request =>
    val form = createDebtTypeForm
    Future.successful(Ok(debt_type.render(form, request)))
  }

  def debtTypeSubmit = Action.async { implicit request =>
    val form = createDebtTypeForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(debt_type.render(form, request)))
    } else {
      Future.successful(Redirect(routes.EligibilityController.existingTtpPresent()))
    }
  }

  def existingTtpPresent =  Action.async { implicit request =>
    val form = createExistingTtpForm
    Future.successful(Ok(existing_ttp.render(form, request)))
  }

  def existingTtpSubmit = Action.async { implicit request =>
    val form = createExistingTtpForm.bindFromRequest()
    Future.successful(Ok(existing_ttp.render(form, request)))
  }
}