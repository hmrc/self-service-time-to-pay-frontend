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

package ssttpcalculator

import java.time.LocalDate

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject._
import journey.Statuses.InProgress
import journey.{Journey, JourneyService}
import model._
import play.api.mvc.{AnyContent, _}
import req.RequestSupport
import ssttpcalculator.CalculatorForm.{createInstalmentForm, createMonthlyAmountForm, createPaymentTodayForm, payTodayForm}
import ssttpcalculator.CalculatorService.{createCalculatorInput, getMaxMonthsAllowed, minimumMonthsAllowedTTP}
import times.ClockProvider
import timetopaycalculator.cor.model.CalculatorInput
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CalculatorController @Inject() (
    mcc:               MessagesControllerComponents,
    calculatorService: CalculatorService,
    as:                Actions,
    journeyService:    JourneyService,
    requestSupport:    RequestSupport,
    views:             Views,
    clockProvider:     ClockProvider)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendBaseController(mcc) {

  import clockProvider._
  import requestSupport._

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getTaxLiabilities: $request")
    journeyService.getJourney.map {
      case _@ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Ok(views.tax_liabilities(sa.debits, isSignedIn))
      case journey =>
        JourneyLogger.info(s"CalculatorController.getTaxLiabilities: pattern match redirect on error", journey)
        technicalDifficulties(journey)
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPayTodayQuestion: $request")
    Ok(views.payment_today_question(payTodayForm, isSignedIn))
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: $request")

    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, InProgress, _, _, _, _, _, _, _, _, _, _, _) =>
        payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>
              val newJourney =
                journey.copy(maybeCalculatorData =
                  Some(createCalculatorInput(
                    0,
                    LocalDate.now(clockProvider.getClock).getDayOfMonth,
                    0,
                    journey.taxpayer.selfAssessment.debits.map(model.asDebitInput))))
              journeyService.saveJourney(newJourney).map[Result] {
                _ => Redirect(ssttpcalculator.routes.CalculatorController.getMonthlyPayment())
              }
          }
        )
      case journey =>
        JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentToday: $request")
    journeyService.getJourney.map {
      case journey @ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, SelfAssessmentDetails(_, _, debits, _))), _, _, _, _, _) if debits.nonEmpty =>
        val newJourney =
          journey.copy(maybeCalculatorData =
            Some(createCalculatorInput(
              0, LocalDate.now(clockProvider.getClock).getDayOfMonth, 0, debits.map(model.asDebitInput))))
        journeyService.saveJourney(newJourney)

        val form = createPaymentTodayForm(debits.map(_.amount).sum)

        if (newJourney.calculatorInput.initialPayment.equals(BigDecimal(0))) Ok(views.payment_today_form(form, isSignedIn))
        else Ok(views.payment_today_form(form.fill(newJourney.calculatorInput.initialPayment), isSignedIn))
      case journey =>
        JourneyLogger.info(s"CalculatorController.getPaymentToday: pattern match redirect on error", journey)
        technicalDifficulties(journey)
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPaymentToday: $request")
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, InProgress, _, _, _, _, _, _, Some(_), _, _, _, _) =>
        createPaymentTodayForm(journey.calculatorInput.debits.map(_.amount).sum).bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_form(formWithErrors, isSignedIn))),
          validFormData => {
            val newJourney = journey.copy(maybeCalculatorData = Some(journey.calculatorInput.copy(initialPayment = validFormData)))
            journeyService.saveJourney(newJourney).map { _ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getPaymentSummary())
            }
          }
        )
      case journey =>
        JourneyLogger.info(s"CalculatorController.submitPaymentToday: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getMonthlyPayment: $request")
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        val form = createMonthlyAmountForm(
          lowerMonthlyPaymentBound(sa, journey.calculatorInput).toInt, upperMonthlyPaymentBound(sa, journey.calculatorInput).toInt)
        Future.successful(Ok(views.monthly_amount(
          form, upperMonthlyPaymentBound(sa, journey.calculatorInput), lowerMonthlyPaymentBound(sa, journey.calculatorInput))))
      case journey =>
        JourneyLogger.info(s"CalculatorController.getMonthlyPayment: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  private def upperMonthlyPaymentBound(sa: SelfAssessmentDetails, calculatorData: CalculatorInput)(implicit hc: HeaderCarrier): String =
    Try(roundUpToNearestHundred((sa.debits.map(_.amount).sum - calculatorData.initialPayment) / minimumMonthsAllowedTTP).toString) match {
      case Success(s) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: [$s]")
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: ERROR - upperMonthlyPaymentBound - [$e]")
        throw e
    }

  private def lowerMonthlyPaymentBound(sa: SelfAssessmentDetails, calculatorData: CalculatorInput)(implicit request: Request[_]): String =
    Try(
      roundDownToNearestHundred(
        (sa.debits.map(_.amount).sum - calculatorData.initialPayment) /
          getMaxMonthsAllowed(sa, LocalDate.now(clockProvider.getClock))).toString) match {
        case Success(s) =>
          JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: [$s]")
          s
        case Failure(e) =>
          JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: ERROR [${e.toString}]")
          throw e
      }

  private def roundDownToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100)

  private def roundUpToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100) + 100

  def submitMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitMonthlyPayment: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>

        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).flatMap { _ =>
          createMonthlyAmountForm(
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
      case journey =>
        JourneyLogger.info(s"CalculatorController.submitMonthlyPayment: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  //  TODO work out what these functions and CalculatorPaymentScheduleExt are for and then refactor to make that clearer
  //  Ideally we would have tests of the controller functions that use these methods so that we could refactor and show no regression
  //  But it is hard to write tests because the intention is obscure.
  //  To remove the wart do something like this:
  //  def getClosestSchedule(amount: BigDecimal, schedules: Seq[CalculatorPaymentScheduleExt])
  //    (implicit hc: HeaderCarrier): CalculatorPaymentScheduleExt = {
  //
  //    def difference(schedule: CalculatorPaymentScheduleExt) =
  //      math.abs(schedule.schedule.getMonthlyInstalment.toInt - amount.toInt)
  //
  //    def closest(min: CalculatorPaymentScheduleExt, next: CalculatorPaymentScheduleExt) =
  //      if (difference(next) < difference(min)) next else min
  //
  //    Try(schedules.reduceOption(closest).getOrElse(throw new RuntimeException(s"No schedules for [$schedules]"))) match {
  //      case Success(s) => s
  //      case Failure(e) =>
  //        JourneyLogger.info(s"CalculatorController.getClosestSchedule: ERROR [$e]")
  //        throw e
  //    }
  //  }
  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  def getClosestSchedule(amount: BigDecimal, schedules: List[CalculatorPaymentScheduleExt])(implicit hc: HeaderCarrier): CalculatorPaymentScheduleExt =
    Try(schedules.minBy(v => math.abs(v.schedule.getMonthlyInstalment.toInt - amount.toInt))) match {
      case Success(s) =>
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.getClosestSchedule: ERROR [$e]")
        throw e
    }

  def getSurroundingSchedule(
      closestSchedule: CalculatorPaymentScheduleExt, schedules: List[CalculatorPaymentScheduleExt], sa: SelfAssessmentDetails)
    (implicit request: Request[_]): List[CalculatorPaymentScheduleExt] = {
    if (schedules.indexOf(closestSchedule) == 0)
      List(Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules), getElementNItemsAbove(2, closestSchedule, schedules))
    else if (schedules.indexOf(closestSchedule) == getMaxMonthsAllowed(sa, LocalDate.now(clockProvider.getClock)) - 2)
      List(getElementNItemsBelow(2, closestSchedule, schedules), getElementNItemsBelow(1, closestSchedule, schedules), Some(closestSchedule))
    else
      List(getElementNItemsBelow(1, closestSchedule, schedules), Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules))
  }.flatten

  private def getElementNItemsAbove[A](n: Int, a: A, list: List[A]): Option[A] =
    list.indexOf(a) match {
      case -1 => None
      case m  => Some(list(m + n))
    }

  private def getElementNItemsBelow[A](n: Int, a: A, list: List[A]): Option[A] =
    list.indexOf(a) match {
      case -1 => None
      case m  => Some(list(m - n))
    }
  //TODO work out what these functions and CalculatorPaymentScheduleExt are for and then refactor to make that clearer

  def getPaymentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentSummary: $request")
    journeyService.getJourney.map {
      case journey @ Journey(_, InProgress, _, _, _, _, _, _, Some(CalculatorInput(debits, initialPayment, _, _, _)), _, _, _, _) if debits.nonEmpty =>
        Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, initialPayment))
      case journey =>
        JourneyLogger.info(s"CalculatorController.getPaymentSummary: pattern match redirect on error", journey)
        technicalDifficulties(journey)
    }
  }

  def getCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getCalculateInstalments: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        JourneyLogger.info("CalculatorController.getCalculateInstalments", journey)
        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).map { schedule =>
          val closestSchedule: CalculatorPaymentScheduleExt = getClosestSchedule(journey.amount, schedule)
          val monthsToSchedule: List[CalculatorPaymentScheduleExt] = getSurroundingSchedule(closestSchedule, schedule, sa)

          Ok(views.calculate_instalments_form(
            routes.CalculatorController.submitCalculateInstalments(), createInstalmentForm(), monthsToSchedule))
        }

      case journey =>
        JourneyLogger.info("CalculatorController.getCalculateInstalment: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalments: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, InProgress, _, _, _, _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments", journey)

        calculatorService.getInstalmentsSchedule(sa, journey.calculatorInput.initialPayment).flatMap { schedules: List[CalculatorPaymentScheduleExt] =>
          createInstalmentForm().bindFromRequest().fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  views.calculate_instalments_form(
                    ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                    formWithErrors,
                    getSurroundingSchedule(getClosestSchedule(journey.amount, schedules), schedules, sa)))),
            validFormData =>
              journeyService.saveJourney(journey.copy(schedule = schedules.find(_.months == validFormData.chosenMonths))).map { _ =>
                Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay())
              }
          )
        }
      case journey =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }
}
