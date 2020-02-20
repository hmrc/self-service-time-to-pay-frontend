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
import journey.{Journey, JourneyService, Statuses}
import model._
import play.api.mvc.{AnyContent, _}
import req.RequestSupport
import times.ClockProvider
import timetopaycalculator.cor.model.CalculatorInput
import timetopaytaxpayer.cor.TaxpayerConnector
import timetopaytaxpayer.cor.model.{ReturnsAndDebits, SaUtr, TaxpayerDetails}
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
    clockProvider:     ClockProvider,
    taxpayerConnector: TaxpayerConnector)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._
  import clockProvider._

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getTaxLiabilities: $request")

    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, _, _, _, _, _, _) =>
        taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).map { returnsAndDebits =>
          val debits = returnsAndDebits.debits
          val view = views.tax_liabilities(debits, isSignedIn)
          Ok(view)
        }
      case journey: Journey =>
        JourneyLogger.info(s"CalculatorController.getTaxLiabilities: pattern match redirect on error", journey)
        Future successful technicalDifficulties(journey)
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPayTodayQuestion: $request")
    Ok(views.payment_today_question(CalculatorForm.payTodayForm, isSignedIn))
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: $request")

    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, _, _, _, _, _, _) =>
        CalculatorForm.payTodayForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
            case PayTodayQuestion(Some(true)) =>
              Future.successful(Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday()))
            case PayTodayQuestion(Some(false)) =>

              taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).flatMap {
                returnsAndDebits =>
                  val debits = returnsAndDebits.debits.map(d => model.asDebitInput(d))
                  val newJourney = journey.copy(maybeCalculatorData =
                    Some(CalculatorService.createCalculatorInput(0, LocalDate.now(clockProvider.getClock).getDayOfMonth, 0, debits)))

                  journeyService.saveJourney(newJourney).map[Result] {
                    _ => Redirect(ssttpcalculator.routes.CalculatorController.getMonthlyPayment())
                  }
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
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(TaxpayerDetails(_, _, _, _)), _, _, _, _, _) =>
        taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).map {
          returnsAndDebits =>
            val debits = returnsAndDebits.debits
            if (debits.nonEmpty) {
              val newJourney = journey.copy(maybeCalculatorData =
                Some(CalculatorService.createCalculatorInput(0, LocalDate.now(clockProvider.getClock).getDayOfMonth, 0,
                                                                debits.map(model.asDebitInput))))
              journeyService.saveJourney(newJourney)
              val form = CalculatorForm.createPaymentTodayForm(debits.map(_.amount).sum)
              if (newJourney.calculatorInput.initialPayment.equals(BigDecimal(0))) Ok(views.payment_today_form(form, isSignedIn))
              else Ok(views.payment_today_form(form.fill(newJourney.calculatorInput.initialPayment), isSignedIn))
            } //TODO this old func would've sent it to the other case AFAIK but surely this is preferred
            else {
              JourneyLogger.info(s"CalculatorController.getPaymentToday: debits empty error", journey)
              technicalDifficulties(journey)
            }
        }
      case journey =>
        JourneyLogger.info(s"CalculatorController.getPaymentToday: pattern match redirect on error", journey)
        technicalDifficulties(journey)
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPaymentToday: $request")
    journeyService.getJourney.flatMap[Result] {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, _, _, _, _, _, _) =>
        CalculatorForm.createPaymentTodayForm(journey.calculatorInput.debits.map(_.amount).sum).bindFromRequest().fold(
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
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(TaxpayerDetails(_, _, _, _)), _, _, _, _, _) =>
        taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).flatMap {
          returnsAndDebits =>
            val form = CalculatorForm.createMonthlyAmountForm(
              lowerMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput).toInt, upperMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput).toInt)
            Future.successful(Ok(views.monthly_amount(
              form, upperMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput), lowerMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput)
            )))
        }
      case journey =>
        JourneyLogger.info(s"CalculatorController.getMonthlyPayment: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  private def upperMonthlyPaymentBound(returnsAndDebits: ReturnsAndDebits, calculatorData: CalculatorInput)(implicit hc: HeaderCarrier): String = {
    val result = Try(roundUpToNearestHundred((returnsAndDebits.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.minimumMonthsAllowedTTP).toString)
    result match {
      case Success(s) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: [$s]")
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.upperMonthlyPaymentBound: ERROR - upperMonthlyPaymentBound - [$e]")
        throw e
    }
  }

  private def lowerMonthlyPaymentBound(returnsAndDebits: ReturnsAndDebits, calculatorData: CalculatorInput)(implicit request: Request[_]): String = {
    val result: Try[String] = Try(roundDownToNearestHundred((returnsAndDebits.debits.map(_.amount).sum - calculatorData.initialPayment) / CalculatorService.getMaxMonthsAllowed(returnsAndDebits, LocalDate.now(clockProvider.getClock))).toString)
    result match {
      case Success(s) =>
        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: [$s]")
        s
      case Failure(e) =>
        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: ERROR [${e.toString}]")
        throw e
    }
  }

  private def roundDownToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100)

  private def roundUpToNearestHundred(value: BigDecimal): BigDecimal = BigDecimal((value.intValue() / 100) * 100) + 100

  def submitMonthlyPayment: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitMonthlyPayment: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(TaxpayerDetails(_, _, _, _)), _, _, _, _, _) =>

        //TODO the below variable was kinda used in the original (maybe?) so may need to confirm the func is the same
        //monthsToSchedule <- calculatorService.getInstalmentsSchedule(returnsAndDebits, journey.calculatorInput.initialPayment)
        taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).flatMap { returnsAndDebits =>
          CalculatorForm.createMonthlyAmountForm(
            lowerMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput).toInt, upperMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput).toInt).bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(views.monthly_amount(
                  formWithErrors, upperMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput), lowerMonthlyPaymentBound(returnsAndDebits, journey.calculatorInput)
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

  def getSurroundingSchedule(closestSchedule:  CalculatorPaymentScheduleExt,
                             schedules:        List[CalculatorPaymentScheduleExt],
                             returnsAndDebits: ReturnsAndDebits)(implicit request: Request[_]): List[CalculatorPaymentScheduleExt] = {
    if (schedules.indexOf(closestSchedule) == 0)
      List(Some(closestSchedule), getElementNItemsAbove(1, closestSchedule, schedules), getElementNItemsAbove(2, closestSchedule, schedules))
        .flatten
    else if (schedules.indexOf(closestSchedule) == CalculatorService.getMaxMonthsAllowed(returnsAndDebits, LocalDate.now(clockProvider.getClock)) - 2)
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
    JourneyLogger.info(s"CalculatorController.getPaymentSummary: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, _, Some(CalculatorInput(debits, initialPayment, _, _, _)), _, _, _, _) //TODO why did this use to check the debits in calcInput then operate on a different debits??
      //TODO Answer for above is because there are two variations of debits one which is with less fields aka basically pointless but in a lib
      //if debits.nonEmpty =>
      // Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, initialPayment))
      if debits.nonEmpty =>
        taxpayerConnector.getReturnsAndDebits(journey.taxpayer.utr).map(returnsAndDebits => Ok(views.payment_summary(returnsAndDebits.debits, initialPayment)))

      case journey =>
        JourneyLogger.info(s"CalculatorController.getPaymentSummary: pattern match redirect on error", journey)
        technicalDifficulties(journey)
    }
  }

  def getCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getCalculateInstalments: ${request}")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(TaxpayerDetails(_, _, _, _)), _, _, _, _, _) =>
        JourneyLogger.info("CalculatorController.getCalculateInstalments", journey)

        for {
          returnsAndDebits <- taxpayerConnector.getReturnsAndDebits((journey.taxpayer.utr))
          calculatorPaymentSchedule <- calculatorService.getInstalmentsSchedule(returnsAndDebits, journey.calculatorInput.initialPayment)
        } yield {
          val closestSchedule: CalculatorPaymentScheduleExt = getClosestSchedule(journey.amount, calculatorPaymentSchedule)
          val monthsToSchedule: List[CalculatorPaymentScheduleExt] = getSurroundingSchedule(closestSchedule, calculatorPaymentSchedule, returnsAndDebits)
          Ok(views.calculate_instalments_form_2(
            routes.CalculatorController.submitCalculateInstalments(),
            CalculatorForm.createInstalmentForm(),
            monthsToSchedule))
        }

      case journey =>
        JourneyLogger.info("CalculatorController.getCalculateInstalment: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalments: $request")
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(TaxpayerDetails(_, _, _, _)), _, _, _, _, _) =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments", journey)

        for {
          returnsAndDebits <- taxpayerConnector.getReturnsAndDebits((journey.taxpayer.utr))
          calculatorPaymentSchedule <- calculatorService.getInstalmentsSchedule(returnsAndDebits, journey.calculatorInput.initialPayment)
          result <- CalculatorForm.createInstalmentForm().bindFromRequest().fold(
            formWithErrors => {
              Future.successful(
                BadRequest(
                  views.calculate_instalments_form_2(
                    ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                    formWithErrors,
                    getSurroundingSchedule(getClosestSchedule(journey.amount, calculatorPaymentSchedule), calculatorPaymentSchedule, returnsAndDebits)
                  )
                )
              )
            },
            validFormData => {
              journeyService.saveJourney(
                journey.copy(
                  schedule = calculatorPaymentSchedule.find(_.months == validFormData.chosenMonths)
                )
              ).map { _ =>
                  Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay())
                }
            }
          )
        } yield {
          result
        }

      case journey =>
        JourneyLogger.info("CalculatorController.submitCalculateInstalments: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

}
