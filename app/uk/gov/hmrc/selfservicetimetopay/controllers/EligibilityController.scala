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
import uk.gov.hmrc.selfservicetimetopay.forms.EligibilityForm
import views.html.selfservicetimetopay.eligibility._

import scala.concurrent.Future

object EligibilityController extends FrontendController{

  def present:Action[AnyContent] = Action { implicit request =>
    Redirect(routes.EligibilityController.typeOfTaxPresent())
  }

  def typeOfTaxPresent:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(type_of_tax_form.render(EligibilityForm.typeOfTaxForm, request)))
  }

  def typeOfTaxSubmit:Action[AnyContent] = Action.async { implicit request =>
    val response = EligibilityForm.typeOfTaxForm.bindFromRequest().fold(
      formWithErrors => {
        BadRequest(views.html.selfservicetimetopay.eligibility.type_of_tax_form(formWithErrors))
      },
      validFormData => {
        //keyStoreConnector.saveFormData(KeystoreKeys.typeOfTaxDetails, validFormData)
        Redirect(routes.EligibilityController.existingTtpPresent())
      }
    )
    Future.successful(response)
  }

  def existingTtpPresent:Action[AnyContent] =  Action.async { implicit request =>
    // get form data from keystore or fill then display page
    Future.successful(Ok(existing_ttp.render(EligibilityForm.existingTtpForm, request)))
  }

  def existingTtpSubmit:Action[AnyContent] = Action.async { implicit request =>
    val response = EligibilityForm.existingTtpForm.bindFromRequest().fold(
      formWithErrors => {
        BadRequest(views.html.selfservicetimetopay.eligibility.existing_ttp(formWithErrors))
      },
      validFormData => {
        //keyStoreConnector.saveFormData(KeystoreKeys.existingTtpDetails, validFormData)
        Redirect(routes.CalculatorController.present())
      }
    )
    Future.successful(response)
  }
}