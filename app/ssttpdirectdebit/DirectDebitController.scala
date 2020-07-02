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
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.play.config.AssetsConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.BankDetails
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitController @Inject() (
    mcc:                  MessagesControllerComponents,
    directDebitConnector: DirectDebitConnector,
    actions:              Actions,
    submissionService:    JourneyService,
    requestSupport:       RequestSupport,
    views:                Views)(implicit appConfig: AppConfig, ec: ExecutionContext, assetsConfig: AssetsConfig)
  extends FrontendBaseController(mcc) {

  import requestSupport._

  def getDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebit: $request")

    submissionService.authorizedForSsttp {
      case journey @ Journey(_, InProgress, _, _, Some(schedule), _, _, Some(_), _, _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_form(journey.taxpayer.selfAssessment.debits, schedule, directDebitForm, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebit: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitAssistance: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitAssistance: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitError: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _, _) =>
        Future.successful(
          Ok(views.direct_debit_assistance(
            sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, loggedIn = true, showErrorNotification = isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitError - redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitConfirmation: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, InProgress, _, _, _, _, Some(_), _, _, _, _, _, _, _) =>
        Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
      case submission @ Journey(_, InProgress, _, _, Some(schedule), Some(_), _, _, _, _, _, _, _, _) =>
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

  def submitDirectDebit: Action[AnyContent] = actions.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.submitDirectDebit: $request")

    submissionService.authorizedForSsttp { journey =>
      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          views.direct_debit_form(
            journey.taxpayer.selfAssessment.debits,
            journey.schedule,
            formWithErrors))),
        validFormData => {
          directDebitConnector.validateBank(validFormData.sortCode, validFormData.accountNumber).flatMap { valid =>
            if (valid)
              submissionService.saveJourney(
                journey.copy(maybeBankDetails = Some(BankDetails(validFormData.sortCode, validFormData.accountNumber, validFormData.accountName)))
              ).map { _ =>
                  Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation())
                }
            else
              Future.successful(BadRequest(views.direct_debit_form(
                journey.taxpayer.selfAssessment.debits,
                journey.schedule,
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
