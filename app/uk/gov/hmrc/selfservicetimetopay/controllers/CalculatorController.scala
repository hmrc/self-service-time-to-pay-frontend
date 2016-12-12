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

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector}
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculatorController(eligibilityConnector: EligibilityConnector,
                           calculatorConnector: CalculatorConnector) extends TimeToPayController {
  def start: Action[AnyContent] = Action { request =>
    Redirect(routes.CalculatorController.getAmountsDue())
  }

  def getAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) =>
        Ok(amounts_due_form.render(CalculatorAmountsDue(debits), CalculatorForm.amountDueForm, request))
      case _ => Ok(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm, request))
    }
  }

  def submitAddAmountDue: Action[AnyContent] = Action.async { implicit request =>
    CalculatorForm.amountDueForm.bindFromRequest().fold(
      formWithErrors =>
        sessionCache.get.map {
          case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) =>
            BadRequest(amounts_due_form.render(CalculatorAmountsDue(debits), formWithErrors, request))
          case _ => BadRequest(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), formWithErrors, request))
        },

      validFormData => {
        sessionCache.get.map {
          case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _))) =>
            ttpSubmission.copy(calculatorData = cd.copy(debits = debits :+ validFormData))
          case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd)) =>
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
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _))) =>
        ttpSubmission.copy(calculatorData = cd.copy(debits = debits.patch(index.value.get, Nil, 1)))
      case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq.empty))
    }.flatMap { ttpData =>
      sessionCache.put(ttpData).map {
        _ => Redirect(routes.CalculatorController.getAmountsDue())
      }
    }
  }

  def submitAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) => Some(debits)
      case _ => Some(IndexedSeq.empty)
    }.map { ttpData =>
      if (ttpData.isEmpty) BadRequest(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty),
        CalculatorForm.amountDueForm.withGlobalError("You need to add at least one amount due"), request))
      else Redirect(routes.CalculatorController.getPaymentToday())
    }
  }

  def getCalculateInstalments(months: Option[Int]): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _))) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        Ok(calculate_instalments_form(schedule, CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11))
      case _ => NotFound("Failed to get schedule")
    }
  }

  def getCalculateInstalmentsPrint: Action[AnyContent] = Action.async { implicit request =>
      sessionCache.get.map {
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _))) =>
          Ok(calculate_instalments_print(schedule, ttpData.taxpayer.isDefined))
        case _ => NotFound("Failed to get schedule")
      }
  }

  def getMisalignmentPage: Action[AnyContent] = AuthorisedSaUser { implicit authContext => implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, _, _, _, _, _))) =>
        Ok(misalignment.render(CalculatorAmountsDue(debits), sa.debits, request))
    }
  }

  def submitRecalculate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, cd @ CalculatorInput(debits, _, _, _, _, _))) =>
        updateSchedule(ttpData).apply(request)
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(Some(schedule), _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, paymentToday, _, _, _, _))) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
        CalculatorForm.durationForm.bindFromRequest().fold(
          //TODO change 2 to 11 to a dynamically calculated permitted range
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, formWithErrors, form, 2 to 11))),
          validFormData => {
            val newEndDate = cd.startDate.plusMonths(validFormData.months).minusDays(1)
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(endDate = newEndDate))).apply(request)
          }
        )
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }

  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(Some(schedule), _, _, None, _, _, cd)) =>
        val durationForm = CalculatorForm.durationForm.fill(CalculatorDuration(3))
        CalculatorForm.createPaymentTodayForm(cd.debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, durationForm, formWithErrors, 2 to 11))),
          validFormData => {
              val ttpSubmission = ttpData.copy(calculatorData = cd.copy(initialPayment = validFormData))
              updateSchedule(ttpSubmission).apply(request)
          }
        )
      case Some(ttpData@TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) if debits.isEmpty =>
        Future.successful(NotFound("failed to get calculatorData"))
    }
  }

  private def updateSchedule(ttpData: TTPSubmission): Action[AnyContent] = Action.async { implicit request =>
    ttpData match {
      case TTPSubmission(_, _, _, None, _, _, calculatorInput) =>
        calculatorConnector.calculatePaymentSchedule(calculatorInput).flatMap {
          case Seq(schedule) =>
            sessionCache.put(ttpData.copy(schedule = Some(schedule))).map[Result] { result =>
              Redirect(routes.CalculatorController.getCalculateInstalments(None))
            }
          case _ => throw new RuntimeException("Failed to get schedule")
        }
      case TTPSubmission(_, _, _, Some(taxpayer @ Taxpayer(_, _, Some(sa))), _, _, calculatorInput) =>
        eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), taxpayer)).flatMap {
          case EligibilityStatus(true, _) =>
            calculatorConnector.calculatePaymentSchedule(calculatorInput.copy(debits = sa.debits)).flatMap {
              case Seq(schedule) =>
                sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = calculatorInput.copy(debits = sa.debits))).map[Result] { result =>
                  Redirect(routes.CalculatorController.getCalculateInstalments(None))
                }
              case _ => throw new RuntimeException("Failed to get schedule")
            }
          case EligibilityStatus(_, reasons) =>
            Logger.info(s"Failed eligibility check because: $reasons")
            Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs()))
        }
    }
  }

  def getPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _))) =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form.render(form, request))
        else Ok(payment_today_form.render(form.fill(paymentToday), request))
      case _ => Redirect(routes.SelfServiceTimeToPayController.start())
    }
  }

  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, paymentToday, _, _, _, _))) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors))),
          validFormData => {
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).apply(request)
          }
        )
      case _ => Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
    }
  }
}