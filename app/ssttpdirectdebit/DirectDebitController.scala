/*
 * Copyright 2021 HM Revenue & Customs
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
import ssttpcalculator.CalculatorService
import ssttpdirectdebit.DirectDebitForm._
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.play.config.AssetsConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDirectDebit, BankDetails}
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class DirectDebitController @Inject() (
    mcc:                  MessagesControllerComponents,
    directDebitConnector: DirectDebitConnector,
    actions:              Actions,
    submissionService:    JourneyService,
    requestSupport:       RequestSupport,
    calculatorService:    CalculatorService,
    views:                Views)(implicit appConfig: AppConfig, ec: ExecutionContext, assetsConfig: AssetsConfig)
  extends FrontendBaseController(mcc) {

  import requestSupport._

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
      val directDebit = journey.arrangementDirectDebit.getOrElse(throw new RuntimeException(s"arrangement direct debit not found on submission [${journey.obfuscate}]"))
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
