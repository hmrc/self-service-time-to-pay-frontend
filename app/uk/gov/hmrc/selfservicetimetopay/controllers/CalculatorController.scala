/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.forms.{CalculatorForm, MonthlyAmountForm}
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService._
import views.html.selfservicetimetopay.calculator._
import views.html.selfservicetimetopay.unauth._

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class CalculatorController @Inject() (val messagesApi:   play.api.i18n.MessagesApi,
                                      calculatorService: CalculatorService)
  extends TimeToPayController with play.api.i18n.I18nSupport {

  def submitSignIn: Action[AnyContent] = Action.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitSignIn: $request")
    Future.successful(Redirect(routes.ArrangementController.determineEligibility()))
  }

  def getPaymentPlanCalculator: Action[AnyContent] = Action { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentPlanCalculator: $request")
    Ok(payment_plan_calculator(isSignedIn))
  }

  def getAmountDue: Action[AnyContent] = Action { implicit request =>
    JourneyLogger.info(s"CalculatorController.getAmountDue: $request")
    Ok(amount_due(isSignedIn, CalculatorForm.createAmountDueForm()))
  }

  def submitAmountDue: Action[AnyContent] = Action.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitAmountDue: $request")
    CalculatorForm.createAmountDueForm().bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(amount_due(isSignedIn, formWithErrors))),
      amountDue => {
        //todo perhaps we dont need  a new one? this will whip the data from the auth journey is this ok ?User can just redo it?
        val dataWithAmount = TTPSubmission(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue.amount))))
        sessionCache.putTtpSessionCarrier(dataWithAmount).map { _ =>
          Redirect(routes.CalculatorController.getCalculateInstalmentsUnAuth())
        }
      }
    )

  }

  def getCalculateInstalmentsUnAuth(): Action[AnyContent] = Action.async {
    implicit request =>
      JourneyLogger.info(s"CalculatorController.getCalculateInstalmentsUnAuth: $request")
      sessionCache.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsSchedule(SelfAssessment (debits = Seq(Debit(amount  = amountDue, dueDate = LocalDate.now()))), 0).map { schedules: List[CalculatorPaymentScheduleExt] =>
            Ok(calculate_instalments_form(CalculatorForm.createInstalmentForm(),
                                          ttpData.lengthOfArrangement, schedules, routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false))
          }

        case maybeSubmission =>
          JourneyLogger.info(s"CalculatorController.getCalculateInstalmentsUnAuth - pattern match redirect on error", maybeSubmission)
          Future.successful(redirectOnError)
      }
  }

  def submitCalculateInstalmentsUnAuth(): Action[AnyContent] = Action.async {
    implicit request =>
      JourneyLogger.info(s"CalculatorController.submitCalculateInstalmentsUnAuth: $request")
      sessionCache.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(Some(amountDue), _)), _)) =>
          calculatorService.getInstalmentsScheduleUnAuth(debits = Seq(Debit(amount  = amountDue, dueDate = LocalDate.now()))).flatMap { monthsToSchedule =>
            CalculatorForm.createInstalmentForm().bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                                                                        monthsToSchedule, routes.CalculatorController.submitCalculateInstalmentsUnAuth(), isSignedIn, false)))
              },
              validFormData => {
                sessionCache.putTtpSessionCarrier(ttpData.copy(notLoggedInJourneyInfo = Some(NotLoggedInJourneyInfo(Some(amountDue),
                                                                                                                    Some(monthsToSchedule(validFormData.chosenMonths)))))).map { _ =>
                  Redirect(routes.CalculatorController.getCheckCalculation())
                }
              }
            )
          }
        case maybeSubmission =>
          JourneyLogger.info(s"CalculatorController.submitCalculateInstalmentsUnAuth: pattern match redirect on error", maybeSubmission)
          Future.successful(redirectOnError)
      }
  }

  def getCheckCalculation: Action[AnyContent] = Action.async {
    implicit request =>
      JourneyLogger.info(s"CalculatorController.getCheckCalculation: $request")
      sessionCache.getTtpSessionCarrier.flatMap {
        case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, _, _, Some(NotLoggedInJourneyInfo(_, Some(schedule))), _)) =>
          Future.successful(Ok(check_calculation(schedule, isSignedIn)))
        case maybeSubmission =>
          JourneyLogger.info(s"CalculatorController.getCheckCalculation: pattern match redirect on error", maybeSubmission)
          Future.successful(redirectOnError)
      }
  }

  def getTaxLiabilities: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getTaxLiabilities: $request")
    sessionCache.getTtpSessionCarrier.map {
      case Some(_@ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
        Ok(tax_liabilities(sa.debits, isSignedIn))
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.getTaxLiabilities: pattern match redirect on error", maybeSubmission)
        redirectOnError
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getPayTodayQuestion: $request")
    sessionCache.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, tp, CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        Ok(payment_today_question(CalculatorForm.payTodayForm, isSignedIn))
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.getPayTodayQuestion: pattern match redirect on error", maybeSubmission)
        redirectOnError
    }
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: $request")
    sessionCache.getTtpSessionCarrier.flatMap[Result] {
      case Some(ttpData @ TTPSubmission(_, _, _, tp, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              sessionCache.putTtpSessionCarrier(ttpData.copy(calculatorData = cd.copy(initialPayment = BigDecimal(0)))).map[Result] {
                _ => Redirect(routes.CalculatorController.getMonthlyPayment())
              }
          }
        )
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  def getPaymentToday: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentToday: $request")
    sessionCache.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, paymentToday, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (paymentToday.equals(BigDecimal(0))) Ok(payment_today_form(form, isSignedIn))
        else Ok(payment_today_form(form.fill(paymentToday), isSignedIn))
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.getPaymentToday: pattern match redirect on error", maybeSubmission)
        redirectOnError
    }
  }

  def submitPaymentToday: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPaymentToday: $request")
    sessionCache.getTtpSessionCarrier.flatMap[Result] {
      case Some(ttpSubmission @ TTPSubmission(_, _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _)) =>
        CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(payment_today_form(formWithErrors, isSignedIn))),
          validFormData => {
            sessionCache.putTtpSessionCarrier(ttpSubmission.copy(calculatorData = cd.copy(initialPayment = validFormData))).map { _ =>
              Redirect(routes.CalculatorController.getPaymentSummary())
            }
          }
        )
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.submitPaymentToday: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  def getMonthlyPayment: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getMonthlyPayment: $request")
    sessionCache.putIsBPath(isBpath = true)
    sessionCache.getTtpSessionCarrier.flatMap[Result] {
      case Some(TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        val form = CalculatorForm.createMonthlyAmountForm(
          lowerMonthlyPaymentBound(sa, calculatorData).toInt, upperMonthlyPaymentBound(sa, calculatorData).toInt)
        Future.successful(Ok(monthly_amount(
          form, upperMonthlyPaymentBound(sa, calculatorData), lowerMonthlyPaymentBound(sa, calculatorData), isSignedIn
        )))
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.getMonthlyPayment: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  private def upperMonthlyPaymentBound(sa: SelfAssessment, calculatorData: CalculatorInput)(implicit hc: HeaderCarrier): String = {
    val result = Try(roundUpToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / minimumMonthsAllowedTTP).toString)
    result match {
      case Success(s) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: [$s]")
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: ERROR - upperMonthlyPaymentBound - [$e]")
        throw e
    }
  }

  private def lowerMonthlyPaymentBound(sa: SelfAssessment, calculatorData: CalculatorInput)(implicit hc: HeaderCarrier): String = {
    val result = Try(roundDownToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / getMaxMonthsAllowed(sa, LocalDate.now())).toString)
    result match {
      case Success(s) =>
        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: [$s]")
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: ERROR [$e]")
        throw e
    }
  }

  private def roundDownToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100)

  private def roundUpToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100) + 100

  def submitMonthlyPayment: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.submitMonthlyPayment: $request")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { monthsToSchedule =>
          CalculatorForm.createMonthlyAmountForm(
            lowerMonthlyPaymentBound(sa, calculatorData).toInt, upperMonthlyPaymentBound(sa, calculatorData).toInt).bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(monthly_amount(
                  formWithErrors, upperMonthlyPaymentBound(sa, calculatorData), lowerMonthlyPaymentBound(sa, calculatorData), isSignedIn
                )))
              },
              validFormData => {
                sessionCache.putAmount(validFormData.amount).map { _ =>
                  Redirect(routes.CalculatorController.getCalculateInstalments())
                }
              }
            )
        }
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.submitMonthlyPayment: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  def getClosestSchedule(amount: BigDecimal, schedules: List[CalculatorPaymentScheduleExt])(implicit hc: HeaderCarrier): CalculatorPaymentScheduleExt = {
    val result = Try(schedules.minBy(v => math.abs(v.schedule.getMonthlyInstalment.toInt - amount.toInt)))
    result match {
      case Success(s) =>
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.getClosestSchedule: ERROR [$e]")
        throw e
    }
  }

  def getSurroundingSchedule(closestSchedule: CalculatorPaymentScheduleExt,
                             schedules:       List[CalculatorPaymentScheduleExt],
                             sa:              SelfAssessment)(implicit hc: HeaderCarrier): List[CalculatorPaymentScheduleExt] = {
    if (schedules.indexOf(closestSchedule) == 0)
      List(Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules), getElementNItemsAbove(2, closestSchedule, schedules))
        .flatten
    else if (schedules.indexOf(closestSchedule) == getMaxMonthsAllowed(sa, LocalDate.now()) - 2)
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

  def getPaymentSummary: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentSummary: $request")
    sessionCache.getTtpSessionCarrier.map {
      case Some(TTPSubmission(_, _, _, _, CalculatorInput(debits, initialPayment, _, _, _, _), _, _, _, _, _)) if debits.nonEmpty =>
        Ok(payment_summary(debits, initialPayment))
      case maybeSubmission =>
        JourneyLogger.info(s"CalculatorController.getPaymentSummary: pattern match redirect on error", maybeSubmission)
        redirectOnError
    }
  }

  def getCalculateInstalments(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getCalculateInstalments: ${request}")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        JourneyLogger.info("CalculatorController.getCalculateInstalments", ttpData)
        sessionCache.getAmount.flatMap{
          case Some(amount) =>
            calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).map { schedules: List[CalculatorPaymentScheduleExt] =>
              {
                val closestSchedule: CalculatorPaymentScheduleExt = getClosestSchedule(amount, schedules)
                val monthsToSchedule: List[CalculatorPaymentScheduleExt] = getSurroundingSchedule(closestSchedule, schedules, sa)

                Ok(calculate_instalments_form_2(
                  routes.CalculatorController.submitCalculateInstalments(),
                  CalculatorForm.createInstalmentForm(),
                  monthsToSchedule))
              }
            }
          case _ =>
            JourneyLogger.info("CalculatorController.getCalculateInstalments: amount not found - redirect on error", ttpData)
            Future.successful(redirectOnError)
        }
      case maybeSubmission =>
        JourneyLogger.info("CalculatorController.getCalculateInstalment: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalments: $request")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments", ttpData)
        sessionCache.getAmount.flatMap {
          case Some(amount) =>
            calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { schedules: List[CalculatorPaymentScheduleExt] =>
              {
                CalculatorForm.createInstalmentForm().bindFromRequest().fold(
                  formWithErrors => {
                    Future.successful(BadRequest(calculate_instalments_form_2(
                      routes.CalculatorController.submitCalculateInstalments(),
                      formWithErrors, getSurroundingSchedule(getClosestSchedule(amount, schedules), schedules, sa))))
                  },
                  validFormData => {
                    sessionCache
                      .putTtpSessionCarrier(
                        ttpData.copy(
                          schedule = schedules.find(_.months == validFormData.chosenMonths)
                        )
                      )
                      .map(_ => Redirect(routes.ArrangementController.getChangeSchedulePaymentDay()))
                  }
                )
              }
            }
          case _ =>
            JourneyLogger.info("CalculatorController.submitCalculateInstalments: amount not found - redirect on error", ttpData)
            Future.successful(redirectOnError)
        }
      case maybeSubmission =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments: pattern match redirect on error", maybeSubmission)
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
  def getCalculateInstalmentsOld: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.getCalculateInstalmentsOld $request")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        JourneyLogger.info("CalculatorController.getCalculateInstalmentsOld: eligible", ttpData)
        if (getMaxMonthsAllowed(sa, LocalDate.now()) >= minimumMonthsAllowedTTP) {
          calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).map { monthsToSchedule =>
            Ok(calculate_instalments_form(CalculatorForm.createInstalmentForm(),
                                          ttpData.lengthOfArrangement, monthsToSchedule, routes.CalculatorController.submitCalculateInstalments(), loggedIn = true))
          }
        } else {
          //todo perhaps move these checks else where to eligbility service?
          val newSubmission = ttpData.copy(eligibilityStatus = Some(EligibilityStatus(eligible = false, Seq(TTPIsLessThenTwoMonths))))
          JourneyLogger.info("CalculatorController.getCalculateInstalmentsOld: not eligible, redirecting to call us calc installments", ttpData)
          sessionCache.putTtpSessionCarrier(newSubmission).map { _ => Redirect(routes.SelfServiceTimeToPayController.getTtpCallUsCalculatorInstalments()) }
        }
      case maybeSubmission =>
        JourneyLogger.info("CalculatorController.getCalculateInstalmentsOld: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }

  def submitCalculateInstalmentsOld(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalmentsOld: $request")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(sa))), calculatorData, _, _, _, _, _)) =>
        calculatorService.getInstalmentsSchedule(sa, calculatorData.initialPayment).flatMap { monthsToSchedule: List[CalculatorPaymentScheduleExt] =>
          CalculatorForm.createInstalmentForm().bindFromRequest().fold(
            formWithErrors => {
              JourneyLogger.info(s"CalculatorController.submitCalculateInstalmentsOld: form with errors")
              Future.successful(BadRequest(calculate_instalments_form(formWithErrors, ttpData.lengthOfArrangement,
                                                                      monthsToSchedule, routes.CalculatorController.submitCalculateInstalments(), loggedIn = true)))
            },
            validFormData => {
              sessionCache.putTtpSessionCarrier(ttpData.copy(schedule = monthsToSchedule.find(_.months == validFormData.chosenMonths))).map { _ =>
                Redirect(routes.ArrangementController.getChangeSchedulePaymentDay())
              }
            }
          )
        }
      case maybeSubmission =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalmentsOld: pattern match redirect on error", maybeSubmission)
        Future.successful(redirectOnError)
    }
  }
}
