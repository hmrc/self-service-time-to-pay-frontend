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

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models.TTPSubmission
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class PaymentTodayController extends TimeToPayController {

  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, debits @ Some(_), Some(paymentToday))) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.get.map(_.amount).sum)
        Ok(payment_today_form.render(form.fill(paymentToday), request))
      case Some(TTPSubmission(_, _, _, _, _, _, debits @ Some(_),_)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.get.map(_.amount).sum)
        Ok(payment_today_form.render(form, request))
      case _ => Ok(payment_today_form.render(CalculatorForm.paymentTodayForm, request))
    }
  }

  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    // TODO: the form validation needs to be fixed!!!!
    CalculatorForm.paymentTodayForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors))),
      validFormData => updateOrCreateInCache(
        found => found.copy(paymentToday = Some(validFormData)),
        () => TTPSubmission(paymentToday = Some(validFormData))).map(_ =>
        Redirect(routes.CalculateInstalmentsController.getCalculateInstalments(Some("6")))
    ))

  }
}