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

import java.time.{Clock, LocalDate}

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject._
import journey.{Journey, JourneyService}
import model._
import play.api.Logger
import play.api.mvc.{AnyContent, _}
import req.RequestSupport
import timetopaycalculator.cor.model.{CalculatorInput, PaymentSchedule}
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class CalculatorController @Inject() (
    mcc:               MessagesControllerComponents,
    calculatorService: CalculatorService,
    as:                Actions,
    journeyService:    JourneyService,
    requestSupport:    RequestSupport,
    views:             Views,
    clock:             Clock)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._

  def getAmountDue: Action[AnyContent] = as.action { implicit request =>
    ???
  }

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.map {
      case _@ Journey(_, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Ok(views.tax_liabilities(sa.debits, isSignedIn))
      case _ => redirectToStartPage
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser{ implicit request =>
    Ok(views.payment_today_question(CalculatorForm.payTodayForm, isSignedIn))
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, _, _, _, _, tp, _, _, _, _, _) =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              val newJourney = journey.copy(
                maybeCalculatorData = Some(CalculatorService.createCalculatorInput(0, LocalDate.now(clock).getDayOfMonth, 0,
                                                                                      journey.taxpayer.selfAssessment.debits.map(model.asDebitInput), clock))
              )
              journeyService.saveJourney(newJourney).map[Result] {
                _ => Redirect(ssttpcalculator.routes.CalculatorController.getMonthlyPayment())
              }
          }
        )
      case _ => Future.successful(redirectToStartPage)
    }
  }

  def getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.map {
      case journey @ Journey(_, _, _, _, _, Some(Taxpayer(_, _, SelfAssessmentDetails(_, _, debits, _))), _, _, _, _, _) if debits.nonEmpty =>
        val newJourney = journey.copy(maybeCalculatorData =
          Some(CalculatorService.createCalculatorInput(0, LocalDate.now(clock).getDayOfMonth, 0,
                                                          debits.map(model.asDebitInput), clock)))
        journeyService.saveJourney(newJourney)
        val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
        if (newJourney.calculatorInput.initialPayment.equals(BigDecimal(0))) Ok(views.payment_today_form(form, isSignedIn))
        else Ok(views.payment_today_form(form.fill(newJourney.calculatorInput.initialPayment), isSignedIn))
      case _ =>
        Logger.info("Missing required data for get payment today page")
        redirectToStartPage
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, _, _, _, _, _, Some(calculatoInput), _, _, _, _) =>

        CalculatorForm.createPaymentTodayForm(journey.calculatorInput.debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_form(formWithErrors, isSignedIn))),
          validFormData => {
            val newJourney = journey.copy(maybeCalculatorData = Some(journey.calculatorInput.copy(initialPayment = validFormData)))
            journeyService.saveJourney(newJourney).map { _ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getPaymentSummary())
            }
          }
        )
      case _ =>
        Logger.info("No TTP Data match in submitPaymentToday")
        Future.successful(redirectToStartPage)
    }
  }

  def getMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, _, _, _, _, Some(Taxpayer(_, _, sa)), calculatorData, _, _, _, _) =>
        val form = CalculatorForm.createMonthlyAmountForm(
          lowerMonthlyPaymentBound(sa, journey.calculatorInput).toInt, upperMonthlyPaymentBound(sa, journey.calculatorInput).toInt)
        Future.successful(Ok(views.monthly_amount(
          form, upperMonthlyPaymentBound(sa, journey.calculatorInput), lowerMonthlyPaymentBound(sa, journey.calculatorInput)
        )))
      case _ =>
        Logger.info("No TTP Data match in getMonthlyPayment")
        Future.successful(redirectToStartPage)
    }
  }

  private def upperMonthlyPaymentBound(sa: SelfAssessmentDetails, calculatorData: CalculatorInput): String =
    roundUpToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.minimumMonthsAllowedTTP).toString

  private def lowerMonthlyPaymentBound(sa: SelfAssessmentDetails, calculatorData: CalculatorInput): String = {
    roundDownToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.getMaxMonthsAllowed(sa, LocalDate.now(clock))).toString
  }

  private def roundDownToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100)

  private def roundUpToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100) + 100

  def submitMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, _, _, _, _, Some(Taxpayer(_, _, sa)), calculatorData, _, _, _, _) =>

        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).flatMap { monthsToSchedule =>
          CalculatorForm.createMonthlyAmountForm(
            lowerMonthlyPaymentBound(sa, journey.calculatorInput).toInt, upperMonthlyPaymentBound(sa, journey.calculatorInput).toInt).bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(views.monthly_amount(
                  formWithErrors, upperMonthlyPaymentBound(sa, journey.calculatorInput), lowerMonthlyPaymentBound(sa, journey.calculatorInput)
                )))
              },
              validFormData => {

                journeyService.saveJourney(journey.copy(maybeAmount = Some(validFormData.amount))).map { _ =>
                  Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments())
                }
              }
            )
        }
      case _ => Future.successful(redirectToStartPage)
    }
  }

  def getClosestSchedule(num: BigDecimal, schedule: List[PaymentSchedule]): PaymentSchedule =
    schedule.minBy(v => math.abs(v.getMonthlyInstalment.toInt - num.toInt))

  def getSurroundingSchedule(closestSchedule: PaymentSchedule,
                             schedules:       List[PaymentSchedule],
                             sa:              SelfAssessmentDetails): List[PaymentSchedule] = {
    if (schedules.indexOf(closestSchedule) == 0)
      List(Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules), getElementNItemsAbove(2, closestSchedule, schedules))
        .flatten
    else if (schedules.indexOf(closestSchedule) == CalculatorService.getMaxMonthsAllowed(sa, LocalDate.now(clock)) - 2)
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
    journeyService.getJourney.map {
      case journey @ Journey(_, _, _, _, _, _, Some(CalculatorInput(debits, initialPayment, _, _, _)), _, _, _, _) if debits.nonEmpty =>
        Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, initialPayment))
      case _ =>
        Logger.info("Missing required data for what you owe review page")
        redirectToStartPage
    }
  }

  def getCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, _, _, _, _, Some(Taxpayer(_, _, sa)), calculatorData, _, _, _, _) =>
        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).map { schedule =>
          {
            Ok(views.calculate_instalments_form_2(
              ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
              CalculatorForm.createInstalmentForm(),
              getSurroundingSchedule(getClosestSchedule(journey.amount, schedule.values.toList), schedule.values.toList, sa)))
          }
        }

      case _ =>
        Logger.info("Missing required data for what you owe review page")
        Future.successful(redirectToStartPage)
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, _, _, _, _, Some(Taxpayer(_, _, sa)), calculatorData, _, _, _, _) =>

        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).flatMap { schedule =>
          {
            CalculatorForm.createInstalmentForm().bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(views.calculate_instalments_form_2(
                  ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                  formWithErrors, getSurroundingSchedule(getClosestSchedule(journey.amount, schedule.values.toList), schedule.values.toList, sa))))
              },
              validFormData => {
                journeyService.saveJourney(journey.copy(schedule = Some(schedule(validFormData.chosenMonths)))).map { _ =>
                  Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay())
                }
              }
            )
          }
        }

    }
  }
}
