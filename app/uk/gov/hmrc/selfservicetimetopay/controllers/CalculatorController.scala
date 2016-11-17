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
      "dueByYear" -> number(min = 2000, max = 2100),
      "dueByMonth" -> nonEmptyText,
      "dueByDay" -> number(min = 1, max = 31)
    )(CalculatorAmountDue.apply)(CalculatorAmountDue.unapply))
  }


  private def createPaymentTodayForm(totalDue:BigDecimal): Form[CalculatorPaymentToday] = {
    Form(mapping(
      "amount" -> optional(bigDecimal
        .verifying("ssttp.calculator.form.payment_today.amount.less-than-owed", _ < totalDue)
        .verifying("ssttp.calculator.form.payment_today.amount.nonnegitive", _.compare(BigDecimal("0")) >= 0)
    )
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
      case Some(json) => Some(generatePaymentSchedules(amountsDue.get.total, paymentToday.get.amount))
      /*Option(JacksonMapper.readValue(json, classOf[Seq[CalculatorPaymentSchedule]]))*/
      case None => None
    }
    (ssttpStart, amountsDue, paymentToday, duration, schedules)
  }

  private def paymentScheduleMatches(paymentSchedule: CalculatorPaymentSchedule, amountsDue:CalculatorAmountsDue, paymentToday:CalculatorPaymentToday):Boolean = {
    (paymentSchedule.amountToPay.compare(amountsDue.total) == 0) && (paymentSchedule.initialPayment.compare(paymentToday.amount.get) == 0)
  }

  private def getRedirectionDestination(keystoreData:(Boolean, Option[CalculatorAmountsDue], Option[CalculatorPaymentToday], Option[CalculatorDuration], Option[Seq[CalculatorPaymentSchedule]])): Result = {
    keystoreData match {
      case (false, _, _, _, _) => {
        Redirect(routes.SelfServiceTimeToPayController.present)
      }
      case (true, None, _, _, _) => {
        Redirect(routes.CalculatorController.amountsDuePresent())
      }
      case (true, a:Some[CalculatorAmountsDue], None, _, _) => {
        Redirect(routes.CalculatorController.paymentTodayPresent())
      }
      case (true, a:Some[CalculatorAmountsDue], p:Some[CalculatorPaymentToday], _, _) => {
        Redirect(routes.CalculatorController.calculateInstalmentsPresent())
      }
      case theRest => Redirect(routes.CalculatorController.paymentTodayPresent())
    }
  }

  def present: Action[AnyContent] = Action.async { request =>
    Future.successful(Redirect(routes.CalculatorController.amountsDuePresent()))
  }

  def amountsDuePresent: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(getKeystoreData match {

      case (_, amountsDue:Option[CalculatorAmountsDue], _, _, _) => {
        Ok(amounts_due_form.render(amountsDue.getOrElse(CalculatorAmountsDue(Seq.empty)), createAmountDueForm, request))
      }

      case other => getRedirectionDestination(other)
    })
  }

  def amountsDueSubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, amountsDue:Option[CalculatorAmountsDue], _, _, _) => {
        val form = createAmountDueForm.bindFromRequest()
        if (form.hasErrors) {
          Ok(amounts_due_form.render(formAmountsDue, form, request))
        } else if (form.get.amount.compare(BigDecimal("32.00")) < 0) {
          Redirect(routes.SelfServiceTimeToPayController.youNeedToFilePresent())
        } else {
          val newAmounts = (amountsDue match {
            case Some(amounts: CalculatorAmountsDue) => amounts.amountsDue
            case None => Seq.empty
          }) :+ form.get
          Redirect(routes.CalculatorController.amountsDuePresent()).addingToSession(
            "CalculatorAmountsDue" -> JacksonMapper.writeValueAsString(CalculatorAmountsDue(newAmounts))
          )
        }
      }

      case other => getRedirectionDestination(other)

    })
  }

  def paymentTodayPresent: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue:CalculatorAmountsDue), paymentTodayOption: Option[CalculatorPaymentToday], _, _) => {
        val form = paymentTodayOption match {
          case Some(paymentToday:CalculatorPaymentToday) => createPaymentTodayForm(amountsDue.total).fill(paymentToday)
          case None => createPaymentTodayForm(amountsDue.total)
        }
        Ok(payment_today_form.render(form, request))
      }

      case other => getRedirectionDestination(other)

    })
  }


  def paymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, amountsDue:Some[CalculatorAmountsDue], _, _, _) => {
        val form = createPaymentTodayForm(amountsDue.get.total).bindFromRequest()
        if (form.hasErrors) {
          Ok(payment_today_form.render(form, request))
        } else {
          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).addingToSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(form.get)
          )
        }
      }

      case other => getRedirectionDestination(other)
    })
  }

  def calculateInstalmentsPresent: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue:CalculatorAmountsDue), Some(paymentToday:CalculatorPaymentToday),
                      durationOption:Option[CalculatorDuration], schedulesOption:Option[List[CalculatorPaymentSchedule]]) => {
        schedulesOption match {
          case Some(schedules:Seq[CalculatorPaymentSchedule]) => {
            val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
            val duration = durationOption match {
              case Some(d:CalculatorDuration) => d
              case None => CalculatorDuration(instalmentOptionsAscending.head)
            }

            Ok(calculate_instalments_form.render(
              schedules.filter(_.instalments.length == duration.months).head,
              createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fill(duration),
              createPaymentTodayForm(amountsDue.total).fill(paymentToday),
              instalmentOptionsAscending, request)
            )
          }
          case None => {
           /* for {
               Some(schedulesList) <- CalculatorConnector.submitLiabilities(CalculatorInput(
                liabilities = amountsDue.get.amountsDue.map(amountDue =>
                  CalculatorLiability("", amountDue.amount, BigDecimal("0"), amountDue.getDueBy(), Some(amountDue.getDueBy()))),
                initialPayment = paymentToday.get.amount.getOrElse(BigDecimal("0")),
                startDate = LocalDate.now,
                endDate = LocalDate.now.plusMonths(11),
                paymentFrequency = "MONTHLY"))

              result <- Future.successful(Redirect(routes.CalculatorController.calculateInstalmentsPresent()).addingToSession(
                "CalculatorPaymentSchedules" -> JacksonMapper.writeValueAsString(schedulesList)
              ))
            } yield result*/
            Redirect(routes.CalculatorController.calculateInstalmentsPresent()).addingToSession(
                "CalculatorPaymentSchedules" -> "*"
            )
          }
        }
      }

      case other => getRedirectionDestination(other)
    })
  }

  def calculateInstalmentsSubmit: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue:CalculatorAmountsDue), Some(paymentToday:CalculatorPaymentToday), durationOption:Option[CalculatorDuration], Some(schedules:List[CalculatorPaymentSchedule])) => {
        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
        val calculatorDurationForm = createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()

        if (calculatorDurationForm.hasErrors) {
         val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
         val duration = durationOption match {
           case Some(d:CalculatorDuration) => d
           case None => CalculatorDuration(instalmentOptionsAscending.head)
         }

         Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
            calculatorDurationForm, createPaymentTodayForm(amountsDue.total).fill(paymentToday), instalmentOptionsAscending, request))
        }
        else {
          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).addingToSession(
            "CalculatorDuration" -> JacksonMapper.writeValueAsString(calculatorDurationForm.get)
          )
        }
      }

      case other => getRedirectionDestination(other)
    })
  }

  def calculateInstalmentsPaymentTodaySubmit: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountDue:CalculatorAmountsDue), _, Some(duration:CalculatorDuration), Some(schedules:List[CalculatorPaymentSchedule])) => {

        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
        val calculatorPaymentTodayForm = createPaymentTodayForm(amountDue.total).bindFromRequest()

        if (calculatorPaymentTodayForm.hasErrors) {
          val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted

          Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
            createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fill(duration),
              calculatorPaymentTodayForm, instalmentOptionsAscending, request))
        } else {
          Redirect(routes.CalculatorController.calculateInstalmentsPresent()).addingToSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(calculatorPaymentTodayForm.get)
          ).removingFromSession("CalculatorPaymentSchedules")
        }
      }

      case other => getRedirectionDestination(other)
    })
  }
}