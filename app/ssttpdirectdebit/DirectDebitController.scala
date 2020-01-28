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
import play.api.Logger
import play.api.mvc._
import req.RequestSupport
import ssttpdirectdebit.DirectDebitForm._
import journey.{Journey, JourneyService}
import timetopaytaxpayer.cor.model
import timetopaytaxpayer.cor.model.{Debit, Taxpayer}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.Views
import views.html.arrangement._
import views.html.core.service_start

import scala.collection.immutable.::
import scala.concurrent.{ExecutionContext, Future}

class DirectDebitController @Inject() (
    mcc:                  MessagesControllerComponents,
    directDebitConnector: DirectDebitConnector,
    as:                   Actions,
    submissionService:    JourneyService,
    requestSupport:       RequestSupport,
    views:                Views)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._

  def getDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case submission @ Journey(_, _, Some(schedule), _, _, Some(taxpayer), calcData, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_form(submission.taxpayer.selfAssessment.debits, schedule, directDebitForm, isSignedIn)))
      case _ => Future.successful(redirectToStartPage)
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, isSignedIn)))
      case _ => Future.successful(redirectToStartPage)
    }
  }

  def getDirectDebitError: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, _, Some(schedule), _, _, Some(Taxpayer(_, _, sa)), _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, true, isSignedIn)))
      case _ => Future.successful(redirectToStartPage)
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case Journey(_, _, _, _, Some(_), _, _, _, _, _, _) =>
        Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
      case submission @ Journey(_, _, Some(schedule), Some(_), _, _, _, _, _, _, _) =>
        Future.successful(Ok(views.direct_debit_confirmation(submission.taxpayer.selfAssessment.debits,
                                                             schedule, submission.arrangementDirectDebit.get, isSignedIn)))
      case _ =>
        Logger.error(s"Bank details missing from cache on Direct Debit Confirmation page")
        Future.successful(redirectToStartPage)
    }
  }

  def getDirectDebitUnAuthorised: Action[AnyContent] = as.action.async { implicit request =>
    submissionService.getJourney.map {
      case ttpData: Journey => Ok(views.direct_debit_unauthorised(isSignedIn))
      case _ =>
        Logger.warn("No TTPSubmission, redirecting to start page")
        Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start)
    }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = as.action { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { submission: Journey =>

      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(
          views.direct_debit_form(submission.taxpayer.selfAssessment.debits,
                                  submission.schedule.get, formWithErrors))),
        validFormData => {

          for {
            bankDetails <- directDebitConnector.getBank(validFormData.sortCode, validFormData.accountNumber.toString)
            result <- bankDetails match {
              case Some(bankDetails) => checkBankDetails(bankDetails, validFormData.accountName)
              case None =>
                Future.successful(BadRequest(views.direct_debit_form(
                  submission.taxpayer.selfAssessment.debits,
                  submission.schedule.get,
                  directDebitFormWithBankAccountError.copy(data = Map(
                    "accountName" -> validFormData.accountName,
                    "accountNumber" -> validFormData.accountNumber,
                    "sortCode" -> validFormData.sortCode)
                  ),
                  isBankError = true
                )))
            }
          } yield result

        }
      )
    }
  }

  /**
   * Using saUtr, gets a list of banks associated with the user. Checks the user entered
   * bank details to see if they already exist. If it does, return existing bank details
   * otherwise return user entered bank details.
   */
  private def checkBankDetails(bankDetails: BankDetails, accName: String)(implicit request: Request[_]) = {
    submissionService.getJourney.flatMap { journey =>

      val taxpayer = journey.maybeTaxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
      val utr = taxpayer.selfAssessment.utr

      directDebitConnector.getBanks(utr).flatMap {
        directDebitBank =>
          {
            val instructions: Seq[DirectDebitInstruction] = directDebitBank.directDebitInstruction.filter(p => {
              p.accountNumber.get.equalsIgnoreCase(bankDetails.accountNumber.get) && p.sortCode.get.equals(bankDetails.sortCode.get)
            })
            val bankDetailsToSave = instructions match {
              case instruction :: _ =>
                val refNumber = instructions.filter(refNo => refNo.referenceNumber.isDefined).
                  map(instruction => instruction.referenceNumber).min
                BankDetails(ddiRefNumber  = refNumber,
                            accountNumber = instruction.accountNumber,
                            sortCode      = instruction.sortCode,
                            accountName   = Some(accName))
              case Nil => bankDetails.copy(accountName = Some(accName))
            }
            submissionService.saveJourney(journey.copy(bankDetails = Some(bankDetailsToSave))).map {
              _ => Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation())
            }
          }
      }
    }
  }
}