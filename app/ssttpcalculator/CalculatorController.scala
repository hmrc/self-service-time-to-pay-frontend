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

import audit.AuditService
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions

import javax.inject._
import journey.{Journey, JourneyService, PaymentToday, PaymentTodayAmount}
import play.api.mvc._
import req.RequestSupport
import ssttpcalculator.CalculatorForm.{createPaymentTodayForm, payTodayForm, selectPlanForm}
import ssttpcalculator.model.PaymentSchedule
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode.{CEILING, HALF_UP}

class CalculatorController @Inject() (
    mcc:                 MessagesControllerComponents,
    paymentPlansService: PaymentPlansService,
    instalmentsService:  InstalmentsService,
    as:                  Actions,
    journeyService:      JourneyService,
    requestSupport:      RequestSupport,
    views:               Views,
    clockProvider:       ClockProvider)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendBaseController(mcc) {

  import requestSupport._

  def getTaxLiabilities: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getTaxLiabilities: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      Ok(views.tax_liabilities(journey.debits, isSignedIn))
    }
  }

  def getPayTodayQuestion: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPayTodayQuestion: $request")

    journeyService.authorizedForSsttp { journey: Journey =>
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
    JourneyLogger.info(s"CalculatorController.submitPayTodayQuestion: $request")

    journeyService.authorizedForSsttp { journey: Journey =>
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
              maybePaymentTodayAmount = None //reseting payment this amount just in case if user goes back and overrides this value
            )

            journeyService.saveJourney(newJourney).map(
              _ => Redirect(ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay()))
          case PayTodayQuestion(None) =>
            val msg = s"could not submitPayTodayQuestion, payToday must be defined"
            val ex = new RuntimeException(msg)
            JourneyLogger.error("Illegal state", journey)
            throw ex
        }
      )
    }
  }

  def getPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentToday: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      val debits = journey.debits
      val emptyForm = createPaymentTodayForm(debits.map(_.amount).sum)
      val formWithData = journey.maybePaymentTodayAmount.map(paymentTodayAmount =>
        emptyForm.fill(CalculatorPaymentTodayForm(paymentTodayAmount.value))).getOrElse(emptyForm)
      Ok(views.payment_today_form(formWithData, debits, isSignedIn))
    }
  }

  def submitPaymentToday: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPaymentToday: $request")
    journeyService.authorizedForSsttp { journey: Journey =>

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
    JourneyLogger.info(s"CalculatorController.getPaymentSummary: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      journey.maybePaymentToday match {
        case Some(PaymentToday(true)) =>
          val payToday = journey.paymentToday
          Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, payToday, journey.upfrontPayment))
        case Some(PaymentToday(false)) =>
          Redirect(ssttpcalculator.routes.CalculatorController.getPayTodayQuestion())
        case None =>
          JourneyLogger.error("Illegal state", journey)
          throw new RuntimeException(s"payToday must be defined")
      }
    }
  }

  def getCalculateInstalments: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getCalculateInstalments: $request")

    journeyService.authorizedForSsttp { journey: Journey =>
      JourneyLogger.info("CalculatorController.getCalculateInstalments", journey)
      val sa = journey.taxpayer.selfAssessment
      val defaultPlanOptions = paymentPlansService.defaultSchedules(
        sa,
        journey.safeUpfrontPayment,
        journey.maybePaymentDayOfMonth,
        journey.remainingIncomeAfterSpending
      )

      val minCustomAmount = defaultPlanOptions.values
        .headOption.fold(BigDecimal(1))(_.firstInstalment.amount)
      val maxCustomAmount = paymentPlansService.maximumPossibleInstalmentAmount(journey)

      val allPlanOptions = maybePreviousCustomAmount(journey, defaultPlanOptions) match {
        case None                 => defaultPlanOptions
        case Some(customSchedule) => Map((0, customSchedule)) ++ defaultPlanOptions
      }

      if (defaultPlanOptions.isEmpty) {
        Redirect(ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP())
      } else {
        Ok(views.how_much_can_you_pay_each_month_form(
          routes.CalculatorController.submitCalculateInstalments(),
          selectPlanForm(minCustomAmount, maxCustomAmount),
          allPlanOptions,
          minCustomAmount.setScale(2, HALF_UP),
          maxCustomAmount.setScale(2, HALF_UP),
          journey.maybePlanSelection))
      }
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalments: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      JourneyLogger.info(s"CalculatorController.submitCalculateInstalments journey: $journey")
      val sa = journey.taxpayer.selfAssessment
      val paymentPlanOptions = paymentPlansService.defaultSchedules(
        sa,
        journey.safeUpfrontPayment,
        journey.maybePaymentDayOfMonth,
        journey.remainingIncomeAfterSpending
      )
      val minCustomAmount = paymentPlanOptions.values
        .headOption.fold(BigDecimal(1))(_.firstInstalment.amount)
      val maxCustomAmount = paymentPlansService.maximumPossibleInstalmentAmount(journey)

      selectPlanForm(minCustomAmount, maxCustomAmount).bindFromRequest().fold(
        formWithErrors => {
          Future.successful(
            BadRequest(
              views.how_much_can_you_pay_each_month_form(
                ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                formWithErrors,
                paymentPlanOptions,
                minCustomAmount.setScale(2, HALF_UP),
                maxCustomAmount.setScale(2, HALF_UP),
                journey.maybePlanSelection))
          )
        },
        (validFormData: PlanSelection) => {
          journeyService.saveJourney(journey.copy(maybePlanSelection = Some(validFormData.mongoSafe))).map { _ =>
            validFormData.selection match {
              case Right(CustomPlanRequest(_)) => Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments())
              case Left(SelectedPlan(_))       => Redirect(ssttparrangement.routes.ArrangementController.getCheckPaymentPlan())
            }
          }
        }

      )

    }
  }

  private def maybePreviousCustomAmount(
      journey:            Journey,
      defaultPlanOptions: Map[Int, PaymentSchedule]
  )(implicit request: Request[_]): Option[PaymentSchedule] = {
    journey.maybePlanSelection.foldLeft(None: Option[PaymentSchedule])((_, planSelection) => planSelection.selection match {
      case Right(CustomPlanRequest(customAmount)) =>
        paymentPlansService.customSchedule(
          journey.taxpayer.selfAssessment,
          journey.safeUpfrontPayment,
          journey.maybePaymentDayOfMonth,
          customAmount
        )
      case Left(SelectedPlan(amount)) if !isDefaultPlan(amount, defaultPlanOptions) =>
        paymentPlansService.customSchedule(
          journey.taxpayer.selfAssessment,
          journey.safeUpfrontPayment,
          journey.maybePaymentDayOfMonth,
          amount
        )
      case _ => None
    })
  }

  private def isDefaultPlan(planAmount: BigDecimal, defaultPlanOptions: Map[Int, PaymentSchedule]): Boolean = {
    defaultPlanOptions.map(_._2.instalments.headOption.fold(BigDecimal(0))(_.amount)).toList.contains(planAmount)
  }
}
