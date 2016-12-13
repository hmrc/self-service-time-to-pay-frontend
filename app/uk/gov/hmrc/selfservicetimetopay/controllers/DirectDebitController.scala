/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.controllers

import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.{CampaignManagerConnector, DirectDebitConnector}
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._

import scala.collection.immutable.::
import scala.concurrent.Future

class DirectDebitController(directDebitConnector: DirectDebitConnector) extends TimeToPayController {

  def getDirectDebit: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        Future.successful(Ok(direct_debit_form.render(directDebitForm, request)))
      }
  }

  def getDirectDebitAssistance: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.map {
          case Some(submission@TTPSubmission(Some(schedule), _, _, Some(taxpayer@Taxpayer(_, _, Some(sa))), _, _, _)) =>
            Ok(direct_debit_assistance.render(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, request))
          case _ => throw new RuntimeException("No data found")
        }
      }
  }

  def getDirectDebitError: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.map {
          case Some(TTPSubmission(_, _, banks@Some(_), _, _, _, _)) =>
            Ok(direct_debit_error.render(directDebitForm, banks, request))
          case _ => Ok(direct_debit_error.render(directDebitForm, None, request))
        }
      }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.map {
          case Some(submission@TTPSubmission(Some(schedule), Some(bankDetails), _, _, _, _, _)) =>
            Ok(showDDConfirmation(schedule, submission.arrangementDirectDebit.get, request))
          case _ => throw new RuntimeException("No data found")
        }
      }
  }

  def getBankAccountNotFound: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.map {
          case Some(submission@TTPSubmission(Some(_), _, Some(existingDDBanks), _, _, _, _)) =>
            Ok(account_not_found(existingBankAccountForm, existingDDBanks.directDebitInstruction))
          case _ => throw new RuntimeException("No data found")
        }
      }
  }

  private def banksListValidation(bankDetails: Seq[DirectDebitInstruction], formData: ArrangementExistingDirectDebit): BankDetails = {
    bankDetails match {
      case bd :: Nil =>
        val selectedBankDetails = bankDetails.head
        BankDetails(sortCode = selectedBankDetails.sortCode.get,
          accountNumber = selectedBankDetails.accountNumber.get,
          ddiRefNumber = selectedBankDetails.referenceNumber)
      case Nil =>
        BankDetails(sortCode = formData.arrangementDirectDebit.get.sortCode,
          accountNumber = formData.arrangementDirectDebit.get.accountNumber,
          ddiRefNumber = None)
    }
  }

  def submitBankAccountNotFound: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.flatMap {
          case Some(ttpData@TTPSubmission(Some(_), _, Some(existingDDBanks), _, _, _, _)) =>
            existingBankAccountForm.bindFromRequest().fold(
              formWithErrors => Future.successful(BadRequest(account_not_found(formWithErrors, existingDDBanks.directDebitInstruction))),
              validFormData => (validFormData.existingDdi, validFormData.arrangementDirectDebit) match {
                case (Some(_), Some(_)) => Future.successful(BadRequest(account_not_found(existingBankAccountForm, existingDDBanks.directDebitInstruction)))
                case (Some(ddi), None) =>
                  val newBankDetails = banksListValidation(existingDDBanks.directDebitInstruction.filter(_.referenceNumber.get == ddi), validFormData)
                  sessionCache.put(ttpData.copy(bankDetails = Some(newBankDetails))).map[Result] {
                    _ => Redirect(routes.DirectDebitController.getDirectDebitConfirmation())
                  }
                case (None, Some(arrangementDirectDebit: ArrangementDirectDebit)) =>
                  directDebitConnector.getBank(arrangementDirectDebit.sortCode, arrangementDirectDebit.accountNumber).flatMap[Result] {
                    case Some(bankDetails) =>
                      val newBankDetails = banksListValidation(existingDDBanks.directDebitInstruction
                        .filter(_.accountNumber.get == arrangementDirectDebit.accountNumber), validFormData)
                      sessionCache.put(ttpData.copy(bankDetails = Some(newBankDetails))).map[Result] {
                        _ => Redirect(routes.DirectDebitController.getDirectDebitConfirmation())
                      }
                    case None => Future.successful(Redirect(routes.DirectDebitController.getBankAccountNotFound()))
                  }
                case _ => Future.successful(Redirect(routes.DirectDebitController.getBankAccountNotFound()))
              })
        }
      }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        directDebitForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(bankDetailsFormErrorPage(formWithErrors, request))),
          validFormData =>
            directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode,
              validFormData.accountNumber.toString, authContext.principal.accounts.sa.get.utr)
              .flatMap(directDebitSubmitRouting))
      }
  }

  private def directDebitSubmitRouting(implicit hc: HeaderCarrier): PartialFunction[Either[BankDetails, DirectDebitBank], Future[Result]] = {
    case Left(singleBankDetails) =>
      sessionCache.get.flatMap {
        _.fold(Future.successful(redirectToStartPage))(ttp => {

          val taxpayer = ttp.taxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
          val sa = taxpayer.selfAssessment.getOrElse(throw new RuntimeException("No self assessment"))

          directDebitConnector.getBanks(SaUtr(sa.utr.get)).flatMap {
            directDebitBank => {
              val instructions: Seq[DirectDebitInstruction] = directDebitBank.directDebitInstruction.filter(p => {
                p.accountNumber.get.equalsIgnoreCase(singleBankDetails.accountNumber) && p.sortCode.get.equals(singleBankDetails.sortCode)
              })

              val bankDetailsToSave = instructions match {
                case instruction :: Nil =>
                  BankDetails(singleBankDetails.sortCode, singleBankDetails.accountNumber, None, None, None,
                    Some(instructions.head.referenceNumber.get))
                case Nil => singleBankDetails
              }

              sessionCache.put(ttp.copy(bankDetails = Some(bankDetailsToSave))).map {
                _ => Redirect(toDDCreationPage)
              }
            }
          }
        })
      }
    case Right(existingDDBanks) =>
      updateOrCreateInCache(found => found.copy(existingDDBanks = Some(existingDDBanks)),
        () => TTPSubmission(None, None, Some(existingDDBanks), None))
        .map(_ => Redirect(toBankSelectionPage))
  }

  private val showDDConfirmation = direct_debit_confirmation.render _
  private val bankDetailsFormErrorPage = direct_debit_form.render _
  private val toDDCreationPage = routes.DirectDebitController.getDirectDebitConfirmation()
  private val toBankSelectionPage = routes.DirectDebitController.getBankAccountNotFound()
}