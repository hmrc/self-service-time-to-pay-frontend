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
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.EligibilityForm
import views.html.selfservicetimetopay.eligibility._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.Future

class EligibilityController extends TimeToPayController {

  def start: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.EligibilityController.getTypeOfTax())
  }

  def getTypeOfTax: Action[AnyContent] = Action.async { implicit request =>

//    sessionCache.get.map {
//      cached => if (cached.isEmpty || cached.get.eligibilityTypeOfTax.isEmpty) EligibilityForm.typeOfTaxForm
//                else EligibilityForm.typeOfTaxForm.fill(cached.get.eligibilityTypeOfTax.get)
//    }
    Future.successful(Ok(type_of_tax_form.render(EligibilityForm.typeOfTaxForm, request)))
  }

  def submitTypeOfTax: Action[AnyContent] = Action.async { implicit request =>
    val response = EligibilityForm.typeOfTaxForm.bindFromRequest().fold(
      formWithErrors => {
        BadRequest(views.html.selfservicetimetopay.eligibility.type_of_tax_form(formWithErrors))
      },
      validFormData => {
        //sessionCache.cache[EligibilityTypeOfTax](sessionCacheKey, validFormData)

        //call taxpayer service
        Redirect(routes.EligibilityController.getExistingTtp())
      }
    )
    Future.successful(response)
  }

  def getExistingTtp: Action[AnyContent] =  Action.async { implicit request =>
    //fetchAndFill[EligibilityExistingTTP](sessionCacheKey, EligibilityForm.existingTtpForm)
    Future.successful(Ok(existing_ttp.render(EligibilityForm.existingTtpForm, request)))
  }

  def submitExistingTtp: Action[AnyContent] = Action.async { implicit request =>
    val response = EligibilityForm.existingTtpForm.bindFromRequest().fold(
      formWithErrors => {
        BadRequest(views.html.selfservicetimetopay.eligibility.existing_ttp(formWithErrors))
      },
      validFormData => {
        //sessionCache.cache[EligibilityExistingTTP](sessionCacheKey, validFormData)
        Redirect(calculator.routes.AmountsDueController.start())
      }
    )
    Future.successful(response)
  }
}