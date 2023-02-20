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
import play.api.mvc.{AnyContent, _}
import req.RequestSupport
import ssttpcalculator.CalculatorForm.{createInstalmentForm, createPaymentTodayForm, payTodayForm}
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
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
    clockProvider:     ClockProvider)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendBaseController(mcc) {

  import clockProvider._
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
          Ok(views.payment_summary(journey.taxpayer.selfAssessment.debits, payToday, journey.initialPayment))
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
      val paymentPlanOptions = calculatorService.scheduleOptions(
        sa,
        journey.safeUpfrontPayment,
        journey.maybeArrangementDayOfMonth,
        journey.remainingIncomeAfterSpending
      )

      if (paymentPlanOptions.isEmpty) {
        Redirect(ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP())
      } else {
        Ok(views.calculate_instalments_form(
          routes.CalculatorController.submitCalculateInstalments(),
          createInstalmentForm(),
          paymentPlanOptions.values.toList
        ))
      }
    }
  }

  def submitCalculateInstalments(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitCalculateInstalments: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      JourneyLogger.info("CalculatorController.submitCalculateInstalments", journey)
      val sa = journey.taxpayer.selfAssessment
      val paymentPlanOptions = calculatorService.scheduleOptions(
        sa,
        journey.safeUpfrontPayment,
        journey.maybeArrangementDayOfMonth,
        journey.remainingIncomeAfterSpending
      )

      createInstalmentForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(
            BadRequest(
              views.calculate_instalments_form(
                ssttpcalculator.routes.CalculatorController.submitCalculateInstalments(),
                formWithErrors,
                paymentPlanOptions.values.toList
              ))
          )
        },
        (validFormData: PlanRegularAmountSelection) => {
          JourneyLogger.info(s"$this.submitCalculateInstalments - valid form data - $validFormData")
          journeyService.saveJourney(journey.copy(maybeRegularPlanAmountSelection = Some(validFormData))).map { _ =>
            Redirect(ssttparrangement.routes.ArrangementController.getInstalmentSummary())
          }
        }

      )

    }
  }
}
