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
import ssttpcalculator.CalculatorForm.{createPaymentTodayForm, customAmountInputMapping, payTodayForm, selectPlanForm}
import model.{AddWorkingDaysResult, PaymentPlanOption, PaymentSchedule}
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views
import bankholidays.DateCalculatorService
import play.api.data.Form
import times.ClockProvider
import util.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CalculatorController @Inject() (
    mcc:                   MessagesControllerComponents,
    calculatorService:     CalculatorService,
    as:                    Actions,
    journeyService:        JourneyService,
    requestSupport:        RequestSupport,
    views:                 Views,
    clockProvider:         ClockProvider,
    interestRateService:   InterestRateService,
    dateCalculatorService: DateCalculatorService
)(implicit appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendBaseController(mcc) with Logging {

  import requestSupport._

  val getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Tax liabilities'")

      Ok(views.tax_liabilities(journey.debits, isSignedIn))
    }
  }

  val submitTaxLiabilities: Action[AnyContent] = as.authorisedSaUser { _ =>
    Redirect(ssttpcalculator.routes.CalculatorController.getPayTodayQuestion)
  }

  val getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
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
  val submitPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Submit 'Pay today question'")

      payTodayForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.payment_today_question(formWithErrors, isSignedIn))), {
          case PayTodayQuestion(Some(true)) =>
            val newJourney = journey.copy(
              maybePaymentToday = Some(PaymentToday(true))
            )
            journeyService.saveJourney(newJourney).map(_ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getPaymentToday))

          case PayTodayQuestion(Some(false)) =>
            val newJourney = journey.copy(
              maybePaymentToday       = Some(PaymentToday(false)),
              maybePaymentTodayAmount = None //resetting payment this amount just in case if user goes back and overrides this value
            )

            journeyService.saveJourney(newJourney).map(
              _ => Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay))
          case PayTodayQuestion(None) =>
            val msg = s"could not submitPayTodayQuestion, payToday must be defined"
            val ex = new RuntimeException(msg)
            journeyLogger.warn("Illegal state")
            throw ex
        }
      )
    }
  }

  val getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Payment Today'")

      val debits = journey.debits
      val emptyForm = createPaymentTodayForm(debits.map(_.amount).sum)
      val formWithData = journey.maybePaymentTodayAmount.map(paymentTodayAmount =>
        emptyForm.fill(CalculatorPaymentTodayForm(paymentTodayAmount.value))).getOrElse(emptyForm)
      Ok(views.payment_today_form(formWithData, debits, isSignedIn))
    }
  }

  val submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
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
            Redirect(ssttpcalculator.routes.CalculatorController.getPaymentSummary)
          }
        }
      )
    }
  }

  val getPaymentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Payment summary'")

      journey.maybePaymentToday match {
        case Some(PaymentToday(true)) =>
          val payToday = journey.paymentToday
          Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, payToday, journey.upfrontPayment))
        case Some(PaymentToday(false)) =>
          Redirect(ssttpcalculator.routes.CalculatorController.getPayTodayQuestion)
        case None =>
          journeyLogger.warn("Illegal state")
          throw new RuntimeException(s"payToday must be defined")
      }
    }
  }

  val submitPaymentSummary: Action[AnyContent] = as.authorisedSaUser { _ =>
    Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay)

  }

  val getCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Calculate instalments'")

      val sa = journey.taxpayer.selfAssessment

      journeyLogger.warn("Applicable interest rates: ", interestRateService.applicableInterestRates(sa))

      getDateFirstPaymentCanBeTaken(journey).map { dateFirstPaymentCanBeTaken =>
        val availablePaymentSchedules =
          calculatorService.allAvailableSchedules(sa, journey.safeUpfrontPayment, journey.maybePaymentDayOfMonth, dateFirstPaymentCanBeTaken)
        val closestSchedule =
          calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
        val defaultPlanOptions =
          calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

        defaultPlanOptions.values.toSeq.sortBy(_.instalmentAmount).headOption match {
          case None =>
            journeyLogger.info(s"No viable plans available: redirecting to 'We cannot agree your payment plan'")
            Redirect(ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP)

          case Some(_) =>
            val minCustomAmount = defaultPlanOptions.values.toSeq.maxBy(_.instalmentAmount).instalmentAmount
            val maxCustomAmount = availablePaymentSchedules.maxBy(_.instalmentAmount).instalmentAmount
            val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

            journeyLogger.info(s"Displaying plans: ${planOptions.keys}")
            okPlanForm(minCustomAmount, maxCustomAmount, planOptions, journey.maybePlanSelection)
        }
      }
    }
  }

  private def getDateFirstPaymentCanBeTaken(journey: Journey)(implicit request: Request[_]): Future[AddWorkingDaysResult] = {
    val today = clockProvider.nowDate()
    val numberOfWorkingDaysToAdd = appConfig.numberOfWorkingDaysToAdd

    journey.maybeDateFirstPaymentCanBeTaken.map(Future.successful).getOrElse(
      for {
        fiveWorkingDaysFromNow <- dateCalculatorService.addWorkingDays(today, numberOfWorkingDaysToAdd)
        addWorkingDaysResult = AddWorkingDaysResult(today, numberOfWorkingDaysToAdd, fiveWorkingDaysFromNow)
        _ <- journeyService.saveJourney(journey.copy(maybeDateFirstPaymentCanBeTaken = Some(addWorkingDaysResult)))
      } yield addWorkingDaysResult
    )
  }

  val submitCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Submit 'Calculate instalments'")

      val sa = journey.taxpayer.selfAssessment
      val availablePaymentSchedules = calculatorService.allAvailableSchedules(
        sa,
        journey.safeUpfrontPayment,
        journey.maybePaymentDayOfMonth,
        journey.dateFirstPaymentCanBeTaken
      )
      val closestSchedule = calculatorService.closestScheduleEqualOrLessThan(journey.remainingIncomeAfterSpending * 0.50, availablePaymentSchedules)
      val defaultPlanOptions = calculatorService.defaultSchedules(closestSchedule, availablePaymentSchedules)

      val minCustomAmount = defaultPlanOptions.values.toSeq.maxBy(_.instalmentAmount).instalmentAmount
      val maxCustomAmount = availablePaymentSchedules.maxBy(_.instalmentAmount).instalmentAmount

      val planOptions: Map[PaymentPlanOption, PaymentSchedule] = allPlanOptions(defaultPlanOptions, journey)

      selectPlanForm(minCustomAmount, maxCustomAmount).bindFromRequest().fold(
        invalidPlanSelectionFormBadRequest(journey, planOptions, minCustomAmount, maxCustomAmount),
        validPlanSelectionFormRedirect(journey)
      )
    }
  }

  private def okPlanForm(
      minCustomAmount:    BigDecimal,
      maxCustomAmount:    BigDecimal,
      allPlanOptions:     Map[PaymentPlanOption, PaymentSchedule],
      maybePlanSelection: Option[PlanSelection]
  )(implicit request: Request[_]): Result = {
    Ok(views.select_a_payment_plan_form(
      routes.CalculatorController.submitCalculateInstalments,
      selectPlanForm(minCustomAmount, maxCustomAmount),
      allPlanOptions,
      minCustomAmount.setScale(2, HALF_UP),
      maxCustomAmount.setScale(2, HALF_UP),
      maybePlanSelection
    ))
  }

  private def invalidPlanSelectionFormBadRequest(
      journey:         Journey,
      planOptions:     Map[PaymentPlanOption, PaymentSchedule],
      minCustomAmount: BigDecimal,
      maxCustomAmount: BigDecimal
  )(implicit request: Request[_]): Form[PlanSelectionRdBtnChoice] => Future[Result] = {
    formWithErrors =>
      {
        Future.successful(
          BadRequest(
            views.select_a_payment_plan_form(
              ssttpcalculator.routes.CalculatorController.submitCalculateInstalments,
              formWithErrors,
              planOptions,
              minCustomAmount.setScale(2, HALF_UP),
              maxCustomAmount.setScale(2, HALF_UP),
              journey.maybePlanSelection))
        )
      }
  }

  private def validPlanSelectionFormRedirect(journey: Journey)(implicit request: Request[_]): PlanSelectionRdBtnChoice => Future[Result] = {
    (validFormData: PlanSelectionRdBtnChoice) =>
      validFormData.selection match {
        case CannotAfford =>
          Redirect(ssttpaffordability.routes.AffordabilityController.getCannotAffordPlan)
        case PlanChoice(planSelection) => planSelection match {
          case Right(CustomPlanRequest(customAmount)) => {
            val planSelection = PlanSelection(Right(CustomPlanRequest(customAmount)))
            journeyService.saveJourney(journey.copy(maybePlanSelection = Some(planSelection.mongoSafe))).map { _ =>
              Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments)
            }
          }
          case Left(SelectedPlan(instalmentAmount)) => {
            val planSelection = PlanSelection(Left(SelectedPlan(instalmentAmount)))
            journeyService.saveJourney(journey.copy(maybePlanSelection = Some(planSelection.mongoSafe))).map { _ =>
              Redirect(ssttparrangement.routes.ArrangementController.getCheckPaymentPlan)
            }
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
      case Some(customSchedule) => (Map((PaymentPlanOption.Custom, customSchedule)) ++ defaultPlanOptions).toMap
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
    calculatorService.closestSchedule(
      amount,
      calculatorService.allAvailableSchedules(
        journey.taxpayer.selfAssessment,
        journey.safeUpfrontPayment,
        journey.maybePaymentDayOfMonth,
        journey.dateFirstPaymentCanBeTaken
      )
    )
  }

  private def isDefaultPlan(planAmount: BigDecimal, defaultPlanOptions: Map[PaymentPlanOption, PaymentSchedule]): Boolean = {
    defaultPlanOptions.map(_._2.instalments.headOption.fold(BigDecimal(0))(_.amount)).toList.contains(planAmount)
  }
}
