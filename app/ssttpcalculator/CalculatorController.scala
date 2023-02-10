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

import java.time.LocalDate

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject._
import journey.Statuses.InProgress
import journey.{Journey, JourneyService, PaymentToday, PaymentTodayAmount}
import model.{TaxPaymentPlan, PaymentSchedule}
import play.api.mvc.{AnyContent, _}
import req.RequestSupport
import ssttpcalculator.CalculatorForm.{createInstalmentForm, createMonthlyAmountForm, createPaymentTodayForm, payTodayForm}
import times.ClockProvider
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views
import _root_.model._

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

  //  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  //  private def monthlyPaymentBound(sa:                         SelfAssessmentDetails,
  //                                  initialPayment:             BigDecimal,
  //                                  maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth])(implicit request: Request[_]): (BigDecimal, BigDecimal) =
  //    Try {
  //      val schedules = calculatorService.paymentPlanOptions(sa, initialPayment, maybeArrangementDayOfMonth)
  //      val lowestPossibleMonthlyAmount = schedules.head.firstInstallment.amount
  //      val largestPossibleMonthlyAmount = schedules.last.firstInstallment.amount
  //      (BigDecimalUtil.roundUpToNearestTen(largestPossibleMonthlyAmount), BigDecimalUtil.roundUpToNearestTen(lowestPossibleMonthlyAmount))
  //    } match {
  //      case Success(s) =>
  //        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: [$s]")
  //        s
  //      case Failure(e) =>
  //        JourneyLogger.info(s"CalculatorController.lowerMonthlyPaymentBound: ERROR [${e.toString}]")
  //        throw e
  //    }

  //  def computeClosestSchedule(amount: BigDecimal, schedules: Seq[PaymentSchedule])(implicit hc: HeaderCarrier): PaymentSchedule = {
  //      def difference(schedule: PaymentSchedule) = math.abs(schedule.getMonthlyInstalment.toInt - amount.toInt)
  //
  //      def closest(min: PaymentSchedule, next: PaymentSchedule) = if (difference(next) < difference(min)) next else min
  //
  //    Try(schedules.reduceOption(closest).getOrElse(throw new RuntimeException(s"No schedules for [$schedules]"))) match {
  //      case Success(s) => s
  //      case Failure(e) =>
  //        JourneyLogger.info(s"CalculatorController.closestSchedule: ERROR [$e]")
  //        throw e
  //    }
  //  }

  //  def computeClosestSchedules(closestSchedule: PaymentSchedule, schedules: List[PaymentSchedule], sa: SelfAssessmentDetails)
  //    (implicit request: Request[_]): List[PaymentSchedule] = {
  //    val closestScheduleIndex = schedules.indexOf(closestSchedule)
  //
  //      def scheduleMonthsLater(n: Int): Option[PaymentSchedule] = closestScheduleIndex match {
  //        case -1                           => None
  //        case i if i + n >= schedules.size => None
  //        case m                            => Some(schedules(m + n))
  //      }
  //
  //      def scheduleMonthsBefore(n: Int): Option[PaymentSchedule] = closestScheduleIndex match {
  //        case -1             => None
  //        case i if i - n < 0 => None
  //        case m              => Some(schedules(m - n))
  //      }
  //
  //    if (closestScheduleIndex == 0)
  //      List(Some(closestSchedule), scheduleMonthsLater(1), scheduleMonthsLater(2))
  //    else if (closestScheduleIndex == schedules.size - 1)
  //      List(scheduleMonthsBefore(2), scheduleMonthsBefore(1), Some(closestSchedule))
  //    else
  //      List(scheduleMonthsBefore(1), Some(closestSchedule), scheduleMonthsLater(1))
  //  }.flatten

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

  def getPaymentPlanOptions: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.getPaymentPlanOptions: $request")

    journeyService.authorizedForSsttp { journey: Journey =>
      JourneyLogger.info("CalculatorController.getPaymentPlanOptions", journey)
      val sa = journey.taxpayer.selfAssessment
      val paymentPlanOptions = calculatorService.paymentPlanOptions(
        sa,
        journey.safeInitialPayment,
        journey.maybeArrangementDayOfMonth,
        journey.regularPaymentAmount,
        journey.maybePaymentToday
      )

      Ok(views.calculate_instalments_form(
        routes.CalculatorController.submitPaymentPlanOptions(),
        createInstalmentForm(),
        paymentPlanOptions
      ))

    }
  }

  def submitPaymentPlanOptions(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"CalculatorController.submitPaymentPlanOptions: $request")
    journeyService.authorizedForSsttp { journey: Journey =>
      JourneyLogger.info("CalculatorController.submitPaymentPlanOptions", journey)
      val sa = journey.taxpayer.selfAssessment
      val paymentPlanOptions = calculatorService.paymentPlanOptions(
        sa,
        journey.safeInitialPayment,
        journey.maybeArrangementDayOfMonth,
        journey.regularPaymentAmount,
        journey.maybePaymentToday
      )

      createInstalmentForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(
            BadRequest(
              views.calculate_instalments_form(
                ssttpcalculator.routes.CalculatorController.submitPaymentPlanOptions(),
                formWithErrors,
                paymentPlanOptions))
          )
        },
        (validFormData: CalculatorDuration) =>
          journeyService.saveJourney(journey.copy(maybeCalculatorDuration = Some(validFormData))).map { _ =>
            Redirect(ssttparrangement.routes.ArrangementController.getInstalmentSummary())
          }
      )

    }
  }
}
