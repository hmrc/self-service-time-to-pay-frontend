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
import uk.gov.hmrc.selfservicetimetopay.controllerVariables
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

object CalculatorController extends FrontendController {

  private def createAmountDueForm: Form[CalculatorAmountDue] = {
    Form(mapping(
      "amount" -> bigDecimal,
      "dueByDay" -> number(min = 1, max = 31),
      "dueByMonth" -> nonEmptyText,
      "dueByYear" -> number(min = 2000, max = 2100)
    )(CalculatorAmountDue.apply)(CalculatorAmountDue.unapply))
  }

  private def createPaymentTodayForm: Form[CalculatorPaymentToday] = {
    Form(mapping(
      "amount" -> optional(bigDecimal)
    )(CalculatorPaymentToday.apply)(CalculatorPaymentToday.unapply))
  }

  private def createDurationForm(min: Integer, max: Integer): Form[CalculatorDuration] = {
    Form(mapping(
      "months" -> number(min = min, max = max)
    )(CalculatorDuration.apply)(CalculatorDuration.unapply))
  }


  def present: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.CalculatorController.amountsDuePresent()).withSession(
      "CalculatorDuration" -> paymentSchedules.map(_.instalments.length).min.toString
    ))
  }

  def amountsDuePresent: Action[AnyContent] = Action.async { implicit request =>
    val form = createAmountDueForm
    Future.successful(Ok(amounts_due_form.render(formAmountsDue, form, request)))
  }

  def amountsDueSubmit: Action[AnyContent] = Action.async { implicit request =>
    val form = createAmountDueForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(amounts_due_form.render(formAmountsDue, form, request)))
    } else if (form.get.amount.compare(BigDecimal("32.00")) < 0) {
      Future.successful(Redirect(routes.SelfServiceTimeToPayController.youNeedToFilePresent()))
    } else {
      Future.successful(Redirect(routes.CalculatorController.paymentTodayPresent()))
    }
  }

  def paymentTodayPresent: Action[AnyContent] = Action.async { implicit request =>
    val form = createPaymentTodayForm
    Future.successful(Ok(payment_today_form.render(form, request)))
  }

  def paymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>
    val form = createPaymentTodayForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(payment_today_form.render(form, request)))
    } else {
      Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()))
    }
  }

  def calculateInstalmentsPresent: Action[AnyContent] = Action.async { implicit request =>
    //@TODO replace with keysotre
    val calculatorDuration:CalculatorDuration = CalculatorDuration(request.session.get("CalculatorDuration").map(_.toInt)
      .getOrElse(paymentSchedules.minBy(_.instalments.length).instalments.length))
    val form = createDurationForm(paymentSchedules.minBy(_.instalments.length).instalments.length,
      paymentSchedules.maxBy(_.instalments.length).instalments.length).fill(calculatorDuration)

    Future.successful(Ok(calculate_instalments_form.render(paymentSchedules.filter(_.instalments.length == calculatorDuration.months).head, form,
      paymentSchedules.map(_.instalments.length).sorted, request)))
  }

  def calculateInstalmentsSubmit: Action[AnyContent] = Action.async { implicit request =>
    val form = createDurationForm(paymentSchedules.minBy(_.instalments.length).instalments.length, paymentSchedules.maxBy(_.instalments.length).instalments.length).bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(calculate_instalments_form.render(paymentSchedules.last, form, paymentSchedules.map(_.instalments.length).sorted, request)))
    } else {
      Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
        "CalculatorDuration" -> form.get.months.toString) //@TODO replace session with keystore
      )
    }
  }
}