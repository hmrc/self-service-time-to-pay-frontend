/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.FrontendController
import controllers.action.Actions
import javax.inject._
import play.api.Logger
import play.api.mvc._
import ssttpdirectdebit.DirectDebitForm._
import sttpsubmission.SubmissionService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._
import views.html.selfservicetimetopay.core.service_start

import scala.collection.immutable.::
import scala.concurrent.Future

class DirectDebitController @Inject() (
    i18nSupport:          play.api.i18n.I18nSupport,
    directDebitConnector: DirectDebitConnector,
    as:                   Actions,
    submissionService:    SubmissionService
)(implicit appConfig: AppConfig)
  extends FrontendController {
  import i18nSupport._

  def getDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case submission @ TTPSubmission(Some(schedule), _, _, Some(taxpayer), calcData, _, _, _, _, _) if areEqual(taxpayer.selfAssessment.get.debits, calcData.debits) =>
        Future.successful(Ok(direct_debit_form(submission.calculatorData.debits, schedule, directDebitForm, isSignedIn)))
      case _ => Future.successful(redirectOnError)
    }
  }

  def getDirectDebitAssistance: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _) =>
        Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, isSignedIn)))
      case _ => Future.successful(redirectOnError)
    }
  }

  def getDirectDebitError: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _) =>
        Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, true, isSignedIn)))
      case _ => Future.successful(redirectOnError)
    }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case TTPSubmission(_, _, Some(_), _, _, _, _, _, _, _) =>
        Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
      case submission @ TTPSubmission(Some(schedule), Some(_), _, _, _, _, _, _, _, _) =>
        Future.successful(Ok(direct_debit_confirmation(submission.calculatorData.debits,
                                                       schedule, submission.arrangementDirectDebit.get, isSignedIn)))
      case _ =>
        Logger.error(s"Bank details missing from cache on Direct Debit Confirmation page")
        Future.successful(redirectOnError)
    }
  }

  def getDirectDebitUnAuthorised: Action[AnyContent] = as.checkSessionAction.async { implicit request =>
    submissionService.getTtpSessionCarrier.map {
      case Some(ttpData: TTPSubmission) => Ok(direct_debit_unauthorised(isSignedIn))
      case _                            => Ok(service_start(isSignedIn))
    }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = as.checkSessionAction { implicit request =>
    Redirect(ssttparrangement.routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp { submission =>
      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(direct_debit_form(submission.calculatorData.debits,
                                                                         submission.schedule.get, formWithErrors))),
        validFormData => {
          directDebitConnector.getBank(validFormData.sortCode,
                                       validFormData.accountNumber.toString).flatMap {
              case Some(bankDetails) => checkBankDetails(bankDetails, validFormData.accountName)
              case None =>
                Future.successful(BadRequest(direct_debit_form(submission.calculatorData.debits,
                                                               submission.schedule.get, directDebitFormWithBankAccountError.copy(data = Map("accountName" -> validFormData.accountName,
                    "accountNumber" -> validFormData.accountNumber, "sortCode" -> validFormData.sortCode)),
                                                               isBankError = true)
                ))
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
  private def checkBankDetails(bankDetails: BankDetails, accName: String)(implicit hc: HeaderCarrier) = {
    submissionService.getTtpSessionCarrier.flatMap {
      _.fold(Future.successful(redirectToStartPage))(ttp => {
        val taxpayer = ttp.taxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
        val sa = taxpayer.selfAssessment.getOrElse(throw new RuntimeException("No self assessment"))

        directDebitConnector.getBanks(SaUtr(sa.utr.get)).flatMap {
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
              submissionService.putTtpSessionCarrier(ttp.copy(bankDetails = Some(bankDetailsToSave))).map {
                _ => Redirect(toDDCreationPage)
              }
            }
        }
      })
    }
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  private val toDDCreationPage = ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation()
}