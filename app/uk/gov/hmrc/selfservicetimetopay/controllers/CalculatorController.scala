/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService._
import views.html.selfservicetimetopay.calculator._
import views.html.selfservicetimetopay.unauth._

import scala.concurrent.Future

class CalculatorController @Inject()(val messagesApi: play.api.i18n.MessagesApi,
                                     calculatorService: CalculatorService)
  extends TimeToPayController with play.api.i18n.I18nSupport {


  def submitSignIn: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.ArrangementController.determineEligibility()))
  }


  def getPaymentPlanCalculator: Action[AnyContent] = Action { implicit request =>
    Ok(payment_plan_calculator(isSignedIn))
  }

  def getAmountDue: Action[AnyContent] = Action { implicit request =>
    Ok(amount_due(isSignedIn, CalculatorForm.createAmountDueForm()))
  }

  def submitAmountDue: Action[AnyContent] = Action.async { implicit request =>
    CalculatorForm.createAmountDueForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(amount_due(isSignedIn, formWithErrors))),
      amountDue => {
        //todo perhaps we dont need  a new one? this will whip the data from the auth journey is this ok ?User can just redo it?
        val dataWithAmount = TTPSubmission(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue.amount))))
        sessionCache.put(dataWithAmount).map { _ =>
          Redirect(routes.CalculatorController.getCalculateInstalmentsUnAuth())
        }
      }
    )

  }

  def getCalculateInstalmentsUnAuth(): Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsSchedule(SelfAssessment
          (debits = Seq(Debit(amount = amountDue, dueDate = LocalDate.now()))), 0).map { monthsToSchedule =>
            Ok(calculate_instalments_form(CalculatorForm.createInstalmentForm(),
              ttpData.lengthOfArrangement, monthsToSchedule, routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false))
          }

        case _ => Future.successful(redirectOnError)
      }
  }


  def submitCalculateInstalmentsUnAuth(): Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsScheduleUnAuth(debits = Seq(Debit(amount = amountDue, dueDate = LocalDate.now()))).flatMap { monthsToSchedule =>
            CalculatorForm.createInstalmentForm().bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                  monthsToSchedule, routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false)))
              },
              validFormData => {
                sessionCache.put(ttpData.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue),
                  Some(monthsToSchedule(validFormData.chosenMonths)))))).map { _ =>
                  Redirect(routes.CalculatorController.getCheckCalculation())
                }
              }
            )
          }
        case _ => Future.successful(redirectOnError)
      }
  }


  def getCheckCalculation: Action[AnyContent] = Action.async {
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttpData@TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(_, Some(schedule))), _)) =>
          Future.successful(Ok(check_calculation( schedule, isSignedIn)))
        case _ => Future.successful(redirectOnError)
      }
  }

  def getTaxLiabilities: Action[AnyContent] = authorisedSaUser {
    implicit request =>
      implicit authContext =>
        sessionCache.get.map {
          case Some(_@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
            Ok(tax_liabilities(sa.debits, isSignedIn))
          case _ => redirectOnError
        }
  }

  def getPayTodayQuestion: Action[AnyContent] = authorisedSaUser {
    implicit request =>
      implicit authContext =>
        sessionCache.get.map {
          case Some(TTPSubmission(_, _, _, tp, CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
            Ok(payment_today_question(CalculatorForm.payTodayForm, isSignedIn))
          case _ => redirectOnError
        }
  }

  /**
    * Checks the response for the pay today question. If yes navigate to payment today page
    * otherwise navigate to calculator page and set the initial payment to 0
    */
  def submitPayTodayQuestion: Action[AnyContent] = authorisedSaUser { implicit request =>
    implicit authContext =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpData@TTPSubmission(_, _, _, tp, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
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

  def getPaymentToday: Action[AnyContent] = authorisedSaUser {
    implicit request =>
      implicit authContext =>
        sessionCache.get.map {
          case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
            val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
            if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form, isSignedIn))
            else Ok(payment_today_form(form.fill(paymentToday), isSignedIn))
          case _ =>
            Logger.info("Missing required data for get payment today page")
            redirectOnError
        }
  }

  def submitPaymentToday: Action[AnyContent] = authorisedSaUser { implicit request =>
    implicit authContext =>
      sessionCache.get.flatMap[Result] {
        case Some(ttpSubmission@TTPSubmission(_, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) =>
          CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, isSignedIn))),
            validFormData => {
              sessionCache.put(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).map { _ =>
                Redirect(routes.CalculatorController.getPaymentSummary())
              }
            }
          )
        case _ =>
          Logger.info("No TTP Data match in submitPaymentToday")
          Future.successful(redirectOnError)
      }
  }


  def getPaymentSummary: Action[AnyContent] = authorisedSaUser { implicit request =>
    implicit authContext =>
      sessionCache.get.map {
        case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, initialPayment, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
          Ok(payment_summary(debits, initialPayment))
        case _ =>
          Logger.info("Missing required data for what you owe review page")
          redirectOnError
      }
  }

  /**
    * Loads the calculator page. Several checks are performed:
    * - Checks to see if the session data is out of date and updates calculations if so
    * - If Taxpayer exists, load auth version of calculator
    * - If user input debits are not empty, load the calculator (avoids direct navigation)
    * - If schedule data is missing, update TTPSubmission
    */
  def getCalculateInstalments: Action[AnyContent] = authorisedSaUser {
    implicit request =>
      implicit authContext =>
        sessionCache.get.flatMap {
          case Some(ttpData@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
            if (getMaxMonthsAllowed(sa, LocalDate.now()) >= minimumMonthsAllowedTTP) {
              calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).map { monthsToSchedule =>
                Ok(calculate_instalments_form(CalculatorForm.createInstalmentForm(),
                  ttpData.lengthOfArrangement, monthsToSchedule, routes.CalculatorController.submitCalculateInstalments(), loggedIn = true))
              }
            }
            else {
              //todo perhaps move these checks else where to eligbility service?
              sessionCache.put(ttpData.copy(eligibilityStatus = Some(EligibilityStatus(eligible = false, Seq(TTPIsLessThenTwoMonths))))).map { _ =>
                Redirect(routes.SelfServiceTimeToPayController.getTtpCallUsCalculatorInstalments())
              }
            }

          case _ => Future.successful(redirectOnError)
        }
  }

  def submitCalculateInstalments(): Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        sessionCache.get.flatMap {
          case Some(ttpData@TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
            calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { monthsToSchedule =>
              CalculatorForm.createInstalmentForm().bindFromRequest().fold(
                formWithErrors => {
                  Future.successful(BadRequest(calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                    monthsToSchedule, routes.CalculatorController.submitCalculateInstalments(), loggedIn = true)))
                },
                validFormData => {
                  sessionCache.put(ttpData.copy(schedule = Some(monthsToSchedule(validFormData.chosenMonths)))).map { _ =>
                    Redirect(routes.ArrangementController.getChangeSchedulePaymentDay())
                  }
                }
              )
            }
          case _ => Future.successful(redirectOnError)
        }
  }
}
