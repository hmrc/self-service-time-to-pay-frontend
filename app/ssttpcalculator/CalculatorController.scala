/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions

import javax.inject._
import journey.{Journey, JourneyService, PaymentToday, PaymentTodayAmount}
import play.api.mvc._
import req.RequestSupport
import ssttpcalculator.CalculatorForm.{createPaymentTodayForm, payTodayForm, selectPlanForm}
import model.{PaymentPlanOption, PaymentSchedule}
import ssttpcalculator.legacy.CalculatorService
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views
import CalculatorType._
import play.api.data.Form
import util.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorController @Inject() (
    mcc:                 MessagesControllerComponents,
    paymentPlansService: PaymentPlansService, // calculator type feature flag: used by PaymentOptimised calculator feature
    instalmentsService:  InstalmentsService, // calculator type feature flag: used by PaymentOptimised calculator feature
    calculatorService:   CalculatorService, // calculator type feature flag: used by Legacy calculator feature
    as:                  Actions,
    journeyService:      JourneyService,
    requestSupport:      RequestSupport,
    views:               Views,
    clockProvider:       ClockProvider)(implicit appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendBaseController(mcc) with Logging {

  import requestSupport._

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Tax liabilities'")

      Ok(views.tax_liabilities(journey.debits, isSignedIn))
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Pay today question'")

      val formData = PayTodayQuestion(journey.maybePaymentToday.map(_.value))
      val form = payTodayForm.fill(formData)
      Ok(views.payment_today_question(form, isSignedIn))
    }
  }

  /**
   * Checks the response for the pay today question. If yes navigate to payment today page
   * otherwise navigate to calculator page and set the initial payment to 0
   */
  def submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Submit 'Pay today question'")

      payTodayForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
          case PayTodayQuestion(Some(true)) =>
            val newJourney = journey.copy(
              maybePaymentToday = Some(PaymentToday(true))
            )
            journeyService.saveJourney(newJourney).map(_ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday()))

          case PayTodayQuestion(Some(false)) =>
            val newJourney = journey.copy(
              maybePaymentToday       = Some(PaymentToday(false)),
              maybePaymentTodayAmount = None //resetting payment this amount just in case if user goes back and overrides this value
            )

            journeyService.saveJourney(newJourney).map(
              _ => Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay()))
          case PayTodayQuestion(None) =>
            val msg = s"could not submitPayTodayQuestion, payToday must be defined"
            val ex = new RuntimeException(msg)
            journeyLogger.warn("Illegal state")
            throw ex
        }
      )
    }
  }

  def getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Payment Today'")

      val debits = journey.debits
      val emptyForm = createPaymentTodayForm(debits.map(_.amount).sum)
      val formWithData = journey.maybePaymentTodayAmount.map(paymentTodayAmount =>
        emptyForm.fill(CalculatorPaymentTodayForm(paymentTodayAmount.value))).getOrElse(emptyForm)
      Ok(views.payment_today_form(formWithData, debits, isSignedIn))
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Submit 'Payment today'")

      val debits = journey.debits
      createPaymentTodayForm(journey.taxpayer.selfAssessment.debits.map(_.amount).sum).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.payment_today_form(formWithErrors, debits, isSignedIn))),
        (form: CalculatorPaymentTodayForm) => {
          val newJourney = journey.copy(
            maybePaymentTodayAmount = Some(PaymentTodayAmount(form.amount))
          )
          journeyService.saveJourney(newJourney).map { _ =>
            Redirect(ssttpcalculator.routes.CalculatorController.getPaymentSummary())
          }
        }
      )
    }
  }

  def getPaymentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Payment summary'")

      journey.maybePaymentToday match {
        case Some(PaymentToday(true)) =>
          val payToday = journey.paymentToday
          Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, payToday, journey.upfrontPayment))
        case Some(PaymentToday(false)) =>
          Redirect(ssttpcalculator.routes.CalculatorController.getPayTodayQuestion())
        case None =>
          journeyLogger.warn("Illegal state")
          throw new RuntimeException(s"payToday must be defined")
      }
    }
  }

  def getCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Calculate instalments'")

      val sa = journey.taxpayer.selfAssessment

      appConfig.calculatorType match {

        case Legacy =>
          journeyLogger.info(s"Using legacy calculator")

          val availablePaymentSchedules = calculatorService.allAvailableSchedules(sa, journey.safeUpfrontPayment, journey.maybePaymentDayOfMonth)
          val closestSchedule = calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
          val defaultPlanOptions = calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

          defaultPlanOptions.values.toSeq.sortBy(_.instalmentAmount).headOption match {
            case None =>
              journeyLogger.info(s"Legacy calculator - No viable plans available: redirecting to 'We cannot agree your payment plan'")
              Redirect(ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP())

            case Some(_) =>
              val minCustomAmount = defaultPlanOptions.values.toSeq.maxBy(_.instalmentAmount).instalmentAmount
              val maxCustomAmount = availablePaymentSchedules.maxBy(_.instalmentAmount).instalmentAmount
              val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

              journeyLogger.info(s"Legacy calculator - Displaying plans: ${planOptions.keys}")
              okPlanForm(Legacy, minCustomAmount, maxCustomAmount, planOptions, journey.maybePlanSelection)
          }

        case PaymentOptimised =>
          journeyLogger.info(s"Using payment optimised calculator")

          val defaultPlanOptions = paymentPlansService.defaultSchedules(
            sa,
            journey.safeUpfrontPayment,
            journey.maybePaymentDayOfMonth,
            journey.remainingIncomeAfterSpending
          )

          defaultPlanOptions.values.toSeq.sortBy(_.instalmentAmount).headOption match {
            case None =>
              journeyLogger.info(s"Payment optimised calculator - No viable plans available: redirecting to 'We cannot agree your payment plan'")
              Redirect(ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP())

            case Some(scheduleWithSmallestInstalmentAmount) =>
              val minCustomAmount = scheduleWithSmallestInstalmentAmount.instalmentAmount
              val maxCustomAmount = paymentPlansService.maximumPossibleInstalmentAmount(journey)
              val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

              journeyLogger.info(s"Payment optimised calculator - Displaying plans: ${planOptions.keys}")
              okPlanForm(PaymentOptimised, minCustomAmount, maxCustomAmount, planOptions, journey.maybePlanSelection)
          }
      }
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Submit 'Calculate instalments'")

      val sa = journey.taxpayer.selfAssessment

      appConfig.calculatorType match {

        case CalculatorType.Legacy =>
          journeyLogger.info(s"Using legacy calculator")

          val availablePaymentSchedules = calculatorService.allAvailableSchedules(sa, journey.safeUpfrontPayment, journey.maybePaymentDayOfMonth)
          val closestSchedule = calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
          val defaultPlanOptions = calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

          val minCustomAmount = defaultPlanOptions.values.toSeq.maxBy(_.instalmentAmount).instalmentAmount
          val maxCustomAmount = availablePaymentSchedules.maxBy(_.instalmentAmount).instalmentAmount

          val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

          selectPlanForm(minCustomAmount, maxCustomAmount).bindFromRequest().fold(
            invalidPlanSelectionFormBadRequest(Legacy, journey, planOptions, minCustomAmount, maxCustomAmount),
            validPlanSelectionFormRedirect(journey)
          )

        case CalculatorType.PaymentOptimised =>
          journeyLogger.info(s"Using payment optimised calculator")

          val defaultPlanOptions = paymentPlansService.defaultSchedules(
            sa,
            journey.safeUpfrontPayment,
            journey.maybePaymentDayOfMonth,
            journey.remainingIncomeAfterSpending
          )
          val minCustomAmount = defaultPlanOptions.values.toSeq
            .sortBy(_.instalmentAmount)
            .headOption.fold(BigDecimal(1))(_.instalmentAmount)
          val maxCustomAmount = paymentPlansService.maximumPossibleInstalmentAmount(journey)

          val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

          selectPlanForm(minCustomAmount, maxCustomAmount).bindFromRequest().fold(
            invalidPlanSelectionFormBadRequest(PaymentOptimised, journey, planOptions, minCustomAmount, maxCustomAmount),
            validPlanSelectionFormRedirect(journey)
          )
      }
    }
  }

  private def okPlanForm(
      calculatorType:     CalculatorType,
      minCustomAmount:    BigDecimal,
      maxCustomAmount:    BigDecimal,
      allPlanOptions:     Map[PaymentPlanOption, PaymentSchedule],
      maybePlanSelection: Option[PlanSelection]
  )(implicit request: Request[_]): Result = {
    Ok(views.how_much_can_you_pay_each_month_form(
      calculatorType,
      routes.CalculatorController.submitCalculateInstalments(),
      selectPlanForm(minCustomAmount, maxCustomAmount),
      allPlanOptions,
      minCustomAmount.setScale(2, HALF_UP),
      maxCustomAmount.setScale(2, HALF_UP),
      maybePlanSelection
    ))
  }

  private def invalidPlanSelectionFormBadRequest(
      calculatorType:  CalculatorType,
      journey:         Journey,
      planOptions:     Map[PaymentPlanOption, PaymentSchedule],
      minCustomAmount: BigDecimal,
      maxCustomAmount: BigDecimal
  )(implicit request: Request[_]): Form[PlanSelection] => Future[Result] = {
    formWithErrors =>
      {
        Future.successful(
          BadRequest(
            views.how_much_can_you_pay_each_month_form(
              calculatorType,
              ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
              formWithErrors,
              planOptions,
              minCustomAmount.setScale(2, HALF_UP),
              maxCustomAmount.setScale(2, HALF_UP),
              journey.maybePlanSelection))
        )
      }
  }

  private def validPlanSelectionFormRedirect(journey: Journey)(implicit request: Request[_]): PlanSelection => Future[Result] = {
    (validFormData: PlanSelection) =>
      {
        journeyService.saveJourney(journey.copy(maybePlanSelection = Some(validFormData.mongoSafe))).map { _ =>
          validFormData.selection match {
            case Right(CustomPlanRequest(_)) => Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments())
            case Left(SelectedPlan(_))       => Redirect(ssttparrangement.routes.ArrangementController.getCheckPaymentPlan())
          }
        }
      }
  }

  private def allPlanOptions(
      defaultPlanOptions: Map[PaymentPlanOption, PaymentSchedule],
      journey:            Journey
  )(implicit request: Request[_]): Map[PaymentPlanOption, PaymentSchedule] = {
    maybePreviousCustomAmount(journey, defaultPlanOptions) match {
      case None                 => defaultPlanOptions
      case Some(customSchedule) => Map((PaymentPlanOption.Custom, customSchedule)) ++ defaultPlanOptions
    }
  }

  private def maybePreviousCustomAmount(
      journey:            Journey,
      defaultPlanOptions: Map[PaymentPlanOption, PaymentSchedule]
  )(implicit request: Request[_]): Option[PaymentSchedule] = {
    journey.maybePlanSelection.foldLeft(None: Option[PaymentSchedule])((_, planSelection) => {
      planSelection.selection match {
        case Right(CustomPlanRequest(customAmount)) =>
          closestSchedule(customAmount, journey)
        case Left(SelectedPlan(amount)) if !isDefaultPlan(amount, defaultPlanOptions) =>
          closestSchedule(amount, journey)
        case _ => None
      }
    })
  }

  private def closestSchedule(
      amount:  BigDecimal,
      journey: Journey
  )(implicit request: Request[_]): Option[PaymentSchedule] = {
    appConfig.calculatorType match {
      case Legacy =>
        calculatorService.closestSchedule(
          amount,
          calculatorService.allAvailableSchedules(
            journey.taxpayer.selfAssessment,
            journey.safeUpfrontPayment,
            journey.maybePaymentDayOfMonth
          )
        )
      case PaymentOptimised =>
        paymentPlansService.customSchedule(
          journey.taxpayer.selfAssessment,
          journey.safeUpfrontPayment,
          journey.maybePaymentDayOfMonth,
          amount
        )
    }
  }

  private def isDefaultPlan(planAmount: BigDecimal, defaultPlanOptions: Map[PaymentPlanOption, PaymentSchedule]): Boolean = {
    defaultPlanOptions.map(_._2.instalments.headOption.fold(BigDecimal(0))(_.amount)).toList.contains(planAmount)
  }
}
