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

package ssttpcalculator

import java.time.LocalDate

import config.AppConfig
import controllers.FrontendController
import controllers.action.Actions
import javax.inject._
import play.api.Logger
import play.api.mvc.{AnyContent, _}
import sttpsubmission.SubmissionService
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.Views
import views.html.calculator._
import views.html.unauth._

import scala.concurrent.{ExecutionContext, Future}

class CalculatorController @Inject() (
    mcc:               MessagesControllerComponents,
    calculatorService: CalculatorService,
    as:                Actions,
    submissionService: SubmissionService,
    views:             Views)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendController(mcc) {

  def submitSignIn: Action[AnyContent] = Action { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.determineEligibility())
  }

  def getPaymentPlanCalculator: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.payment_plan_calculator(isSignedIn))
  }

  def getAmountDue: Action[AnyContent] = as.checkSession { implicit request =>
    Ok(views.amount_due(isSignedIn, CalculatorForm.createAmountDueForm()))
  }

  def submitAmountDue: Action[AnyContent] = as.checkSession.async { implicit request =>
    CalculatorForm.createAmountDueForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.amount_due(isSignedIn, formWithErrors))),
      amountDue => {
        //todo perhaps we dont need  a new one? this will whip the data from the auth journey is this ok ?User can just redo it?
        val dataWithAmount = TTPSubmission(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue.amount))))
        submissionService.putTtpSessionCarrier(dataWithAmount).map { _ =>
          Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalmentsUnAuth())
        }
      }
    )

  }

  def getCalculateInstalmentsUnAuth(): Action[AnyContent] = as.checkSession.async {
    implicit request =>
      submissionService.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsSchedule(SelfAssessment (debits = Seq(Debit(amount  = amountDue, dueDate = LocalDate.now()))), 0).map { monthsToSchedule =>
            Ok(views.calculate_instalments_form(CalculatorForm.createInstalmentForm(),
                                                ttpData.lengthOfArrangement, monthsToSchedule, ssttpcalculator.routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false))
          }

        case _ => Future.successful(redirectOnError)
      }
  }

  def submitCalculateInstalmentsUnAuth(): Action[AnyContent] = as.checkSession.async {
    implicit request =>
      submissionService.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsScheduleUnAuth(debits = Seq(Debit(amount  = amountDue, dueDate = LocalDate.now()))).flatMap { monthsToSchedule =>
            CalculatorForm.createInstalmentForm().bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(views.calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                                                                              monthsToSchedule, ssttpcalculator.routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false)))
              },
              validFormData => {
                submissionService.putTtpSessionCarrier(ttpData.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue),
                                                                                                                         Some(monthsToSchedule(validFormData.chosenMonths)))))).map { _ =>
                  Redirect(ssttpcalculator.routes.CalculatorController.getCheckCalculation())
                }
              }
            )
          }
        case _ => Future.successful(redirectOnError)
      }
  }

  def getCheckCalculation: Action[AnyContent] = as.checkSession.async {
    implicit request =>
      submissionService.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(_, Some(schedule))), _)) =>
          Future.successful(Ok(views.check_calculation(schedule, isSignedIn)))
        case _ => Future.successful(redirectOnError)
      }
  }

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.map {
      case Some(_@ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
        Ok(views.tax_liabilities(sa.debits, isSignedIn))
      case _ => redirectOnError
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, tp, CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        Ok(views.payment_today_question(CalculatorForm.payTodayForm, isSignedIn))
      case _ => redirectOnError
    }
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap[Result] {
      case Some(ttpData @ TTPSubmission(_, _, _, tp, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              submissionService.putTtpSessionCarrier(ttpData.copy(calculatorData = cd.copy(initialPayment = BigDecimal(0)))).map[Result] {
                _ => Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments())
              }
          }
        )
      case _ => Future.successful(redirectOnError)
    }
  }

  def getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (paymentToday.equals(BigDecimal(0))) Ok(views.payment_today_form(form, isSignedIn))
        else Ok(views.payment_today_form(form.fill(paymentToday), isSignedIn))
      case _ =>
        Logger.info("Missing required data for get payment today page")
        redirectOnError
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap[Result] {
      case Some(ttpSubmission @ TTPSubmission(_, _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_form(formWithErrors, isSignedIn))),
          validFormData => {
            submissionService.putTtpSessionCarrier(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).map { _ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getPaymentSummary())
            }
          }
        )
      case _ =>
        Logger.info("No TTP Data match in submitPaymentToday")
        Future.successful(redirectOnError)
    }
  }

  def getMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.putIsBPath(isBpath = true)
    submissionService.getTtpSessionCarrier.flatMap[Result] {
      case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        val form = CalculatorForm.createMonthlyAmountForm(
          lowerMonthlyPaymentBound(sa, calculatorData).toInt, upperMonthlyPaymentBound(sa, calculatorData).toInt)
        Future.successful(Ok(views.monthly_amount(
          form, upperMonthlyPaymentBound(sa, calculatorData), lowerMonthlyPaymentBound(sa, calculatorData)
        )))
      case _ =>
        Logger.info("No TTP Data match in getMonthlyPayment")
        Future.successful(redirectOnError)
    }
  }

  private def upperMonthlyPaymentBound(sa: SelfAssessment, calculatorData: CalculatorInput): String =
    roundUpToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.minimumMonthsAllowedTTP).toString

  private def lowerMonthlyPaymentBound(sa: SelfAssessment, calculatorData: CalculatorInput): String =
    roundDownToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.getMaxMonthsAllowed(sa, LocalDate.now())).toString

  private def roundDownToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100)

  private def roundUpToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100) + 100

  def submitMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { monthsToSchedule =>
          CalculatorForm.createMonthlyAmountForm(
            lowerMonthlyPaymentBound(sa, calculatorData).toInt, upperMonthlyPaymentBound(sa, calculatorData).toInt).bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(views.monthly_amount(
                  formWithErrors, upperMonthlyPaymentBound(sa, calculatorData), lowerMonthlyPaymentBound(sa, calculatorData)
                )))
              },
              validFormData => {
                submissionService.putAmount(validFormData.amount).map { _ =>
                  Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalmentsAB())
                }
              }
            )
        }
      case _ => Future.successful(redirectOnError)
    }
  }

  def getClosestSchedule(num: BigDecimal, schedule: List[CalculatorPaymentSchedule]): CalculatorPaymentSchedule =
    schedule.minBy(v => math.abs(v.getMonthlyInstalment.toInt - num.toInt))

  def getSurroundingSchedule(closestSchedule: CalculatorPaymentSchedule,
                             schedules:       List[CalculatorPaymentSchedule],
                             sa:              SelfAssessment): List[CalculatorPaymentSchedule] = {
    if (schedules.indexOf(closestSchedule) == 0)
      List(Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules), getElementNItemsAbove(2, closestSchedule, schedules))
        .flatten
    else if (schedules.indexOf(closestSchedule) == CalculatorService.getMaxMonthsAllowed(sa, LocalDate.now()) - 2)
      List(getElementNItemsBelow(2, closestSchedule, schedules), getElementNItemsBelow(1, closestSchedule, schedules), Some(closestSchedule))
        .flatten
    else
      List(getElementNItemsBelow(1, closestSchedule, schedules), Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules))
        .flatten
  }

  private def getElementNItemsAbove[A](n: Int, a: A, list: List[A]): Option[A] = {
    list.indexOf(a) match {
      case -1 => None
      case m  => Some(list(m + n))
    }
  }

  private def getElementNItemsBelow[A](n: Int, a: A, list: List[A]): Option[A] = {
    list.indexOf(a) match {
      case -1 => None
      case m => {
        Some(list(m - n))
      }
    }
  }

  def getPaymentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, initialPayment, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        Ok(views.payment_summary(debits, initialPayment))
      case _ =>
        Logger.info("Missing required data for what you owe review page")
        redirectOnError
    }
  }

  def getCalculateInstalmentsAB(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        submissionService.getAmount.flatMap{
          case Some(amount) =>
            calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).map { schedule =>
              {
                Ok(views.calculate_instalments_form_2(
                  ssttpcalculator.routes.CalculatorController.submitCalculateInstalmentsAB(),
                  CalculatorForm.createInstalmentForm(),
                  getSurroundingSchedule(getClosestSchedule(amount, schedule.values.toList), schedule.values.toList, sa)))
              }
            }
          case _ =>
            Logger.info("Missing required data for what you owe review page")
            Future.successful(redirectOnError)
        }
      case _ =>
        Logger.info("Missing required data for what you owe review page")
        Future.successful(redirectOnError)
    }
  }

  def submitCalculateInstalmentsAB(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        submissionService.getAmount.flatMap {
          case Some(amount) =>
            calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { schedule =>
              {
                CalculatorForm.createInstalmentForm().bindFromRequest().fold(
                  formWithErrors => {
                    Future.successful(BadRequest(views.calculate_instalments_form_2(
                      ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                      formWithErrors, getSurroundingSchedule(getClosestSchedule(amount, schedule.values.toList), schedule.values.toList, sa))))
                  },
                  validFormData => {
                    submissionService.putTtpSessionCarrier(ttpData.copy(schedule = Some(schedule(validFormData.chosenMonths)))).map { _ =>
                      Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay())
                    }
                  }
                )
              }
            }
          case _ => Future.successful(redirectOnError)
        }
    }
  }

  /**
   * Loads the calculator page. Several checks are performed:
   * - Checks to see if the session data is out of date and updates calculations if so
   * - If Taxpayer exists, load auth version of calculator
   * - If user input debits are not empty, load the calculator (avoids direct navigation)
   * - If schedule data is missing, update TTPSubmission
   */
  def getCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        if (CalculatorService.getMaxMonthsAllowed(sa, LocalDate.now()) >= CalculatorService.minimumMonthsAllowedTTP) {
          calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).map { monthsToSchedule =>
            Ok(views.calculate_instalments_form(CalculatorForm.createInstalmentForm(),
                                                ttpData.lengthOfArrangement, monthsToSchedule, ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(), loggedIn = true))
          }
        } else {
          //todo perhaps move these checks else where to eligbility service?
          submissionService.putTtpSessionCarrier(ttpData.copy(eligibilityStatus = Some(EligibilityStatus(eligible = false, Seq(TTPIsLessThenTwoMonths))))).map { _ => Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUsCalculatorInstalments()) }
        }

      case _ => Future.successful(redirectOnError)
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { monthsToSchedule =>
          CalculatorForm.createInstalmentForm().bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(views.calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                                                                            monthsToSchedule, ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(), loggedIn = true)))
            },
            validFormData => {
              submissionService.putTtpSessionCarrier(ttpData.copy(schedule = Some(monthsToSchedule(validFormData.chosenMonths)))).map { _ =>
                Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay())
              }
            }
          )
        }
      case _ => Future.successful(redirectOnError)
    }
  }
}
