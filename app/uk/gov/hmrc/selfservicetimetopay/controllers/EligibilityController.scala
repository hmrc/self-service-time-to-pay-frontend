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

package uk.gov.hmrc.selfservicetimetopay.controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.selfservicetimetopay.eligibility._
import play.api.data.Form
import play.api.data.Forms.{tuple, boolean, optional, mapping}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import scala.concurrent.Future
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityExistingTTP, EligibilityTypeOfTax}

object EligibilityController extends FrontendController{

  def atLeastOneRequired: Constraint[(Boolean, Boolean)] = Constraint[(Boolean, Boolean)]("constraint.required") { data  =>
    if (!data._1 && !data._2) Invalid(ValidationError("ssttp.eligibility.form.type_of_tax.required")) else Valid
  }

  private def createTypeOfTaxForm:Form[EligibilityTypeOfTax] = {
    Form(mapping(
      "type_of_tax" -> tuple(
        "hasSelfAssessmentDebt" -> boolean,
        "hasOtherDebt" -> boolean
      ).verifying(atLeastOneRequired)
    )((type_of_tax) => {
      EligibilityTypeOfTax(type_of_tax._1, type_of_tax._2)
    })
      ((type_of_tax:EligibilityTypeOfTax) => Some(type_of_tax.hasSelfAssessmentDebt, type_of_tax.hasOtherDebt))
    )
  }

  private def createExistingTtpForm:Form[EligibilityExistingTTP] = {
    Form(mapping(
      "hasExistingTTP" -> optional(boolean).verifying("ssttp.eligibility.form.existing_ttp.required", _.nonEmpty)
    )(EligibilityExistingTTP.apply)(EligibilityExistingTTP.unapply))
  }

  def present:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.EligibilityController.typeOfTaxPresent()))
  }

  def typeOfTaxPresent:Action[AnyContent] = Action.async { implicit request =>
    val form = createTypeOfTaxForm
    Future.successful(Ok(type_of_tax_form.render(form, request)))
  }

  def typeOfTaxSubmit:Action[AnyContent] = Action.async { implicit request =>
    val form = createTypeOfTaxForm.bindFromRequest()
    if(form.hasErrors) {
      Future.successful(Ok(type_of_tax_form.render(form, request)))
    } else {
      if (form.get.hasOtherDebt) {
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.ttpCallUsPresent()))
      } else {
        Future.successful(Redirect(routes.EligibilityController.existingTtpPresent()))
      }
    }
  }


  def existingTtpPresent:Action[AnyContent] =  Action.async { implicit request =>
    val form = createExistingTtpForm
    Future.successful(Ok(existing_ttp.render(form, request)))
  }

  def existingTtpSubmit:Action[AnyContent] = Action.async { implicit request =>
    val form = createExistingTtpForm.bindFromRequest()
    if(form.hasErrors) {
      Future.successful(Ok(existing_ttp.render(form, request)))
    } else {
      if (form.get.hasExistingTTP.get) {
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.ttpCallUsPresent()))
      } else {
        Future.successful(Redirect(routes.CalculatorController.present()))
      }
    }
  }
}