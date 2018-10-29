/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject._
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculatorController @Inject()(val messagesApi: play.api.i18n.MessagesApi,
                                     calculatorService: CalculatorService)
  extends TimeToPayController with play.api.i18n.I18nSupport {

  def submitSignIn: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.determineEligibility()))
  }



  //todo find a way to test around the authorisedSaUser
  def getTaxLiabilities: Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        sessionCache.get.map {
          case Some(ttpData@TTPSubmission(_, _, _, Some(tp), _, _, _, _)) =>
            Ok(tax_liabilities(tp.selfAssessment.get.debits, isSignedIn))
          case _ => redirectOnError
        }
  }
//todo perhaps wrap in auth
  def getPayTodayQuestion: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, tp, CalculatorInput(debits, _, _, _, _, _), _, _, _)) if debits.nonEmpty =>
        val dataForm = CalculatorForm.payTodayForm
        Ok(payment_today_question(dataForm, isSignedIn))
      case _ => redirectOnError
    }
  }

  /**
    * Checks the response for the pay today question. If yes navigate to payment today page
    * otherwise navigate to calculator page and set the initial payment to 0
    */
  def submitPayTodayQuestion: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpData@TTPSubmission(_, _, _, tp, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)) if debits.nonEmpty =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              sessionCache.put(ttpData.copy(calculatorData = cd.copy(initialPayment = BigDecimal(0)))).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments())
              }
          }
        )
      case _ => Future.successful(redirectOnError)
    }
  }

  def getPaymentToday: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.map {
        case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) if debits.nonEmpty =>
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
          if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form, isSignedIn))
          else Ok(payment_today_form(form.fill(paymentToday), isSignedIn))
        case _ =>
          Logger.info("Missing required data for get payment today page")
          redirectOnError
      }
  }
  def submitPaymentToday: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap[Result] {
      case Some(ttpSubmission@TTPSubmission(_, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, isSignedIn))),
          validFormData => {
            sessionCache.put(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).map{_ =>
              Redirect(routes.CalculatorController.getPaymentSummary())
            }
          }
        )
      case _ =>
        Logger.info("No TTP Data match in submitPaymentToday")
        Future.successful(redirectOnError)
    }
  }

  /**
    * Loads the calculator page. Several checks are performed:
    * - Checks to see if the session data is out of date and updates calculations if so
    * - If Taxpayer exists, load auth version of calculator
    * - If user input debits are not empty, load the calculator (avoids direct navigation)
    * - If schedule data is missing, update TTPSubmission
    */
  def getCalculateInstalments(): Action[AnyContent] = Action.async {
    implicit request =>
    /*  sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(Some(
        CalculatorPaymentSchedule(Some(startDate), _, _, _, _, _, _, _)), _, _, _, _, _, _, _))
          if !startDate.equals(LocalDate.now) =>
          updateSchedule(ttpData)(request)

        case Some(ttpData@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))),
        CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) =>
          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)

          if (setMaxMonthsAllowed(Some(sa), LocalDate.now()) >= minimunMonthsAllowedTTP) {
            Future.successful(Ok(calculate_instalments_form(schedule, Some(sa.debits),
              CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, setMonthOptions(Some(sa)), isSignedIn)))
          } else {
            //todo perhaps move to time-to-pay-eligbility?
            sessionCache.put(ttpData.copy(eligibilityStatus = Some(EligibilityStatus(false, Seq(TTPIsLessThenTwoMonths))))).map { _ =>
              Redirect(routes.SelfServiceTimeToPayController.getTtpCallUsCalculatorInstalments())
            }
          }

        case Some(ttpData@TTPSubmission(Some(schedule), _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _)) if debits.nonEmpty =>

          val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).fill(paymentToday)
          Future.successful(Ok(calculate_instalments_form(schedule, None,
            CalculatorForm.durationForm.bind(Map("months" -> schedule.instalments.length.toString)), form, setMonthOptions(), isSignedIn)))

        case Some(ttpData@TTPSubmission(None, _, _, _, _, _, _, _)) =>
          updateSchedule(ttpData)(request)
        case _ =>
          Future.successful(redirectOnError)
      }*/
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _)) =>
          calculatorService.getInstalmentsSchedule(sa).map(monthsToSchedule =>
            Ok(calculate_instalments_form(monthsToSchedule, true))
          )
        case _ =>   Future.successful(redirectOnError)
      }
  }


  def getPaymentSummary: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, initialPayment, _, _, _, _), _, _, _)) if debits.nonEmpty =>
        Ok(payment_summary(debits,initialPayment))
      case _ =>
        Logger.info("Missing required data for what you owe review page")
        redirectOnError
    }
  }




  /**
    * Update the schedule data stored in TTPSubmission, ensuring that 7 and 14 day payment
    * rules are applied
    */
  /*
  private def updateSchedule(ttpData: TTPSubmission): Action[AnyContent] = Action.async {
    implicit request =>
      ttpData match {
        case TTPSubmission(_, _, _, None,  calculatorInput, durationMonths, _, _) =>
          val newInput = logic.validateCalculatorDates(calculatorInput, durationMonths.get, calculatorInput.debits)
          calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
            case Seq(schedule) =>
              sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments())
              }
            case _ =>
              Logger.error("Failed to get payment schedule from calculator when updating schedule")
              Future.successful(redirectOnError)
          }

        case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorInput, durationMonths, _, _) =>
          val newInput = logic.validateCalculatorDates(calculatorInput, durationMonths.get, sa.debits).copy(debits = sa.debits)
          calculatorConnector.calculatePaymentSchedule(newInput).flatMap {
            case Seq(schedule) =>
              sessionCache.put(ttpData.copy(schedule = Some(schedule), calculatorData = newInput)).map[Result] {
                _ => Redirect(routes.CalculatorController.getCalculateInstalments())
              }
            case _ =>
              Logger.error("Failed to get payment schedule from calculator when updating schedule")
              Future.successful(redirectOnError)
          }
      }
  }
  */

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum
}
