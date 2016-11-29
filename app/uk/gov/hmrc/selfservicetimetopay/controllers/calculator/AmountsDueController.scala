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

package uk.gov.hmrc.selfservicetimetopay.controllers.calculator

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models.CalculatorAmountsDue
import views.html.selfservicetimetopay.calculator.amounts_due_form

import scala.concurrent.Future

class AmountsDueController(sessionCache: SessionCacheConnector) extends FrontendController {

  def start: Action[AnyContent] = Action.async { request =>
    Future.successful(Redirect(routes.AmountsDueController.getAmountsDue()))
  }

  def getAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(amounts_due_form.render(CalculatorAmountsDue(Seq.empty), CalculatorForm.amountDueForm, request)))
  }

  def submitAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    val response = CalculatorForm.amountDueForm.bindFromRequest().fold(
      formWithErrors => {
        BadRequest(views.html.selfservicetimetopay.calculator.amounts_due_form(CalculatorAmountsDue(Seq.empty), formWithErrors))
      },
      validFormData => {
        Redirect(uk.gov.hmrc.selfservicetimetopay.controllers.routes.CalculatorController.getPaymentToday())
      }
    )
    Future.successful(response)
  }
}