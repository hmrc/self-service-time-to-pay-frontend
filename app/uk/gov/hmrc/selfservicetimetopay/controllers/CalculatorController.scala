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

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.util.JacksonMapper
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculatorController extends TimeToPayController {

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
      case Some(json) => if(amountsDue.isDefined && paymentToday.isDefined) {
        Some(generatePaymentSchedules(amountsDue.get.total, paymentToday.get.amount))
      } else {
        None
      }
      case None => None
    }
    (ssttpStart, amountsDue, paymentToday, duration, schedules)
  }

  private def paymentScheduleMatches(paymentSchedule: CalculatorPaymentSchedule, amountsDue:CalculatorAmountsDue, paymentToday:CalculatorPaymentToday):Boolean = {
    (paymentSchedule.amountToPay.compare(amountsDue.total) == 0) && (paymentSchedule.initialPayment.compare(paymentToday.amount.get) == 0)
  }

  private def getRedirectionDestination(keystoreData:(Boolean, Option[CalculatorAmountsDue],
    Option[CalculatorPaymentToday], Option[CalculatorDuration], Option[Seq[CalculatorPaymentSchedule]])): Result = {
    keystoreData match {
      case (false, _, _, _, _) => Redirect(routes.SelfServiceTimeToPayController.start())

      case (true, None, _, _, _) => Redirect(calculator.routes.AmountsDueController.getAmountsDue())

      case (true, a:Some[CalculatorAmountsDue], None, _, _) => Redirect(routes.CalculatorController.getPaymentToday())

      case (true, a:Some[CalculatorAmountsDue], p:Some[CalculatorPaymentToday], _, _) =>
        Redirect(routes.CalculatorController.getCalculateInstalments(None))

      case theRest => Redirect(calculator.routes.AmountsDueController.getAmountsDue())
    }
  }

  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue:CalculatorAmountsDue), paymentTodayOption: Option[CalculatorPaymentToday], _, _) =>
        val form = paymentTodayOption match {
          case Some(paymentToday:CalculatorPaymentToday) => CalculatorForm.createPaymentTodayForm(amountsDue.total).fill(paymentToday)
          case None => CalculatorForm.createPaymentTodayForm(amountsDue.total)
        }
        Ok(payment_today_form.render(form, request))

      case other => getRedirectionDestination(other)

    })
  }


  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, amountsDue:Some[CalculatorAmountsDue], _, _, _) =>
        val form = CalculatorForm.createPaymentTodayForm(amountsDue.get.total).bindFromRequest()
        if (form.hasErrors) {
          Ok(payment_today_form.render(form, request))
        } else {
          Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(form.get)
          )
        }

      case other => getRedirectionDestination(other)
    })
  }

  def getCalculateInstalments(monthsOption:Option[String]): Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue: CalculatorAmountsDue), Some(paymentToday: CalculatorPaymentToday),
      durationOption: Option[CalculatorDuration], schedulesOption: Option[List[CalculatorPaymentSchedule]]) =>
        schedulesOption match {

          case Some(schedules: Seq[CalculatorPaymentSchedule]) =>
            val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
            monthsOption match {

              case Some(monthsString: String) =>
                try {
                  val duration = CalculatorDuration(monthsString.toInt)
                  val durationForm = CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()
                  if (durationForm.hasErrors) {
                    val months = durationOption match {

                      case Some(d: CalculatorDuration) => d.months
                      case None => schedules.head.instalments.length
                    }
                    Ok(calculate_instalments_form(schedules.filter(_.instalments.length == months).head, durationForm,
                      CalculatorForm.createPaymentTodayForm(amountsDue.total).fill(paymentToday), instalmentOptionsAscending)
                    )
                  } else {
                    Redirect(routes.CalculatorController.getCalculateInstalments(None))
                      .addingToSession("CalculatorDuration" -> JacksonMapper.writeValueAsString(duration))
                  }
                } catch {
                  case ex:Exception => BadRequest(ex.getLocalizedMessage)
                }

              case None =>
                val duration = durationOption.getOrElse(CalculatorDuration(schedules.head.instalments.length))
                Ok(calculate_instalments_form(schedules.filter(_.instalments.length == duration.months).head,
                  CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fillAndValidate(duration),
                  CalculatorForm.createPaymentTodayForm(amountsDue.total).fill(paymentToday), instalmentOptionsAscending)
                ).addingToSession("CalculatorDuration" -> JacksonMapper.writeValueAsString(duration))
            }

          case None =>
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
            Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
              "CalculatorPaymentSchedules" -> "*"
            )
        }
      case other => getRedirectionDestination(other)
    })
  }

  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(getKeystoreData match {

      case (_, Some(amountsDue:CalculatorAmountsDue), Some(paymentToday:CalculatorPaymentToday),
      durationOption:Option[CalculatorDuration], Some(schedules:List[CalculatorPaymentSchedule])) =>
        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
        val calculatorDurationForm = CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()

        if (calculatorDurationForm.hasErrors) {
         val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
         val duration = durationOption match {
           case Some(d:CalculatorDuration) => d
           case None => CalculatorDuration(instalmentOptionsAscending.head)
         }

         Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
            calculatorDurationForm, CalculatorForm.createPaymentTodayForm(amountsDue.total).fill(paymentToday), instalmentOptionsAscending, request))
        }
        else {
          Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
            "CalculatorDuration" -> JacksonMapper.writeValueAsString(calculatorDurationForm.get)
          )
        }

      case other => getRedirectionDestination(other)
    })
  }

  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async { implicit request =>

    Future.successful(getKeystoreData match {

      case (_, Some(amountDue:CalculatorAmountsDue), _, Some(duration:CalculatorDuration), Some(schedules:List[CalculatorPaymentSchedule])) =>

        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
        val calculatorPaymentTodayForm = CalculatorForm.createPaymentTodayForm(amountDue.total).bindFromRequest()

        if (calculatorPaymentTodayForm.hasErrors) {
          val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted

          Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
            CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fill(duration),
              calculatorPaymentTodayForm, instalmentOptionsAscending, request))
        } else {
          Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(calculatorPaymentTodayForm.get)
          ).removingFromSession("CalculatorPaymentSchedules")
        }

      case other => getRedirectionDestination(other)
    })
  }
}