/*
 * Copyright 2017 HM Revenue & Customs
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
      case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        Ok(amounts_due_form(CalculatorAmountsDue(debits), CalculatorForm.amountDueForm(debits.map(_.amount).sum), ttpData.taxpayer.isDefined))
      case _ => Ok(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm))
    }
  }

  def getAmountsDueDate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        Ok(amount_due_date_form(CalculatorAmountsDue(debits), CalculatorForm.amountDueForm(debits.map(_.amount).sum), ttpData.taxpayer.isDefined))
      case _ => Ok(amount_due_date_form(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm))
    }
  }

  def submitAddAmountDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap { ttpData =>
      val totalDebits = ttpData match {
        case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) => debits.map(_.amount).sum
        case _ => BigDecimal(0)
      }

      CalculatorForm.amountDueForm(totalDebits).bindFromRequest().fold(
        formWithErrors =>
          ttpData match {
            case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) =>
              Future.successful(BadRequest(amounts_due_form(CalculatorAmountsDue(debits), formWithErrors, ttpData.taxpayer.isDefined)))
            case _ => Future.successful(BadRequest(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty), formWithErrors)))
          },
        validFormData => {
          val newTtpData = ttpData match {
            case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _)) =>
              ttpSubmission.copy(calculatorData = cd.copy(debits = debits :+ validFormData))
            case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd, _, _)) =>
              ttpSubmission.copy(calculatorData = cd.copy(debits = IndexedSeq(validFormData)))
            case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq(validFormData)))
          }

          Logger.info(newTtpData.toString)
          sessionCache.put(newTtpData).map[Result] {
            _ => Redirect(routes.CalculatorController.getAmountsDue())
          }
        }
      )
    }
  }

  def submitRemoveAmountDue: Action[AnyContent] = Action.async { implicit request =>
    val index = CalculatorForm.removeAmountDueForm.bindFromRequest()
    sessionCache.get.map {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        ttpSubmission.copy(calculatorData = cd.copy(debits = debits.patch(index.value.get, Nil, 1)))
      case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq.empty))
    }.flatMap[Result] { data: TTPSubmission =>
      sessionCache.put(data).map(_ => Redirect(routes.CalculatorController.getAmountsDue()))
    }
  }

  def submitAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        (Some(debits), ttpSubmission.taxpayer.isDefined)
      case _ => (Some(IndexedSeq.empty), false)
    }.map { ttpData: (Option[Seq[Debit]], Boolean) =>
      if (ttpData._1.isEmpty) BadRequest(amounts_due_form(CalculatorAmountsDue(IndexedSeq.empty),
        CalculatorForm.amountDueForm.withGlobalError("You need to add at least one amount due"), ttpData._2))
      else Redirect(routes.CalculatorController.getPaymentToday())
    }
  }

  def getCalculateInstalments(months: Option[Int]): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        Future.successful(Ok(calculate_instalments_form(schedule, Some(sa.debits),
          CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        Future.successful(Ok(calculate_instalments_form(schedule, None,
          CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
      case Some(ttpData@TTPSubmission(None, _, _, _, _, _, _, _, _)) =>
        updateSchedule(ttpData).apply(request)
      case _ =>
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def getCalculateInstalmentsPrint: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        Ok(calculate_instalments_print(schedule, Some(sa.debits), ttpData.taxpayer.isDefined))
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        Ok(calculate_instalments_print(schedule, None, ttpData.taxpayer.isDefined))
      case _ => Redirect(routes.SelfServiceTimeToPayController.start())
    }
  }

  def getMisalignmentPage: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) =>
          Future.successful(Ok(misalignment(CalculatorAmountsDue(debits), sa.debits, loggedIn = true)))
        case _ =>
          Logger.error("Unhandled case in getMisalignmentPage")
          Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable()))
      }
  }

  def submitRecalculate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        updateSchedule(ttpData).apply(request)
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, tp, Some(_), _, cd@CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        CalculatorForm.durationForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, tp match {
            case Some(Taxpayer(_, _, Some(sa))) => Some(sa.debits)
            case _ => None
          }, formWithErrors, form, 2 to 11, ttpData.taxpayer.isDefined))),
          validFormData => {
            val newEndDate = cd.startDate.plusMonths(validFormData.months.get).minusDays(1)
            updateSchedule(ttpData.copy(calculatorData = cd.copy(endDate = newEndDate), durationMonths = validFormData.months))(request)
          }
        )
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, taxpayer: Option[Taxpayer], _, _, cd, _, _)) =>
        val durationForm = CalculatorForm.durationForm.fill(CalculatorDuration(Some(3)))
        CalculatorForm.createPaymentTodayForm(cd.debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, taxpayer match {
            case Some(Taxpayer(_, _, Some(sa))) => Some(sa.debits)
            case _ => None
          }, durationForm, formWithErrors, 2 to 11, ttpData.taxpayer.isDefined))),
          validFormData => {
            val ttpSubmission = ttpData.copy(calculatorData = cd.copy(initialPayment = validFormData))
            updateSchedule(ttpSubmission).apply(request)
          }
        )
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _), _, _)) if debits.isEmpty =>
        Logger.error("failed to get calculatorData")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable()))
      case _ =>
        Logger.error("No TTP Data available")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable()))
    }
  }

  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(_, _, _, Some(tp), _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        if (tp.selfAssessment.get.debits.map(_.amount).sum >= BigDecimal(32)) {
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
          if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form, ttpData.taxpayer.isDefined))
          else Ok(payment_today_form(form.fill(paymentToday), ttpData.taxpayer.isDefined))
        } else {
          Logger.info("Amount owed is less than £32")
          Redirect(routes.SelfServiceTimeToPayController.getYouNeedToFile())
        }
      case Some(TTPSubmission(_, _, _, None, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _)) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form))
        else Ok(payment_today_form(form.fill(paymentToday)))
      case _ =>
        Logger.info("No TTP Data match in getPaymentToday")
        Redirect(routes.SelfServiceTimeToPayController.start())
    }
  }

  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, _, _, _, _, _), _, _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, ttpSubmission.taxpayer.isDefined))),
          validFormData => {
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).apply(request)
          }
        )
      case _ =>
        Logger.info("No TTP Data match in submitPaymentToday")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  private def dayOfMonthCheck(date: LocalDate): LocalDate = date.getDayOfMonth match {
    case day if day > 28 => date.plusMonths(1).withDayOfMonth(1)
    case _ => date
  }

  private def validateCalculatorDates(calculatorInput: CalculatorInput, numberOfMonths: Int, debits: Seq[Debit]): CalculatorInput = {
    val firstPaymentDate = dayOfMonthCheck(LocalDate.now().plusWeeks(1))

    if (calculatorInput.initialPayment > 0) {
      if (debits.map(_.amount).sum.-(calculatorInput.initialPayment) < BigDecimal.exact("32.00")) {
        calculatorInput.copy(initialPayment = BigDecimal(0),
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      } else {
        calculatorInput.copy(firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      }
    }
    else
      calculatorInput.copy(startDate = LocalDate.now(),
        firstPaymentDate = Some(firstPaymentDate),
        endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
  }

  private def updateSchedule(ttpData: TTPSubmission): Action[AnyContent] = Action.async { implicit request =>
    ttpData match {
      case TTPSubmission(_, _, _, None, _, _, calculatorInput, durationMonths, _) =>
        val newInput = validateCalculatorDates(calculatorInput, durationMonths.get, calculatorInput.debits)

        calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
          case Seq(schedule) =>
            sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] { _ =>
              Redirect(routes.CalculatorController.getCalculateInstalments(None))
            }
          case _ => throw new RuntimeException("Failed to get schedule")
        }
      case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, calculatorInput, durationMonths, _) =>
        val newInput = validateCalculatorDates(calculatorInput, durationMonths.get, sa.debits).copy(debits = sa.debits)

        calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
          case Seq(schedule) =>
            sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] { _ =>
              Redirect(routes.CalculatorController.getCalculateInstalments(None))
            }
          case _ => throw new RuntimeException("Failed to get schedule")
        }
    }
  }
}
