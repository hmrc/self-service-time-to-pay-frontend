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


import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.SessionCacheConnector

class PaymentTodayController extends TimeToPayController {

//  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>
//    Future.successful(Ok(views.html.selfservicetimetopay.calculator.payment_today_form.render(CalculatorForm.paymentTodayForm, request)))
//  }
//
//  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
//    val response = CalculatorForm.paymentTodayForm.bindFromRequest().fold(
//      formWithErrors => {
//        BadRequest(views.html.selfservicetimetopay.calculator.payment_today_form(formWithErrors))
//      },
//      validFormData => {
//
//        //call taxpayer service
//        Redirect(routes.CalculateInstallmentsController.getCalculateInstalments())
//      }
//    )
//    Future.successful(response)
//  }
}