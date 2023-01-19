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

package ssttpaffordability

import audit.AuditService
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.{Actions, AuthorisedSaUserRequest}
import journey.{Journey, JourneyService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import req.RequestSupport
import ssttpaffordability.AffordabilityForm.{createIncomeForm, validateIncomeInputTotal}
import ssttpaffordability.model.{Income, IncomeCategory}
import ssttparrangement.ArrangementForm.dayOfMonthForm
import ssttparrangement.ArrangementForm
import ssttpcalculator.IncomeInput
import ssttpdirectdebit.DirectDebitConnector
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth
import views.Views

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AffordabilityController @Inject() (
    mcc:                  MessagesControllerComponents,
    auditService:         AuditService,
    journeyService:       JourneyService,
    as:                   Actions,
    requestSupport:       RequestSupport,
    views:                Views,
    directDebitConnector: DirectDebitConnector)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext) extends FrontendBaseController(mcc) {

  import requestSupport._

  //TODO This method is duplicated in ArrangementController as def submitChangeSchedulePaymentDay.
  // Delete from here and direct it to getCheckYouCanAfford, when the journey incorporates affordability
  def submitChooseDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AffordabilityController.submitChooseDay: $request")
    journeyService.authorizedForSsttp {
      journey =>
        dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.change_day(formWithErrors)))
          },
          (validFormData: ArrangementForm) => {
            JourneyLogger.info(s"changing schedule day to [${validFormData.dayOfMonth}]")
            val updatedJourney = journey.copy(maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(validFormData.dayOfMonth)))
            journeyService.saveJourney(updatedJourney).map {
              _ => Redirect(ssttpaffordability.routes.AffordabilityController.getCheckYouCanAfford())
            }
          }
        )
    }
  }

  def getCheckYouCanAfford: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AffordabilityController.getCheckYouCanAfford: $request")
    journeyService.authorizedForSsttp { journey =>
      val totalLiability = journey.debits.map(_.amount).sum
      val initialPayment = journey.maybePaymentTodayAmount.fold(BigDecimal(0))(_.value)
      Future.successful(Ok(views.check_you_can_afford(totalLiability, initialPayment)))
    }
  }

  def getAddIncomeAndSpending: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AffordabilityController.getAddIncomeAndSpending: $request")
    journeyService.authorizedForSsttp { _ =>
      Future.successful(Ok(views.add_income_spending()))
    }
  }

  def getYourMonthlyIncome: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AffordabilityController.getYourMonthlyIncome: $request")
    journeyService.authorizedForSsttp{ journey: Journey =>
      val emptyForm = createIncomeForm()
      val formWithData = journey.maybeIncome.map(income =>
        emptyForm.fill(IncomeInput(
          monthlyIncome = income.amount("monthlyIncome"),
          benefits      = income.amount("benefits"),
          otherIncome   = income.amount("otherIncome")
        ))
      ).getOrElse(emptyForm)
      Future.successful(Ok(views.your_monthly_income(formWithData, isSignedIn)))
    }
  }

  def submitMonthlyIncome: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AffordabilityController.submitMonthlyIncome: $request")

    journeyService.authorizedForSsttp { journey: Journey =>
      createIncomeForm().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.your_monthly_income(formWithErrors, isSignedIn)))
        },

        { (input: IncomeInput) =>
          val formValidatedForPositiveTotal = validateIncomeInputTotal(createIncomeForm().fill(input))
          if (formValidatedForPositiveTotal.hasErrors) {
            Future.successful(BadRequest(views.your_monthly_income(formValidatedForPositiveTotal, isSignedIn)))

          } else {
            storeIncomeInputToJourney(input, journey).map { _ =>
              Redirect(ssttpaffordability.routes.AffordabilityController.getAddIncomeAndSpending())
            }
          }
        }
      )
    }
  }

  private def storeIncomeInputToJourney(
      input:   IncomeInput,
      journey: Journey
  )(implicit request: AuthorisedSaUserRequest[AnyContent]) = {
    val newJourney = journey.copy(
      maybeIncome = Some(Income(Seq(
        IncomeCategory("monthlyIncome", input.monthlyIncome),
        IncomeCategory("benefits", input.benefits),
        IncomeCategory("otherIncome", input.otherIncome)
      )))
    )
    journeyService.saveJourney(newJourney)
  }

}
