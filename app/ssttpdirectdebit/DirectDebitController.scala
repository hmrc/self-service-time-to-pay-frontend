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
import journey.{Journey, JourneyService, Statuses}
import play.api.Logger
import play.api.mvc._
import req.RequestSupport
import ssttpdirectdebit.DirectDebitForm._
import times.ClockProvider
import timetopaytaxpayer.cor.model.Taxpayer
import uk.gov.hmrc.play.config.AssetsConfig
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views

import scala.collection.immutable.::
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
      case journey @ Journey(_, Statuses.InProgress, _, _, Some(schedule), _, _, Some(taxpayer), calcData, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_form(journey.taxpayer.selfAssessment.debits, schedule, directDebitForm, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebit: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitAssistance: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, Statuses.InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.getDueDate.toEpochDay()), schedule, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitAssistance: pattern match redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitError: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, Statuses.InProgress, _, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.getDueDate.toEpochDay()), schedule, true, isSignedIn)))
      case journey =>
        JourneyLogger.info("DirectDebitController.getDirectDebitError - redirect on error", journey)
        Future.successful(technicalDifficulties(journey))
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"DirectDebitController.getDirectDebitConfirmation: $request")

    submissionService.authorizedForSsttp {
      case Journey(_, Statuses.InProgress, _, _, _, _, Some(_), _, _, _, _, _, _) =>
        Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
      case submission @ Journey(_, Statuses.InProgress, _, _, Some(schedule), Some(_), _, _, _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_confirmation(
          debits      = submission.taxpayer.selfAssessment.debits,
          schedule    = schedule,
          directDebit = submission.arrangementDirectDebit.get,
          loggedIn    = isSignedIn
        )))
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
      case ttpData: Journey => Ok(views.direct_debit_unauthorised(isSignedIn))
      case _ =>
        JourneyLogger.info("DirectDebitController.getDirectDebitUnAuthorised - no TTPSubmission")
        Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start)
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
          views.direct_debit_form(submission.taxpayer.selfAssessment.debits,
                                  submission.schedule.get, formWithErrors))),
        validFormData => {

          directDebitConnector.validateBank(validFormData.sortCode, validFormData.accountNumber).flatMap { isValid =>
            if (isValid) checkBankDetails(sortCode      = validFormData.sortCode, accountNumber = validFormData.accountNumber, validFormData.accountName)
            else
              Future.successful(BadRequest(views.direct_debit_form(
                submission.taxpayer.selfAssessment.debits,
                submission.schedule.get,
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
  private def checkBankDetails(sortCode: String, accountNumber: String, accName: String)(implicit request: Request[_]) = {
    submissionService.getJourney.flatMap { journey =>

      val taxpayer = journey.maybeTaxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
      val utr = taxpayer.selfAssessment.utr

      directDebitConnector.getBanks(utr).flatMap {
        directDebitBank =>
          {
            val instructions: Seq[DirectDebitInstruction] = directDebitBank.directDebitInstruction.filter(p => {
              p.accountNumber.get.equalsIgnoreCase(accountNumber) && p.sortCode.get.equals(sortCode)
            })
            val bankDetailsToSave = instructions match {
              case instruction :: _ =>
                val refNumber = instructions.filter(refNo => refNo.referenceNumber.isDefined).
                  map(instruction => instruction.referenceNumber).min
                BankDetails(ddiRefNumber  = refNumber,
                            accountNumber = instruction.accountNumber,
                            sortCode      = instruction.sortCode,
                            accountName   = Some(accName))
              case Nil => BankDetails(sortCode      = Some(sortCode), accountNumber = Some(accountNumber), accountName = Some(accName))
            }
            submissionService.saveJourney(journey.copy(bankDetails = Some(bankDetailsToSave))).map {
              _ => Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation())
            }
          }
      }
    }
  }
}
