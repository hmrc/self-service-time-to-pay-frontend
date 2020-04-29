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

package ssttpdirectdebit

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject._
import journey.Statuses.InProgress
import journey.{Journey, JourneyService}
import play.api.Logger
import play.api.mvc._
import req.RequestSupport
import ssttpdirectdebit.DirectDebitForm._
import times.ClockProvider
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.play.config.AssetsConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitController @Inject() (
    mcc:                  MessagesControllerComponents,
    directDebitConnector: DirectDebitConnector,
    as:                   Actions,
    submissionService:    JourneyService,
    requestSupport:       RequestSupport,
    views:                Views,
    clockProvider:        ClockProvider)(
    implicit
    appConfig:    AppConfig,
    ec:           ExecutionContext,
    assetsConfig: AssetsConfig
) extends FrontendBaseController(mcc) {

  import requestSupport._

  def getDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebit: $request")

    submissionService.authorizedForSsttp {
      case journey @ Journey(_, InProgress, _, _, Some(schedule), _, _, Some(_), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_form(journey.taxpayer.selfAssessment.debits, schedule, directDebitForm, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebit: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitAssistance: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitAssistance: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitError: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(
          Ok(views.direct_debit_assistance(
            sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, loggedIn = true, showErrorNotification = isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitError - redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitConfirmation: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, _, _, Some(_), _, _, _, _, _, _) =>
        Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
      case submission @ Journey(_, InProgress, _, _, Some(schedule), Some(_), _, _, _, _, _, _, _) =>
        val directDebit =
          submission.arrangementDirectDebit.getOrElse(
            throw new RuntimeException(s"arrangement direct debit not found on submission [$submission]"))

        Future.successful(Ok(views.direct_debit_confirmation(
          submission.taxpayer.selfAssessment.debits, schedule, directDebit, isSignedIn)))
      case journey =>
        Logger.error(s"Bank details missing from cache on Direct Debit Confirmation page")
        JourneyLogger.info("DirectDebitController.getDirectDebitConfirmation - redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  //TODO: probably not used
  def getDirectDebitUnAuthorised: Action[AnyContent] = as.action.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitUnAuthorised: $request")

    submissionService.getJourney.map {
      case _: Journey => Ok(views.direct_debit_unauthorised(isSignedIn))
      case _ =>
        JourneyLogger.info("DirectDebitController.getDirectDebitUnAuthorised - no TTPSubmission")
        Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start())
    }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = as.action { implicit request =>
    JourneyLogger.info(s"DirectDebitController.submitDirectDebitConfirmation: $request")
    Redirect(ssttparrangement.routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.submitDirectDebit: $request")
    submissionService.authorizedForSsttp { submission: Journey =>

      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          views.direct_debit_form(
            submission.taxpayer.selfAssessment.debits,
            submission.schedule,
            formWithErrors))),
        validFormData => {
          directDebitConnector.validateBank(validFormData.sortCode, validFormData.accountNumber).flatMap { isValid =>
            if (isValid)
              checkBankDetails(validFormData.sortCode, validFormData.accountNumber, validFormData.accountName)
            else
              Future.successful(BadRequest(views.direct_debit_form(
                submission.taxpayer.selfAssessment.debits,
                submission.schedule,
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

  /**
   * Using saUtr, gets a list of banks associated with the user. Checks the user entered
   * bank details to see if they already exist. If it does, return existing bank details
   * otherwise return user entered bank details.
   */
  private[ssttpdirectdebit] def checkBankDetails(sortCode: String, accountNumber: String, accountName: String)
    (implicit request: Request[_]) = {

    submissionService.getJourney.flatMap { journey =>
      val taxpayer = journey.maybeTaxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
      val utr = taxpayer.selfAssessment.utr

      directDebitConnector.getBanks(utr).flatMap { directDebitBank =>
        val bankDetails = DirectDebitUtils.bankDetails(sortCode, accountNumber, accountName, directDebitBank)

        submissionService.saveJourney(journey.copy(maybeBankDetails = Some(bankDetails))).map {
          _ => Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation())
        }
      }
    }
  }
}
