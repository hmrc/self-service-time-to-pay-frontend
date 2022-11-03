/*
 * Copyright 2022 HM Revenue & Customs
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
import bars.model.{InvalidBankDetails, ValidBankDetails}
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions

import javax.inject._
import journey.{Journey, JourneyService}
import model.enumsforforms.{IsSoleSignatory, TypeOfBankAccount}
import model.forms.TypeOfAccountForm
import play.api.libs.json.Json
import play.api.mvc._
import req.RequestSupport
import ssttpcalculator.CalculatorService
import ssttpdirectdebit.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDirectDebit, BankDetails, TypeOfAccountDetails}
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitController @Inject() (
    mcc:               MessagesControllerComponents,
    barsService:       BarsService,
    actions:           Actions,
    submissionService: JourneyService,
    requestSupport:    RequestSupport,
    calculatorService: CalculatorService,
    views:             Views)(implicit appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendBaseController(mcc) {

  import requestSupport._

  def aboutBankAccount: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"$request")
    submissionService.authorizedForSsttp { journey: Journey =>
      // TODO journey.bankAccountDetails
      Ok(views.about_bank_account(TypeOfAccountForm.form))
    }
  }

  def submitAboutBankAccount: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { journey =>
      TypeOfAccountForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              Ok(views.about_bank_account(formWithErrors))
            ),
          (detailsAboutBankAccountForm: TypeOfAccountForm) => {
            submissionService.saveJourney(
              journey.copy(maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(
                detailsAboutBankAccountForm.typeOfAccount,
                detailsAboutBankAccountForm.isSoleSignatory.asBoolean)))
            )
              .map { _ =>
                val redirectTo = detailsAboutBankAccountForm.isSoleSignatory match {
                  case IsSoleSignatory.Yes => ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()
                  case IsSoleSignatory.No  => ssttpdirectdebit.routes.DirectDebitController.aboutBankAccount() //TODO
                }
                Redirect(redirectTo)
              }
          }
        )
    }
  }

  //  def aboutBankAccountSubmit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
  //    TypeOfAccountForm.form
  //      .bindFromRequest()
  //      .fold(
  //        formWithErrors =>
  //          Future.successful(
  //            Ok(views.about_bank_account(formWithErrors))
  //          ),
  //        (detailsAboutBankAccountForm: TypeOfAccountForm) =>
  //          submissionService
  //            .updateDetailsAboutBankAccount(
  //              journeyId = request.journeyId,
  //              aboutBankAccount = TypeOfBankAccount(
  //                TypesOfBankAccount.typeOfBankAccountFromFormValue(detailsAboutBankAccountForm.typeOfAccount),
  //                detailsAboutBankAccountForm.isSoleSignatory.asBoolean
  //              )
  //            ).map { _ =>
  //            val redirectTo = detailsAboutBankAccountForm.isSoleSignatory match {
  //              case IsSoleSignatoryFormValue.No =>
  //                routes.BankDetailsController.cannotSetupDirectDebitOnlinePage
  //
  //              case IsSoleSignatoryFormValue.Yes =>
  //                routes.BankDetailsController.enterBankDetails
  //            }
  //            Redirect(redirectTo)
  //          }
  //      )
  //  }

  def getDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebit: $request")

    submissionService.authorizedForSsttp { journey =>
      journey.requireScheduleIsDefined()
      val schedule = calculatorService.computeSchedule(journey)
      Future.successful(Ok(views.direct_debit_form(journey.taxpayer.selfAssessment.debits, schedule, directDebitForm, isSignedIn)))
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitAssistance: $request")

    submissionService.authorizedForSsttp { journey: Journey =>
      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      val schedule = calculatorService.computeSchedule(journey)
      Future.successful(Ok(views.direct_debit_assistance(journey.taxpayer.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()), schedule, isSignedIn)))
    }
  }

  def getDirectDebitError: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { journey: Journey =>
      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      val schedule = calculatorService.computeSchedule(journey)
      Future.successful(
        Ok(views.direct_debit_assistance(
          journey.taxpayer.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()), schedule, loggedIn = true, showErrorNotification = isSignedIn)))
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitConfirmation: $request")

    submissionService.authorizedForSsttp { journey: Journey =>
      journey.requireScheduleIsDefined()
      journey.requireDdIsDefined()
      val schedule = calculatorService.computeSchedule(journey)
      val directDebit = journey.arrangementDirectDebit.getOrElse(throw new RuntimeException(s"arrangement direct debit not found on submission [${journey}]"))
      Future.successful(Ok(views.direct_debit_confirmation(
        journey.taxpayer.selfAssessment.debits, schedule, directDebit, isSignedIn))
      )
    }
  }

  def submitDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.submitDirectDebit: $request")

    submissionService.authorizedForSsttp { journey =>
      journey.requireScheduleIsDefined()
      val schedule = calculatorService.computeSchedule(journey)
      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          views.direct_debit_form(
            journey.taxpayer.selfAssessment.debits,
            schedule,
            formWithErrors))),
        (validFormData: ArrangementDirectDebit) => {
          barsService.validateBankDetails(validFormData.sortCode, validFormData.accountNumber).flatMap {

            case ValidBankDetails(obfuscatedBarsResponse) =>
              JourneyLogger.info(s"Bank details are valid, response from BARS: ${Json.prettyPrint(Json.toJson(obfuscatedBarsResponse))}", journey)
              submissionService.saveJourney(
                journey.copy(maybeBankDetails = Some(BankDetails(validFormData.sortCode, validFormData.accountNumber, validFormData.accountName)))
              ).map { _ =>
                  Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation())
                }
            case InvalidBankDetails(obfuscatedBarsResponse) =>
              JourneyLogger.info(s"Bank details are invalid, response from BARS: ${Json.prettyPrint(Json.toJson(obfuscatedBarsResponse))}", journey)
              Future.successful(BadRequest(views.direct_debit_form(
                journey.taxpayer.selfAssessment.debits,
                schedule,
                directDebitFormWithBankAccountError.copy(data = Map(
                  "accountName" -> validFormData.accountName,
                  "accountNumber" -> validFormData.accountNumber,
                  "sortCode" -> validFormData.sortCode)
                ),
                isBankError = true)))
          }
        }
      )
    }
  }
}
