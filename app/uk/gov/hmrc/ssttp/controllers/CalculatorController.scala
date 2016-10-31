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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.ssttp.models._
import views.html.calculator._
import java.math.BigDecimal
import java.time.LocalDate

import scala.concurrent.Future

object CalculatorController extends FrontendController {
  //temp data for development
  private val formAmountsDue = CalculatorAmountsDue(Seq(
    CalculatorAmountDue(new BigDecimal("200.00"), 2010, "January", 20),
    CalculatorAmountDue(new BigDecimal("100.00"), 2014, "December", 1)
  ))

  private val paymentSchedules = Seq(
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("300.00"))
      )
    ),
    CalculatorPaymentSchedule(new BigDecimal("200.00"), new BigDecimal("2000.00"), new BigDecimal("1800.00"),
      new BigDecimal("20.00"), new BigDecimal("2020.00"), Seq(
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 1, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 2, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 3, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 4, 1), new BigDecimal("300.00")),
        CalculatorPaymentScheduleInstalment(LocalDate.of(2016, 5, 1), new BigDecimal("300.00"))
      )
    )
  )
  // end


  private def createAmountDueForm:Form[CalculatorAmountDue] = {
    Form(mapping(
      "amount" -> bigDecimal,
      "dueByDay" -> number(min = 1, max = 31),
      "dueByMonth" -> nonEmptyText,
      "dueByYear" -> number(min = 2000, max = 2100)
    )(CalculatorAmountDue.apply)(CalculatorAmountDue.unapply))
  }

  private def createPaymentTodayForm:Form[CalculatorPaymentToday] = {
    Form(mapping(
      "amount" -> optional(bigDecimal)
    )(CalculatorPaymentToday.apply)(CalculatorPaymentToday.unapply))
  }

  private def createDurationForm(min:Integer, max:Integer):Form[CalculatorDuration] = {
    Form(mapping(
      "months" -> number(min=min, max=max)
    )(CalculatorDuration.apply)(CalculatorDuration.unapply))
  }


  def present:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.CalculatorController.amountsDuePresent()))
  }

  def amountsDuePresent:Action[AnyContent] = Action.async {implicit request =>
    val form = createAmountDueForm
    Future.successful(Ok(amounts_due_form.render(formAmountsDue, form, request) ) )
  }

  def amountsDueSubmit:Action[AnyContent] = Action.async { implicit request =>
    val form = createAmountDueForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(amounts_due_form.render(formAmountsDue, form, request)))
    } else {
      Future.successful(Redirect(routes.CalculatorController.paymentTodayPresent()))
    }
  }

  def paymentTodayPresent:Action[AnyContent] = Action.async {implicit request =>
    val form = createPaymentTodayForm
    Future.successful(Ok(payment_today_form.render(form, request) ) )
  }

  def paymentTodaySubmit:Action[AnyContent] = Action.async { implicit request =>
    val form = createPaymentTodayForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(payment_today_form.render(form, request)))
    } else {
      Future.successful(Redirect(routes.CalculatorController.durationPresent()))
    }
  }

  def durationPresent:Action[AnyContent] = Action.async {implicit request =>
    val form = createDurationForm(paymentSchedules.minBy(_.instalments.length).instalments.length, paymentSchedules.maxBy(_.instalments.length).instalments.length)
    Future.successful(Ok(duration_form.render(paymentSchedules.last, form, paymentSchedules.map(_.instalments.length).sorted, request) ) )
  }

  def durationSubmit:Action[AnyContent] = Action.async { implicit request =>
    val form = createPaymentTodayForm.bindFromRequest()
    if (form.hasErrors) {
      Future.successful(Ok(payment_today_form.render(form, request)))
    } else {
      Future.successful(Redirect(routes.CalculatorController.durationPresent()))
    }
  }
}