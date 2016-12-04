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

import java.time.LocalDate

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

class CalculateInstalmentsController extends TimeToPayController {

  def getCalculateInstalments(monthsOption:Option[String]): Action[AnyContent] = Action.async { implicit request =>
    // create fake schedule
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, debits @ Some(_), paymentToday @ Some(_))) =>
        // TODO replace the below with a call to the orchestration layer (self-service-time-pay service)
        val total = debits.get.map(_.amount).sum
        val form = CalculatorForm.createPaymentTodayForm(total)
        val instalments = Seq(
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,3,1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,4,1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,5,1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,6,1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,7,1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017,8,1), 350))
        val schedule = CalculatorPaymentSchedule(startDate = Some(LocalDate.of(2017,2,1)), endDate = Some(LocalDate.of(2017,12,1)),
          initialPayment = paymentToday.get.amount.get,
          amountToPay = total,
          instalmentBalance = total - paymentToday.get.amount.get,
          totalInterestCharged = total*0.0275,
          totalPayable = total+total*0.0275,
          instalments = instalments)

        val instalmentOptionsAscending = Seq(2,3,4,5,6,7,8,9,10,11)

        Ok(calculate_instalments_form(schedule, CalculatorForm.durationForm, form.fill(paymentToday.get), instalmentOptionsAscending))
      // TODO Redirect to approrpiate page - i.e. start of service
      case _ => Ok(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm, request))
    }

  }


//  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
//    Future.successful(getKeystoreData match {
//
//      case (_, Some(amountsDue:CalculatorAmountsDue), Some(paymentToday:CalculatorPaymentToday),
//      durationOption:Option[CalculatorDuration], Some(schedules:List[CalculatorPaymentSchedule])) =>
//        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
//        val calculatorDurationForm = CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).bindFromRequest()
//
//        if (calculatorDurationForm.hasErrors) {
//          val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
//          val duration = durationOption match {
//            case Some(d:CalculatorDuration) => d
//            case None => CalculatorDuration(instalmentOptionsAscending.head)
//          }
//
//          Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
//            calculatorDurationForm, CalculatorForm.createPaymentTodayForm(amountsDue.total).fill(paymentToday), instalmentOptionsAscending, request))
//        }
//        else {
//          Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
//            "CalculatorDuration" -> JacksonMapper.writeValueAsString(calculatorDurationForm.get)
//          )
//        }
//
//      case other => getRedirectionDestination(other)
//    })
//  }
//
//  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async { implicit request =>
//
//    Future.successful(getKeystoreData match {
//
//      case (_, Some(amountDue:CalculatorAmountsDue), _, Some(duration:CalculatorDuration), Some(schedules:List[CalculatorPaymentSchedule])) =>
//
//        val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
//        val calculatorPaymentTodayForm = CalculatorForm.createPaymentTodayForm(amountDue.total).bindFromRequest()
//
//        if (calculatorPaymentTodayForm.hasErrors) {
//          val instalmentOptionsAscending = schedules.map(_.instalments.length).sorted
//
//          Ok(calculate_instalments_form.render(schedules.filter(_.instalments.length == duration.months).head,
//            CalculatorForm.createDurationForm(instalmentOptionsAscending.head, instalmentOptionsAscending.last).fill(duration),
//            calculatorPaymentTodayForm, instalmentOptionsAscending, request))
//        } else {
//          Redirect(routes.CalculatorController.getCalculateInstalments(None)).addingToSession(
//            "CalculatorPaymentToday" -> JacksonMapper.writeValueAsString(calculatorPaymentTodayForm.get)
//          ).removingFromSession("CalculatorPaymentSchedules")
//        }
//
//      case other => getRedirectionDestination(other)
//    })
//  }

}