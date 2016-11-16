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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.connectors.CalculatorConnector
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.util.JacksonMapper
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

//@TODO replace session with keysotre
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

  private def getKeystoreData(implicit request:Request[AnyContent]) : (Boolean, Option[CalculatorAmountsDue], Option[CalculatorPaymentToday], Option[CalculatorDuration], Option[Seq[CalculatorPaymentSchedule]]) = {
    val ssttpStart = request.session.get("SelfServiceTimeToPayStart").isDefined
    val amountsDue:Option[CalculatorAmountsDue] = request.session.get("CalculatorAmountsDue") match {
      case Some(json) => Option(JacksonMapper.readValue(json, classOf[CalculatorAmountsDue]))
      case None => None
    }
    val paymentToday:Option[CalculatorPaymentToday] = request.session.get("CalculatorPaymentToday") match {
      case Some(json) => Option(JacksonMapper.readValue(json, classOf[CalculatorPaymentToday]))
      case None => None
    }
    val duration:Option[CalculatorDuration] = request.session.get("CalculatorDuration") match {
      case Some(json) => Option(JacksonMapper.readValue(json, classOf[CalculatorDuration]))
      case None => None
    }
    val schedules:Option[Seq[CalculatorPaymentSchedule]] = request.session.get("CalculatorPaymentSchedules") match {
      case Some(json) => Option(JacksonMapper.readValue(json, classOf[Seq[CalculatorPaymentSchedule]]))
      case None => None
    }
    (ssttpStart, amountsDue, paymentToday, duration, schedules)
  }

  private def paymentScheduleMatches(paymentSchedule: CalculatorPaymentSchedule, amountsDue:CalculatorAmountsDue, paymentToday:CalculatorPaymentToday):Boolean = {
    (paymentSchedule.amountToPay.compare(amountsDue.total) == 0) && (paymentSchedule.initialPayment.compare(paymentToday.amount.get) == 0)
  }

  private def getKeystoreDataOrRedirectionDestination(target: Call)(implicit request:Request[AnyContent]) : Option[Any] = {
    val keystoreData = getKeystoreData
    def optionalRedirectOrKeystoreData(redirect:Result):Option[Any] = {
      if(target.url.equals(redirect.header.headers.get("Location").get)) { Option(keystoreData) } else { Some(redirect) }
    }
    keystoreData match {
      case (false, _, _, _, _) => {
        optionalRedirectOrKeystoreData(Redirect(routes.SelfServiceTimeToPayController.present))
      }
      case (true, None, _, _, _) => {
        optionalRedirectOrKeystoreData(Redirect(routes.CalculatorController.amountsDuePresent()))
      }
      case (true, a:Some[CalculatorAmountsDue], None, _, _) => {
        optionalRedirectOrKeystoreData(Redirect(routes.CalculatorController.paymentTodayPresent()))
      }
      case (true, a:Some[CalculatorAmountsDue], p:Some[CalculatorPaymentToday], None, None) => {
        optionalRedirectOrKeystoreData(Redirect(routes.CalculatorController.calculateInstalmentsPresent()))
      }
      case (true, a:Some[CalculatorAmountsDue], p:Some[CalculatorPaymentToday], d:Option[CalculatorDuration], s:Some[Seq[CalculatorPaymentSchedule]]) => {
        if (paymentScheduleMatches(s.get.head, a.get, p.get)) {
          val duration = d match {
            case Some(d:CalculatorDuration) => d
            case None => CalculatorDuration(s.get.map(_.instalments.length).min)
          }
          Some((true, a, p, duration, s))
        } else {
          Some(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).removingFromSession("CalculatorPaymentSchedules"))
        }
      }
    }
  }

  def present: Action[AnyContent] = Action.async { request =>
    Future.successful(Redirect(routes.CalculatorController.amountsDuePresent()))
  }

  def amountsDuePresent: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.amountsDuePresent()) match {

        case Some(r:Result) => r

        case Some((_, amountsDue:Option[CalculatorAmountsDue], _, _, _)) => {
          Ok(amounts_due_form.render(amountsDue.getOrElse(CalculatorAmountsDue(Seq.empty)), createAmountDueForm, request))
        }

    })
  }

  def amountsDueSubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.amountsDuePresent()) match {

      case Some(r:Result) => r

      case Some((_, amountsDue:Option[CalculatorAmountsDue], _, _, _)) => {
        val form = createAmountDueForm.bindFromRequest()
        if (form.hasErrors) {
          Ok(amounts_due_form.render(formAmountsDue, form, request))
        } else if (form.get.amount.compare(BigDecimal("32.00")) < 0) {
          Redirect(routes.SelfServiceTimeToPayController.youNeedToFilePresent())
        } else {
          Redirect(routes.CalculatorController.amountsDuePresent()).withSession(
            "CalculatorAmountsDue" -> JacksonMapper.writeValueAsString(amountsDue.getOrElse(CalculatorAmountsDue(Seq.empty)).amountsDue ++ Seq(form.get))
          )
        }
      }

    })
  }

  def paymentTodayPresent: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.paymentTodayPresent()) match {

      case Some(r: Result) => r

      case Some((_, _, paymentTodayOption: Option[CalculatorPaymentToday], _, _)) => {
        val form = paymentTodayOption match {
          case paymentToday: Some[CalculatorPaymentToday] => createPaymentTodayForm.fill(paymentToday.get)
          case None => createPaymentTodayForm
        }
        Ok(payment_today_form.render(form, request))
      }

    })
  }


  def paymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.paymentTodaySubmit()) match {

      case Some(r: Result) => r

      case Some((_, _, _, _, _)) => {
        val form = createPaymentTodayForm.bindFromRequest()
        if (form.hasErrors) {
          Ok(payment_today_form.render(form, request))
        } else {
          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(form.get)
          )
        }
      }
    })
  }

  def calculateInstalmentsPresent: Action[AnyContent] = Action.async { implicit request =>

    getKeystoreDataOrRedirectionDestination(routes.CalculatorController.calculateInstalmentsPresent()) match {

      case Some(r: Result) => Future.successful(r)

      case Some((_, amountsDue:Some[CalculatorAmountsDue], paymentToday:Some[CalculatorPaymentToday],
                      duration:Some[CalculatorDuration], schedulesOption:Option[Seq[CalculatorPaymentSchedule]])) => {
        schedulesOption match {
          case Some(schedules:Seq[CalculatorPaymentSchedule]) => {
            val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
            Future.successful(Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.get.months).head,
              createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last), createPaymentTodayForm.fill(paymentToday.get),
              instalmentOptionsAscending, request)))
          }
          case None => {
            for {
              Some(schedulesList) <- CalculatorConnector.submitLiabilities(CalculatorInput(
                liabilities = amountsDue.get.amountsDue.map(amountDue =>
                  CalculatorLiability("", amountDue.amount, BigDecimal("0"), amountDue.getDueBy(), Some(amountDue.getDueBy()))),
                initialPayment = paymentToday.get.amount.getOrElse(BigDecimal("0")),
                startDate = LocalDate.now,
                endDate = LocalDate.now.plusMonths(duration.get.months),
                paymentFrequency = "MONTHLY"))

              result <- Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
                "CalculatorPaymentSchedules" -> JacksonMapper.writeValueAsString(schedulesList)
              ))
            } yield result
          }
        }
      }
    }
  }

  def calculateInstalmentsSubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.calculateInstalmentsSubmit()) match {

      case Some(r: Result) => r

      case Some((_, _, paymentToday:Some[CalculatorPaymentToday], duration:Some[CalculatorDuration], schedules:Some[Seq[CalculatorPaymentSchedule]])) => {
        val instalmentOptionsAscending = schedules.get.map(_.instalments.length).sorted
        val calculatorDurationForm = createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()

        if (calculatorDurationForm.hasErrors) {
         val instalmentOptionsAscending = schedules.get.map(_.instalments.length).sorted

          Ok(calculate_instalments_form.render(schedules.get.filter(_.instalments.length == duration.get.months).head,
            calculatorDurationForm, createPaymentTodayForm.fill(paymentToday.get), instalmentOptionsAscending, request))
        }
        else {

          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
            "CalculatorDuration" -> JacksonMapper.writeValueAsString(calculatorDurationForm.get)
          )
        }
      }
    })
  }

  def calculateInstalmentsPaymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreDataOrRedirectionDestination(routes.CalculatorController.calculateInstalmentsPaymentTodaySubmit()) match {

      case Some(r: Result) => r

      case Some((_, _, paymentToday:Some[CalculatorPaymentToday], duration:Some[CalculatorDuration], schedules:Some[Seq[CalculatorPaymentSchedule]])) => {
        val instalmentOptionsAscending = schedules.get.map(_.instalments.length).sorted

        val calculatorPaymentTodayForm = createPaymentTodayForm.bindFromRequest()
        if (calculatorPaymentTodayForm.hasErrors) {
          val instalmentOptionsAscending = schedules.get.map(_.instalments.length).sorted

          Ok(calculate_instalments_form.render(schedules.get.filter(_.instalments.length == duration.get.months).head,
            createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fill(duration.get),
              calculatorPaymentTodayForm, instalmentOptionsAscending, request))
        } else {
          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(calculatorPaymentTodayForm.get)
          ).removingFromSession("CalculatorPaymentSchedules")
        }
      }
    })
  }
}