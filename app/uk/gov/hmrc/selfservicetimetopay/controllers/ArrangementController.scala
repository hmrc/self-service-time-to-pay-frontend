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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement._

import scala.concurrent.Future

object ArrangementController extends FrontendController {

  private def createDirectDebitForm:Form[ArrangementDirectDebit] = {
    Form(mapping(
      "accountHolderName" -> nonEmptyText,
      "sortCode1" -> number(min = 0, max = 99),
      "sortCode2" -> number(min = 0, max = 99),
      "sortCode3" -> number(min = 0, max = 99),
      "accountNumber" -> longNumber(min = 0, max = 999999999),
      "confirmed" -> optional(boolean)
    )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply))
  }

  private def createDayOfMonthForm:Form[ArrangementDayOfMonth] = {
    Form(mapping(
      "dayOfMonth" -> number(min=0, max=28)
    )(ArrangementDayOfMonth.apply)(ArrangementDayOfMonth.unapply))
  }

  private def createDurationForm(min:Integer, max:Integer):Form[CalculatorDuration] = {
    Form(mapping(
      "months" -> number(min=min, max=max)
    )(CalculatorDuration.apply)(CalculatorDuration.unapply))
  }

  def present:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.directDebitPresent()))
  }

  def scheduleSummaryPresent:Action[AnyContent] = Action.async { implicit request =>
  val form = createDayOfMonthForm
    Future.successful(Ok(schedule_summary.render(paymentSchedules.last, form, request)))
  }

  def scheduleSummaryPrintPresent:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(print_schedule_summary.render(paymentSchedules.last, request)))
  }

  def scheduleSummaryDayOfMonthSubmit:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.directDebitPresent()))
  }

  def scheduleSummarySubmit:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.directDebitPresent()))
  }

  def directDebitPresent:Action[AnyContent] = Action.async {implicit request =>
    val form = createDirectDebitForm
    Future.successful(Ok(direct_debit_form.render(form, request) ) )
  }

  def directDebitSubmit:Action[AnyContent] = Action.async {implicit request =>
    val form = createDirectDebitForm.bindFromRequest()
    Future.successful(Ok(direct_debit_form.render(form, request) ) )
  }
}