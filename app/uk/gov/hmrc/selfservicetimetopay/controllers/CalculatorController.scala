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
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject._

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.connectors.CalculatorConnector
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculatorController @Inject()(val messagesApi: play.api.i18n.MessagesApi, calculatorConnector: CalculatorConnector)
  extends TimeToPayController with play.api.i18n.I18nSupport {


  def getDebitDate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData@TTPSubmission(_, _, _, None, `validTypeOfTax`, `validExistingTTP`, _, _, _, debitDate)) =>
        debitDate match {
          case Some(_) =>
            sessionCache.put(ttpData.copy(debitDate = None)).map[Result] {
              _ => Ok(what_you_owe_date(CalculatorForm.createDebitDateForm()))
            }
          case _ => Future.successful(Ok(what_you_owe_date(CalculatorForm.createDebitDateForm())))
        }
      case _ => Future.successful(redirectOnError)
    }
  }

  def submitDebitDate: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(_, _, _, None, `validTypeOfTax`, `validExistingTTP`, _, _, _, None)) =>
          CalculatorForm.createDebitDateForm().bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(what_you_owe_date(formWithErrors))),
            validFormData => {
              sessionCache.put(ttpData.copy(debitDate = Some(LocalDate.of(validFormData.dueByYear.toInt,
                validFormData.dueByMonth.toInt, validFormData.duebyDay.toInt)))).map[Result] {
                _ => Redirect(routes.CalculatorController.getAmountOwed())
              }
            }
          )
        case _ => Future.successful(redirectOnError)
      }
  }

  def getAmountOwed: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.map[Result] {
        case Some(TTPSubmission(_, _, _, None, `validTypeOfTax`, `validExistingTTP`, _, _, _, Some(debitDate))) =>
          val dataForm = CalculatorForm.createSinglePaymentForm()
          Ok(what_you_owe_amount(dataForm, debitDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))))
        case _ => redirectOnError
      }
  }

  def submitAmountOwed: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpData@TTPSubmission(_, _, _, None, `validTypeOfTax`, `validExistingTTP`,
        CalculatorInput(debits, _, _, _, _, _), _, _, Some(debitDate))) =>
          CalculatorForm.createSinglePaymentForm().bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(what_you_owe_amount(formWithErrors,
              debitDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))))),
            validFormData => {
              sessionCache.put(ttpData.copy(
                calculatorData = ttpData.calculatorData.copy(debits :+ Debit(amount = validFormData.amount, dueDate = debitDate)),
                debitDate = None)).map(_ => Redirect(routes.CalculatorController.getWhatYouOweReview()))
            }
          )
        case _ => Future.successful(redirectOnError)
      }
  }


  def getPayTodayQuestion: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, tp, `validTypeOfTax`,
      `validExistingTTP`, CalculatorInput(debits, _, _, _, _, _), _, _, _)) if debits.nonEmpty =>
          val dataForm = CalculatorForm.payTodayForm
          Ok(payment_today_question(dataForm, tp.isDefined))
      case _ => redirectOnError
    }
  }

  def submitPayTodayQuestion: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(_, _, _, tp, `validTypeOfTax`,
      `validExistingTTP`, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)) if debits.nonEmpty =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_question(formWithErrors, tp.isDefined))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              sessionCache.put(ttpData.copy(calculatorData = cd.copy(initialPayment = BigDecimal(0)))).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments(None))
              }
          }
        )
      case _ => Future.successful(redirectOnError)
    }
  }


  def submitRemoveAmountDue: Action[AnyContent] = Action.async { implicit request =>
    val index = CalculatorForm.removeAmountDueForm.bindFromRequest()
    sessionCache.get.map {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)) =>
        ttpSubmission.copy(calculatorData = cd.copy(debits = debits.patch(index.value.get, Nil, 1)),
          schedule = None)
      case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq.empty),
        schedule = None)
    }.flatMap[Result] {
      case data@TTPSubmission(_, _, _, _, `validTypeOfTax`, `validExistingTTP`,
      CalculatorInput(debits, _, _, _, _, _), _, _, _) if debits.isEmpty =>
        sessionCache.put(data).map(_ => Redirect(routes.CalculatorController.getDebitDate()))
      case data =>
        sessionCache.put(data).map(_ => Redirect(routes.CalculatorController.getWhatYouOweReview()))
    }
  }


  def getCalculateInstalments(months: Option[Int]): Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(Some(
        CalculatorPaymentSchedule(Some(startDate), _, _, _, _, _, _, _)), _, _, _, _, _, _, _, _, _))
          if !startDate.equals(LocalDate.now) =>
          updateSchedule(ttpData).apply(request)
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _,
        CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) =>
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
          Future.successful(Ok(calculate_instalments_form(schedule, Some(sa.debits),
            CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) if debits.nonEmpty =>
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
          Future.successful(Ok(calculate_instalments_form(schedule, None,
            CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, 2 to 11, ttpData.taxpayer.isDefined)))
        case Some(ttpData@TTPSubmission(None, _, _, _, _, _, _, _, _, _)) =>
          updateSchedule(ttpData).apply(request)
        case _ =>
          Future.successful(redirectOnError)
      }
  }

  def getCalculateInstalmentsPrint: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.map {
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
          Ok(calculate_instalments_print(schedule, Some(sa.debits), ttpData.taxpayer.isDefined))
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, _, _, _, _, _, _)) =>
          Ok(calculate_instalments_print(schedule, None, ttpData.taxpayer.isDefined))
        case _ => redirectOnError
      }
  }

  def getMisalignmentPage: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      sessionCache.get.map {
        case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, CalculatorInput(debits, _, _, _, _, _), _, _, _)) =>
          if (!areEqual(sa.debits, debits)) Ok(misalignment(CalculatorAmountsDue(debits), sa.debits, loggedIn = true))
          else Redirect(routes.ArrangementController.getInstalmentSummary())
        case _ =>
          Logger.error("Unhandled case in getMisalignmentPage")
          Redirect(routes.SelfServiceTimeToPayController.getUnavailable())
      }
  }

  def submitRecalculate: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpData@TTPSubmission(_, _, _, _, _, _, _, _, _, _)) => updateSchedule(ttpData).apply(request)
        case None => Future.successful(redirectOnError)
      }
  }

  def submitCalculateInstalments: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, tp, Some(_), _, cd@CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) =>
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
        case _ =>
          Logger.info("Missing required data for submit payment today on instalments page")
          Future.successful(redirectOnError)
      }
  }

  def submitCalculateInstalmentsPaymentToday: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpData@TTPSubmission(Some(schedule), _, _, taxpayer, `validTypeOfTax`,
        `validExistingTTP`, cd@CalculatorInput(debits, _, _, _, _, _), Some(months), _, _)) =>
          val durationForm = CalculatorForm.durationForm.fill(CalculatorDuration(Some(months)))
          CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(calculate_instalments_form(schedule, taxpayer match {
              case Some(Taxpayer(_, _, Some(sa))) => Some(sa.debits)
              case _ => None
            }, durationForm, formWithErrors, 2 to 11, ttpData.taxpayer.isDefined))),
            validFormData => {
              val ttpSubmission = ttpData.copy(calculatorData = cd.copy(initialPayment = validFormData))
              updateSchedule(ttpSubmission).apply(request)
            }
          )
        case _ =>
          Logger.info("Missing required data for submit payment today on instalments page")
          Future.successful(redirectOnError)
      }
  }

  def getPaymentToday: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.map {
        case Some(TTPSubmission( _, _, _, taxpayer, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) if debits.nonEmpty =>
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
          if (paymentToday.equals(BigDecimal(0))) {
            Ok(payment_today_form(form, taxpayer.isDefined))
          }
          else Ok(payment_today_form(form.fill(paymentToday), taxpayer.isDefined))
        case _ =>
          Logger.info("Missing required data for get payment today page")
          redirectOnError
      }
  }

  def getWhatYouOweReview: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, `validTypeOfTax`,
      `validExistingTTP`, CalculatorInput(debits, _, _, _, _, _), _, _, _)) if debits.nonEmpty =>
        Ok(what_you_owe_review(debits))
      case _ =>
        Logger.info("Missing required data for what you owe review page")
        redirectOnError
    }
  }

  def submitWhatYouOweReview: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.CalculatorController.getPayTodayQuestion()))
  }

  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, Some(_), Some(_), cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, ttpSubmission.taxpayer.isDefined))),
          validFormData => {
            updateSchedule(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).apply(request)
          }
        )
      case _ =>
        Logger.info("No TTP Data match in submitPaymentToday")
        Future.successful(redirectOnError)
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
        calculatorInput.copy(startDate = LocalDate.now,
          initialPayment = BigDecimal(0),
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      } else {
        calculatorInput.copy(startDate = LocalDate.now,
          firstPaymentDate = Some(dayOfMonthCheck(firstPaymentDate.plusWeeks(2))),
          endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
      }
    }
    else
      calculatorInput.copy(startDate = LocalDate.now(),
        firstPaymentDate = Some(firstPaymentDate),
        endDate = calculatorInput.startDate.plusMonths(numberOfMonths))
  }

  private def updateSchedule(ttpData: TTPSubmission): Action[AnyContent] = Action.async {
    implicit request =>
      ttpData match {
        case TTPSubmission(_, _, _, None, _, _, calculatorInput, durationMonths, _, _) =>
          val newInput = validateCalculatorDates(calculatorInput, durationMonths.get, calculatorInput.debits)

          calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
            case Seq(schedule) =>
              sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments(None))
              }
            case _ =>
              Logger.error("Failed to get payment schedule from calculator when updating schedule")
              Future.successful(redirectOnError)
          }

        case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, calculatorInput, durationMonths, _, _) =>
          val newInput = validateCalculatorDates(calculatorInput, durationMonths.get, sa.debits).copy(debits = sa.debits)

          calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
            case Seq(schedule) =>
              sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments(None))
              }
            case _ =>
              Logger.error("Failed to get payment schedule from calculator when updating schedule")
              Future.successful(redirectOnError)
          }
      }
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum
}
