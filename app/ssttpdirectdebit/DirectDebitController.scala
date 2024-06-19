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

package ssttpdirectdebit

import bars.BarsService
import bars.model.{BarsResponseOk, InvalidBankDetails, ValidBankDetails}
import config.{AppConfig, ViewConfig}
import controllers.FrontendBaseController
import controllers.action.Actions
import journey.{Journey, JourneyService}
import model.enumsforforms.IsSoleSignatory
import model.enumsforforms.IsSoleSignatory.booleanToIsSoleSignatory
import model.enumsforforms.TypesOfBankAccount.typeOfBankAccountAsFormValue
import model.forms.TypeOfAccountForm
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._
import req.RequestSupport
import ssttpcalculator.CalculatorService
import ssttpdirectdebit.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDirectDebit, BankDetails, TypeOfAccountDetails}
import util.{Logging, SelectedScheduleHelper}
import views.Views

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DirectDebitController @Inject() (
    mcc:                   MessagesControllerComponents,
    barsService:           BarsService,
    viewConfig:            ViewConfig,
    actions:               Actions,
    submissionService:     JourneyService,
    requestSupport:        RequestSupport,
    val calculatorService: CalculatorService,
    views:                 Views
)(
    implicit
    val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendBaseController(mcc)
  with SelectedScheduleHelper
  with Logging {

  import requestSupport._

  implicit val config: ViewConfig = viewConfig

  val getAboutBankAccount: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'About bank account'")

      journey.requireScheduleIsDefined()
      val form = journey.maybeTypeOfAccountDetails match {
        case Some(value) =>
          TypeOfAccountForm.form.fill(
            TypeOfAccountForm(typeOfBankAccountAsFormValue(value.typeOfAccount),
                              booleanToIsSoleSignatory(value.isAccountHolder)))
        case None => TypeOfAccountForm.form
      }
      Ok(views.about_bank_account(form))
    }
  }

  val submitAboutBankAccount: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Submit 'About bank account'")

      journey.requireScheduleIsDefined()
      TypeOfAccountForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            BadRequest(views.about_bank_account(formWithErrors))
          },
          (detailsAboutBankAccountForm: TypeOfAccountForm) => {
            submissionService.saveJourney(
              journey.copy(maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(
                detailsAboutBankAccountForm.typeOfAccount,
                detailsAboutBankAccountForm.isSoleSignatory.asBoolean)))
            )
              .map { _ =>
                val redirectTo = detailsAboutBankAccountForm.isSoleSignatory match {
                  case IsSoleSignatory.Yes => ssttpdirectdebit.routes.DirectDebitController.getDirectDebit
                  case IsSoleSignatory.No  => ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSoleSignatory
                }
                Redirect(redirectTo)
              }
          }
        )
    }
  }

  val getDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'Direct debit'")

      journey.requireScheduleIsDefined()
      val typeOfAccInJourney = journey.maybeTypeOfAccountDetails.map(_.typeOfAccount)
      val typeOfAccInBankDetails = journey.maybeBankDetails.flatMap(_.typeOfAccount)
      val formData = journey.arrangementDirectDebit match {
        case Some(value) =>
          if (typeOfAccInJourney == typeOfAccInBankDetails)
            directDebitForm.fill(value)
          else directDebitForm
        case None => directDebitForm
      }
      Future.successful(Ok(views.direct_debit_form(formData)))
    }
  }

  val getDirectDebitAssistance: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Direct debit assistance'")

      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      Future.successful(Ok(views.direct_debit_assistance()))
    }
  }

  val getDirectDebitError: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Direct debit error'")

      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      Future.successful(
        Ok(views.direct_debit_assistance()))
    }
  }

  val getDirectDebitConfirmation: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey: Journey =>
      journeyLogger.info("Get 'Direct debit confirmation'")

      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      val directDebit = journey.arrangementDirectDebit.getOrElse(
        throw new RuntimeException(s"arrangement direct debit not found on submission for journey ID [${journey._id}]")
      )
      Future.successful(Ok(views.direct_debit_confirmation(directDebit))
      )
    }
  }

  val submitDirectDebitConfirmation: Action[AnyContent] = actions.authorisedSaUser { _ =>
    Redirect(ssttparrangement.routes.ArrangementController.getTermsAndConditions)
  }

  val submitDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Submit 'Direct debit'")

      journey.requireScheduleIsDefined()
      journey.requireIsAccountHolder()

      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(views.direct_debit_form(formWithErrors))),
        (validFormData: ArrangementDirectDebit) =>
          if (ArrangementDirectDebit.to(validFormData) == journey.maybeBankDetails) {
            Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation)
          } else {
            barsService.validateBankDetails(validFormData.sortCode, validFormData.accountNumber, validFormData.accountName, journey.maybeTypeOfAccountDetails, request.utr).flatMap {
              case ValidBankDetails(obfuscatedBarsResponse) =>
                journeyLogger.info(s"Bank details are valid, response from BARS: ${Json.prettyPrint(Json.toJson(obfuscatedBarsResponse))}")
                submissionService.saveJourney(
                  journey.copy(maybeBankDetails = Some(BankDetails(journey.maybeTypeOfAccountDetails.map(_.typeOfAccount),
                                                                   validFormData.sortCode, validFormData.accountNumber, validFormData.accountName)))
                ).map { _ =>
                    Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation)
                  }
              case InvalidBankDetails(obfuscatedBarsResponse) =>
                journeyLogger.info(s"Bank details are invalid, response from BARS: ${Json.prettyPrint(Json.toJson(obfuscatedBarsResponse))}")
                obfuscatedBarsResponse match {
                  case BarsResponseOk(validateBankDetailsResponse) if !validateBankDetailsResponse.supportsDirectDebit =>
                    futureSuccessfulBadRequest(validFormData, directDebitFormWithSortCodeError)
                  case _ =>
                    futureSuccessfulBadRequest(validFormData, directDebitFormWithAccountComboError)
                }
            }
          }
      )
    }
  }

  private def futureSuccessfulBadRequest(
      validFormData: ArrangementDirectDebit,
      formWithError: Form[ArrangementDirectDebit]
  )(implicit request: Request[_]): Future[Result] =
    Future.successful(BadRequest(views.direct_debit_form(
      formWithError.copy(data = Map(
        "accountName" -> validFormData.accountName,
        "accountNumber" -> validFormData.accountNumber,
        "sortCode" -> validFormData.sortCode)
      ))))
}
