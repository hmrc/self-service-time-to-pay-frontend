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

  private def getKeystoreData(implicit request:Request[AnyContent]) : (Boolean, Option[CalculatorAmountsDue], Option[CalculatorPaymentToday], Option[CalculatorDuration]) = {
    val ssttpStart = request.session.get("SelfserviceTimeToPayStart").isDefined
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
    (ssttpStart, amountsDue, paymentToday, duration)
  }



  def present: Action[AnyContent] = Action.async { (request =>
    Future.successful(Redirect(routes.CalculatorController.amountsDuePresent()))
  }

  def amountsDuePresent: Action[AnyContent] = Action.async { implicit request =>
    request.session.get("SelfServiceTimeToPayStart") match {
      case Some(String) => {
        val calculatorAmountsDue:CalculatorAmountsDue = request.session.get("CalculatorAmountsDue") match {
          case Some(calculatorAmountsDueJson) => {
            JacksonMapper.readValue(calculatorAmountsDueJson, classOf[CalculatorAmountsDue])
          }
          case None => {
            CalculatorAmountsDue(Seq.empty)
          }
        }
        Future.successful(Ok(amounts_due_form.render(calculatorAmountsDue, createAmountDueForm, request)))
      }
      case None => {
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.present()))
      }
    }
  }

    def amountsDueSubmit: Action[AnyContent] = Action.async { implicit request =>
      val form = createAmountDueForm.bindFromRequest()
      if (form.hasErrors) {
        Future.successful(Ok(amounts_due_form.render(formAmountsDue, form, request)))
      } else if (form.get.amount.compare(BigDecimal("32.00")) < 0) {
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.youNeedToFilePresent()))
      } else {
        Future.successful(Redirect(routes.CalculatorController.paymentTodayPresent()).withSession(
          "CalculatorAmountsDue" -> JacksonMapper.writeValueAsString(form.get)
        ))
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
      Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
        "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(form.get)
      ))
    }
  }

  def calculateInstalmentsPresent: Action[AnyContent] = Action.async { implicit request =>
    request.session.get("CalculatorPaymentToday") match {
      case Some(calculatorPaymentTodayJson) => {
        val calculatorPaymentToday: CalculatorPaymentToday = JacksonMapper.readValue(calculatorPaymentTodayJson, classOf[CalculatorPaymentToday])
        request.session.get("CalculatorPaymentSchedules") match {
          case Some(calculatorPaymentSchedulesJson) => {
            val calculatorPaymentSchedules = JacksonMapper.readValue(calculatorPaymentSchedulesJson, classOf[Seq[CalculatorPaymentSchedule]])
            val instalmentOptionsAscending = calculatorPaymentSchedules.map(_.instalments.length).sorted

            val calculatorDuration = request.session.get("CalculatorDuration") match {
              case Some(calculatorDurationJson) => {
                JacksonMapper.readValue(request.session("CalculatorDuration"), classOf[CalculatorDuration])
              }
              case None => {
                CalculatorDuration(instalmentOptionsAscending.head)
              }
            }
            Future.successful(Ok(calculate_instalments_form.render(paymentSchedules.filter(_.instalments.length == calculatorDuration.months).head,
              createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last), createPaymentTodayForm.fill(calculatorPaymentToday),
              instalmentOptionsAscending, request)))
          }
          case None => {
            Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
              "CalculatorPaymentSchedules" -> JacksonMapper.writeValueAsString(paymentSchedules)
            ))
          }
        }
      }
      case None => {
        Future.successful(Redirect(routes.CalculatorController.paymentTodayPresent()))
      }
    }
  }

  def calculateInstalmentsSubmit: Action[AnyContent] = Action.async { implicit request =>
    request.session.get("CalculatorPaymentToday") match {
      case Some(calculatorPaymentTodayJson) => {
        val calculatorPaymentToday: CalculatorPaymentToday = JacksonMapper.readValue(calculatorPaymentTodayJson, classOf[CalculatorPaymentToday])
        request.session.get("CalculatorPaymentSchedules") match {
          case Some(calculatorPaymentSchedulesJson) => {
            val calculatorPaymentSchedules = JacksonMapper.readValue(calculatorPaymentSchedulesJson, classOf[Seq[CalculatorPaymentSchedule]])
            val instalmentOptionsAscending = calculatorPaymentSchedules.map(_.instalments.length).sorted

            val calculatorDurationForm = createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()
            if (calculatorDurationForm.hasErrors) {
              val calculatorPaymentSchedules: Seq[CalculatorPaymentSchedule] = JacksonMapper.readValue(calculatorPaymentSchedulesJson, classOf[Seq[CalculatorPaymentSchedule]])
              val instalmentOptionsAscending = calculatorPaymentSchedules.map(_.instalments.length).sorted
              val calculatorDuration: CalculatorDuration = request.session.get("CalculatorDuration") match {
                case Some(calculatorDurationJson) => JacksonMapper.readValue(calculatorDurationJson, classOf[CalculatorDuration])
                case None => CalculatorDuration(calculatorPaymentSchedules.head.instalments.length)
              }

              Future.successful(Ok(calculate_instalments_form.render(paymentSchedules.filter(_.instalments.length == calculatorDuration.months).head,
                calculatorDurationForm, createPaymentTodayForm.fill(calculatorPaymentToday), instalmentOptionsAscending, request)))
            } else {
              Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
                "CalculatorDuration" -> JacksonMapper.writeValueAsString(calculatorDurationForm.get)
              ))
            }
          }
          case None => {
            Future.successful(Redirect(routes.CalculatorController.paymentTodayPresent()))
          }
      }
    }
  }

  def calculateInstalmentsPaymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>
        request.session.get("CalculatorPaymentSchedules") match {
          case Some(calculatorPaymentSchedulesJson) => {
            val calculatorPaymentSchedules = JacksonMapper.readValue(calculatorPaymentSchedulesJson, classOf[Seq[CalculatorPaymentSchedule]])
            val instalmentOptionsAscending = calculatorPaymentSchedules.map(_.instalments.length).sorted

            val calculatorPaymentTodayForm = createPaymentTodayForm.bindFromRequest()
            if (calculatorPaymentTodayForm.hasErrors) {
              val calculatorPaymentSchedules: Seq[CalculatorPaymentSchedule] = JacksonMapper.readValue(calculatorPaymentSchedulesJson, classOf[Seq[CalculatorPaymentSchedule]])
              val instalmentOptionsAscending = calculatorPaymentSchedules.map(_.instalments.length).sorted
              val calculatorDuration: CalculatorDuration = request.session.get("CalculatorDuration") match {
                case Some(calculatorDurationJson) => JacksonMapper.readValue(calculatorDurationJson, classOf[CalculatorDuration])
                case None => CalculatorDuration(calculatorPaymentSchedules.head.instalments.length)
              }

              Future.successful(Ok(calculate_instalments_form.render(paymentSchedules.filter(_.instalments.length == calculatorDuration.months).head,
                calculatorDurationForm, calculatorPaymentTodayForm, instalmentOptionsAscending, request)))
            } else {
          else {
          Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).withSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString())
            )
          }
            }
          }
          case None => {
            request.session.get("CalculatorPaymentToday") match {
              case Some(calculatorPaymentTodayJson) => {
                Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()))
              }
            }
          }
        }
      }

  }
}