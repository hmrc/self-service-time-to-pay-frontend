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

import java.time.LocalDate
import java.time.temporal.TemporalField

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement._
import uk.gov.hmrc.selfservicetimetopay.controllerVariables

import scala.concurrent.Future
import scala.util.control.Exception._

object ArrangementController extends FrontendController {


  private def createDayOfMonthForm:Form[ArrangementDayOfMonth] = {
    def tryToInt(input:String) = { catching(classOf[NumberFormatException]) opt input.toInt}
    def isInt(input:String) = { tryToInt(input).nonEmpty }
    Form(mapping(
      "dayOfMonth" -> text
        .verifying("ssttp.arrangement.instalment-summary.payment-day.required", {i:String => (i != null) && i.nonEmpty })
        .verifying("ssttp.arrangement.instalment-summary.payment-day.number", { i => (i.isEmpty || (i.nonEmpty && isInt(i))) })
        .verifying("ssttp.arrangement.instalment-summary.payment-day.out-of-range", { i => (!isInt(i) || (isInt(i) && (i.toInt >= 1)) )})
        .verifying("ssttp.arrangement.instalment-summary.payment-day.out-of-range", { i => (!isInt(i) || (isInt(i) && (i.toInt <= 28)) )})
      )(dayOfMonth => ArrangementDayOfMonth(tryToInt(dayOfMonth).get))(data => Some(data.dayOfMonth.toString))
    )
  }

  def present:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.scheduleSummaryPresent()))
  }

  def scheduleSummaryPresent:Action[AnyContent] = Action.async { implicit request =>
  val form = createDayOfMonthForm
    Future.successful(Ok(instalment_plan_summary.render(generatePaymentSchedules(BigDecimal("2000.00"), Some(BigDecimal("100.00"))).last, form, request)))
  }

  def scheduleSummaryPrintPresent:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(print_schedule_summary.render(generatePaymentSchedules(BigDecimal("2000.00"), Some(BigDecimal("100.00"))).last, request)))
  }

  def scheduleSummaryDayOfMonthSubmit:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.scheduleSummaryPresent()))
  }

  def scheduleSummarySubmit:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.DirectDebitController.directDebitPresent()))
  }

  def applicationCompletePresent:Action[AnyContent] = Action.async {implicit request =>

    // These will be populated by the results of the TTP Arrangement in the meamtime this is just dummy data
    val tempDirectDebitData = ArrangementDirectDebit("My Account Name", 1, 2, 3, 1234567890, Option(true))
    val tempPaymentSchedule = generatePaymentSchedules(BigDecimal("2000.00"), Some(BigDecimal("100.00"))).last
    val tempDebits:Seq[Debit] = Seq(
      Debit("BCD", Some(BigDecimal("6000.00")), LocalDate.of(2015, 1, 31), None),
      Debit("IN1", Some(BigDecimal("1500.00")), LocalDate.of(2015, 6, 30), None),
      Debit("IN2", Some(BigDecimal("1500.00")), LocalDate.of(2015, 1, 31), None)
    ).sortBy(_.dueDate.toEpochDay())
    // end
    Future.successful(Ok(application_complete.render(tempDebits, tempDirectDebitData, tempPaymentSchedule, request)))
  }
}