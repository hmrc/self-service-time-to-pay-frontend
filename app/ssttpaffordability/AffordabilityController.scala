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
import com.google.inject.Singleton
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import journey.{Journey, JourneyService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import req.RequestSupport
import ssttpaffordability.AffordabilityForm.{incomeForm, spendingForm}
import ssttpaffordability.model.Expense._
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model._
import util.Logging
import views.Views

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AffordabilityController @Inject() (
    mcc:            MessagesControllerComponents,
    auditService:   AuditService,
    journeyService: JourneyService,
    as:             Actions,
    requestSupport: RequestSupport,
    views:          Views)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext) extends FrontendBaseController(mcc) with Logging {

  import requestSupport._

  val getCheckYouCanAfford: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'Check you can afford'")

      val totalLiability = journey.debits.map(_.amount).sum
      val initialPayment = journey.maybePaymentTodayAmount.fold(BigDecimal(0))(_.value)
      Future.successful(Ok(views.check_you_can_afford(totalLiability, initialPayment)))
    }
  }

  val submitCheckYouCanAfford: Action[AnyContent] = as.authorisedSaUser { _ =>
    Redirect(ssttpaffordability.routes.AffordabilityController.getAddIncomeAndSpending)
  }

  val getAddIncomeAndSpending: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'Add income and spending'")

      val spending = journey.maybeSpending.fold(Seq.empty[Expenses])(_.expenses)
      val income = journey.maybeIncome.fold(Seq.empty[IncomeBudgetLine])(_.budgetLines)
      if (spending.nonEmpty && income.nonEmpty) {
        Redirect(ssttpaffordability.routes.AffordabilityController.getHowMuchYouCouldAfford)
      } else {
        Future.successful(Ok(views.add_income_spending(income, spending)))
      }
    }
  }

  val getHowMuchYouCouldAfford: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'How much you could afford'")

      val spending = journey.maybeSpending.fold(Seq.empty[Expenses])(_.expenses)
      val income = journey.maybeIncome.fold(Seq.empty[IncomeBudgetLine])(_.budgetLines)
      val remainingIncomeAfterSpending = journey.remainingIncomeAfterSpending
      Future.successful(Ok(views.how_much_you_could_afford(income, spending, remainingIncomeAfterSpending)))
    }
  }

  val submitHowMuchYouCouldAfford: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Submnit 'How much you could afford'")
      val remainingIncomeAfterSpending = journey.remainingIncomeAfterSpending

      val redirectTo = if (remainingIncomeAfterSpending <= 0)
        ssttpaffordability.routes.AffordabilityController.getWeCannotAgreeYourPP
      else
        ssttpcalculator.routes.CalculatorController.getCalculateInstalments

      Future.successful(Redirect(redirectTo))
    }
  }

  val getYourMonthlyIncome: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp{ implicit journey: Journey =>
      journeyLogger.info("Get 'your monthly income'")

      val emptyForm = incomeForm
      val formWithData = journey.maybeIncome.map(income =>
        emptyForm.fill(IncomeInput(
          monthlyIncome = income.amount(MonthlyIncome),
          benefits      = income.amount(Benefits),
          otherIncome   = income.amount(OtherIncome)
        ))
      ).getOrElse(emptyForm)
      Future.successful(Ok(views.your_monthly_income(formWithData)))
    }
  }

  val submitMonthlyIncome: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info(s"Submit 'Your monthly income'")

      incomeForm.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.your_monthly_income(formWithErrors)))
        },
        { (input: IncomeInput) =>
          if (input.hasPositiveTotal) {
            storeIncomeInputToJourney(input, journey).map { _ =>
              Redirect(ssttpaffordability.routes.AffordabilityController.getAddIncomeAndSpending)
            }
          } else {
            storeIncomeInputToJourney(IncomeInput.empty, journey).map { _ =>
              Redirect(ssttpaffordability.routes.AffordabilityController.getCallUsNoIncome)
            }
          }
        }
      )
    }
  }

  val getCallUsNoIncome: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info(s"Get 'Call us no income'")

      Ok(views.call_us_no_income())
    }
  }

  private def storeIncomeInputToJourney(
      input:   IncomeInput,
      journey: Journey
  ) = {
    val newJourney = journey.copy(
      maybeIncome        = Some(Income(
        IncomeBudgetLine(MonthlyIncome, input.monthlyIncome),
        IncomeBudgetLine(Benefits, input.benefits),
        IncomeBudgetLine(OtherIncome, input.otherIncome)
      )),
      maybePlanSelection = None
    )
    journeyService.saveJourney(newJourney)
  }

  val getYourMonthlySpending: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info(s"Get 'Your monthly spending'")

      val formWithData = journey.maybeSpending.map(expense =>
        spendingForm.fill(SpendingInput(
          housing              = expense.amount(HousingExp),
          pensionContributions = expense.amount(PensionContributionsExp),
          councilTax           = expense.amount(CouncilTaxExp),
          utilities            = expense.amount(UtilitiesExp),
          debtRepayments       = expense.amount(DebtRepaymentsExp),
          travel               = expense.amount(TravelExp),
          childcare            = expense.amount(ChildcareExp),
          insurance            = expense.amount(InsuranceExp),
          groceries            = expense.amount(GroceriesExp),
          health               = expense.amount(HealthExp),
        ))
      ).getOrElse(spendingForm)
      Future.successful(Ok(views.your_monthly_spending(formWithData)))
    }
  }

  val submitMonthlySpending: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info(s"Submit 'Your monthly spending'")

      spendingForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.your_monthly_spending(formWithErrors))),
        (form: SpendingInput) => {
          val newJourney = journey.copy(
            maybeSpending      = Some(Spending(
              Expenses(HousingExp, form.housing),
              Expenses(PensionContributionsExp, form.pensionContributions),
              Expenses(CouncilTaxExp, form.councilTax),
              Expenses(UtilitiesExp, form.utilities),
              Expenses(DebtRepaymentsExp, form.debtRepayments),
              Expenses(TravelExp, form.travel),
              Expenses(ChildcareExp, form.childcare),
              Expenses(InsuranceExp, form.insurance),
              Expenses(GroceriesExp, form.groceries),
              Expenses(HealthExp, form.health)
            )),
            maybePlanSelection = None)
          journeyService.saveJourney(newJourney).map { _ =>
            Redirect(ssttpaffordability.routes.AffordabilityController.getAddIncomeAndSpending)
          }
        }
      )
    }
  }
  val getWeCannotAgreeYourPP: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'We cannot agree your payment plan'")

      auditService.sendPlanNotAffordableEvent(journey)
      Ok(views.we_cannot_agree_your_pp())
    }
  }

  val getSetUpPlanWithAdviser: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Set up a payment plan with an adviser'")

      auditService.sendPlanFailsNDDSValidationEvent(journey)
      Ok(views.set_up_a_payment_plan_with_an_adviser(isWelsh))
    }
  }

  val getCannotAffordPlan: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'We cannot agree your payment plan'")

      Ok(views.call_us_about_a_payment_plan(cannotAfford = true))
    }
  }
}
