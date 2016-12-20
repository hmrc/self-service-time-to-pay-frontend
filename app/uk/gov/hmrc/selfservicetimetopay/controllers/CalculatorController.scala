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

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.CalculatorConnector
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculatorController(calculatorConnector: CalculatorConnector) extends TimeToPayController {
  def start: Action[AnyContent] = Action { request =>
    Redirect(routes.CalculatorController.getAmountsDue())
  }

  def getAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _)) =>
        Ok(amounts_due_form(CalculatorAmountsDue(debits), CalculatorForm.amountDueForm, ttpData.taxpayer.isDefined))
      case _ => Ok(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm))
    }
  }

  def submitAddAmountDue: Action[AnyContent] = Action.async { implicit request =>
    CalculatorForm.amountDueForm.bindFromRequest().fold(
      formWithErrors =>
        sessionCache.get.map {
          case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _)) =>
            BadRequest(amounts_due_form(CalculatorAmountsDue(debits), formWithErrors, ttpData.taxpayer.isDefined))
          case _ => BadRequest(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty), formWithErrors))
        },

      validFormData => {
        sessionCache.get.map {
          case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _)) =>
            ttpSubmission.copy(calculatorData = cd.copy(debits = debits :+ validFormData))
          case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd, _)) =>
            ttpSubmission.copy(calculatorData = cd.copy(debits = IndexedSeq(validFormData)))
          case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq(validFormData)))
        }.flatMap { ttpData =>
          Logger.info(ttpData.toString)
          sessionCache.put(ttpData).map {
            _ => Redirect(routes.CalculatorController.getAmountsDue())
          }
        }
      }
    )
  }

  def submitRemoveAmountDue: Action[AnyContent] = Action.async { implicit request =>
    val index = CalculatorForm.removeAmountDueForm.bindFromRequest()
    sessionCache.get.map {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _)) =>
        ttpSubmission.copy(calculatorData = cd.copy(debits = debits.patch(index.value.get, Nil, 1)))
      case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq.empty))
    }.flatMap { ttpData =>
      sessionCache.put(ttpData).map {
        _ => Redirect(routes.CalculatorController.getAmountsDue())
      }
    }
  }

  def submitAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    var loggedIn = false
    sessionCache.get.map {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _)) =>
        loggedIn = ttpSubmission.taxpayer.isDefined
        Some(debits)
      case _ => Some(IndexedSeq.empty)
    }.map { ttpData =>
      if (ttpData.isEmpty) BadRequest(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty),
        CalculatorForm.amountDueForm.withGlobalError("You need to add at least one amount due"), loggedIn))
      else Redirect(routes.CalculatorController.getPaymentToday())
    }
  }

  def getCalculateInstalments(months: Option[Int]): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        Future.successful(Ok(calculate_instalments_form(schedule,
          CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        Future.successful(Ok(calculate_instalments_form(schedule,
          CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
      case Some(ttpData@TTPSubmission(None, _, _, _, _, _, _, _)) =>
        updateSchedule(ttpData).apply(request)
      case _ =>
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def getCalculateInstalmentsPrint: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        Ok(calculate_instalments_print(schedule, ttpData.taxpayer.isDefined))
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        Ok(calculate_instalments_print(schedule, ttpData.taxpayer.isDefined))
      case _ => Redirect(routes.SelfServiceTimeToPayController.start())
    }
  }

  def getMisalignmentPage: Action[AnyContent] = AuthorisedSaUser { implicit authContext => implicit request =>
    authorizedForSsttp {
      sessionCache.get.map {
        case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, _, _, _, _, _), _)) =>
          Ok(misalignment(CalculatorAmountsDue(debits), sa.debits, loggedIn = true))
        case _ => throw new RuntimeException("Unhandled case in getMisalignmentPage")
      }
    }
  }

  def submitRecalculate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, cd@CalculatorInput(debits, _, _, _, _, _), _)) =>
        updateSchedule(ttpData).apply(request)
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(Some(schedule), _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        CalculatorForm.durationForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, formWithErrors, form, 2 to 11, ttpSubmission.taxpayer.isDefined))),
          validFormData => {
            val newEndDate = cd.startDate.plusMonths(validFormData.months).minusDays(1)
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(endDate = newEndDate), durationMonths = Some(validFormData.months))).apply(request)
          }
        )
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, cd, _)) =>
        val durationForm = CalculatorForm.durationForm.fill(CalculatorDuration(3))
        CalculatorForm.createPaymentTodayForm(cd.debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, durationForm, formWithErrors, 2 to 11, ttpData.taxpayer.isDefined))),
          validFormData => {
            val ttpSubmission = ttpData.copy(calculatorData = cd.copy(initialPayment = validFormData))
            updateSchedule(ttpSubmission).apply(request)
          }
        )
      case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _)) if debits.isEmpty =>
        Logger.error("failed to get calculatorData")
        throw new RuntimeException("failed to get calculatorData")
    }
  }

  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, Some(tp), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        if(tp.selfAssessment.get.debits.map(_.amount).sum >= BigDecimal(32)) {
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
          if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form, true))
          else Ok(payment_today_form(form.fill(paymentToday), true))
        } else Redirect(routes.SelfServiceTimeToPayController.getYouNeedToFile())
      case Some(ttpData@TTPSubmission(_, _, _, None, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form))
        else Ok(payment_today_form(form.fill(paymentToday)))
      case _ => Redirect(routes.SelfServiceTimeToPayController.start())
    }
  }

  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, paymentToday, _, _, _, _), _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, ttpSubmission.taxpayer.isDefined))),
          validFormData => {
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).apply(request)
          }
        )
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  private def setCalculatorInputDates(calculatorInput: CalculatorInput, sa: SelfAssessment, durationMonths: Int): CalculatorInput = {
    val firstPmntDate = calculatorInput.startDate.plusMonths(1)
    var newInput = calculatorInput.copy(debits = sa.debits, firstPaymentDate = Some(firstPmntDate))
    val endDate = calculatorInput.startDate.plusMonths(durationMonths)

    if (calculatorInput.initialPayment.equals(BigDecimal(0))) {
      newInput = calculatorInput.copy(
        debits = sa.debits, initialPayment = BigDecimal(0),
        firstPaymentDate = Some(calculatorInput.startDate),
        endDate = calculatorInput.startDate.plusMonths(durationMonths - 1))
    } else {
      // if there is an initial payment then startDate = firstPaymentDate
      newInput = if (sa.debits.map(_.amount).sum.-(calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(debits = sa.debits, initialPayment = BigDecimal(0),
          firstPaymentDate = Some(calculatorInput.startDate.plusMonths(1)),
          endDate = endDate
        )
      } else {
        calculatorInput.copy(debits = sa.debits,
          firstPaymentDate = Some(calculatorInput.startDate.plusMonths(1)),
          endDate = endDate
        )
      }
    }
    newInput
  }

  private def updateSchedule(ttpData: TTPSubmission): Action[AnyContent] = Action.async { implicit request =>
    ttpData match {
      case TTPSubmission(_, _, _, None, _, _, calculatorInput, durationMonths) =>
        var newInput = calculatorInput
        // if there is an initial payment then move startDate and endDate by a month
        if(calculatorInput.initialPayment > BigDecimal(0)) {
          newInput = calculatorInput.copy(firstPaymentDate = Some(calculatorInput.startDate.plusMonths(1)),
            endDate = calculatorInput.startDate.plusMonths(durationMonths.get)) // firstPaymentDate + durationMonths
        }

        calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
          case Seq(schedule) =>
            sessionCache.put(ttpData.copy(schedule = Some(schedule))).map[Result] { result =>
              Redirect(routes.CalculatorController.getCalculateInstalments(None))
            }
          case _ => throw new RuntimeException("Failed to get schedule")
        }
      case TTPSubmission(_, _, _, Some(taxpayer@Taxpayer(_, _, Some(sa))), _, _, calculatorInput, durationMonths) =>

        val newInput = setCalculatorInputDates(calculatorInput, sa, durationMonths.get)

        calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
          case Seq(schedule) =>
            sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] { result =>
              Redirect(routes.CalculatorController.getCalculateInstalments(None))
            }
          case _ => throw new RuntimeException("Failed to get schedule")
        }
    }
  }
}