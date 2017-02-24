/*
 * Copyright 2017 HM Revenue & Customs
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
import javax.inject._

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.DirectDebitConnector
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._

import scala.collection.immutable.::
import scala.concurrent.Future

class DirectDebitController @Inject() (val messagesApi: play.api.i18n.MessagesApi, directDebitConnector: DirectDebitConnector) extends TimeToPayController with play.api.i18n.I18nSupport {

  def getDirectDebit: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(submission@TTPSubmission(Some(schedule), _, _, Some(taxpayer), _, _, calcData, _, _, _))
          if areEqual(taxpayer.selfAssessment.get.debits, calcData.debits) =>
          Future.successful(Ok(direct_debit_form(submission.calculatorData.debits, schedule, directDebitForm, submission.taxpayer.isDefined)))
        case _ => throw new RuntimeException("Invalid request submitted")
      }
  }

  def getDirectDebitAssistance: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(submission@TTPSubmission(Some(schedule), _, _, Some(taxpayer@Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
          Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, submission.taxpayer.isDefined)))
        case _ => throw new RuntimeException("No data found")
      }
  }

  def getDirectDebitError: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(submission@TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _)) =>
          Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, submission.taxpayer.isDefined,
            showErrorNotification = true)))
        case _ => throw new RuntimeException("No data found")
      }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(submission@TTPSubmission(Some(schedule), Some(_), _, _, _, _, _, _, _, _)) =>
          Future.successful(Ok(direct_debit_confirmation(submission.calculatorData.debits,
            schedule, submission.arrangementDirectDebit.get, submission.taxpayer.isDefined)))
        case _ =>
          Logger.error( s"Schedule missing from cache")
          Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable()))
      }
  }

  def getBankAccountNotFound: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(TTPSubmission(Some(_), _, Some(existingDDBanks), _, _, _, _, _, _, _)) =>
          Future.successful(Ok(account_not_found(existingBankAccountForm, existingDDBanks.directDebitInstruction, loggedIn = true)))
        case _ => throw new RuntimeException("No data found")
      }
  }

  def submitBankAccountNotFound: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case Some(ttpData@TTPSubmission(Some(_), _, Some(existingDDBanks), _, _, _, _, _, _, _)) =>
          existingBankAccountForm.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(account_not_found(formWithErrors, existingDDBanks.directDebitInstruction))),
            validFormData => (validFormData.existingDdi, validFormData.arrangementDirectDebit) match {
              case (Some(_), Some(_)) =>
                Future.successful(BadRequest(account_not_found(existingBankAccountForm, existingDDBanks.directDebitInstruction, loggedIn = true)))
              case (Some(ddi), None) =>
                val newBankDetails =
                  banksListValidation(existingDDBanks.directDebitInstruction.filter(_.referenceNumber.get == ddi), validFormData)
                sessionCache.put(ttpData.copy(bankDetails = Some(newBankDetails))).map[Result] {
                  _ => Redirect(routes.DirectDebitController.getDirectDebitConfirmation())
                }
              case (None, Some(arrangementDirectDebit: ArrangementDirectDebit)) =>
                directDebitConnector.getBank(arrangementDirectDebit.sortCode, arrangementDirectDebit.accountNumber).flatMap[Result] {
                  case Some(_) =>
                    val newBankDetails = banksListValidation(existingDDBanks.directDebitInstruction
                      .filter(_.accountNumber.get == arrangementDirectDebit.accountNumber), validFormData)
                    sessionCache.put(ttpData.copy(bankDetails = Some(newBankDetails))).map[Result] {
                      _ => Redirect(routes.DirectDebitController.getDirectDebitConfirmation())
                    }
                  case None => Future.successful(Redirect(routes.DirectDebitController.getBankAccountNotFound()))
                }
              case _ => Future.successful(Redirect(routes.DirectDebitController.getBankAccountNotFound()))
            })
        case _ => throw new RuntimeException("Unhandled case in submitBankAccountNotFound")
      }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = AuthorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp { submission =>
        directDebitForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(direct_debit_form(submission.get.calculatorData.debits,
            submission.get.schedule.get, formWithErrors, loggedIn = true))),
          validFormData =>
            directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode,
              validFormData.accountNumber.toString, authContext.principal.accounts.sa.get.utr)
              .flatMap(directDebitSubmitRouting(validFormData.accountName)))
      }
  }

  private def banksListValidation(bankDetails: Seq[DirectDebitInstruction], formData: ArrangementExistingDirectDebit): BankDetails = {
    bankDetails match {
      case bd :: tail =>
        BankDetails(sortCode = bd.sortCode, accountNumber = bd.accountNumber, ddiRefNumber = bd.referenceNumber)
      case Nil =>
        BankDetails(sortCode = Some(formData.arrangementDirectDebit.get.sortCode),
          accountNumber = Some(formData.arrangementDirectDebit.get.accountNumber),
          ddiRefNumber = None,
          accountName = Some(formData.arrangementDirectDebit.get.accountName))
    }
  }

  private def directDebitSubmitRouting(accName: String)(implicit hc: HeaderCarrier): PartialFunction[Either[BankDetails, DirectDebitBank], Future[Result]] = {
    case Left(singleBankDetails) =>
      sessionCache.get.flatMap {
        _.fold(Future.successful(redirectToStartPage))(ttp => {
          val taxpayer = ttp.taxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
          val sa = taxpayer.selfAssessment.getOrElse(throw new RuntimeException("No self assessment"))

          directDebitConnector.getBanks(SaUtr(sa.utr.get)).flatMap {
            directDebitBank => {
              val instructions: Seq[DirectDebitInstruction] = directDebitBank.directDebitInstruction.filter(p => {
                p.accountNumber.get.equalsIgnoreCase(singleBankDetails.accountNumber.get) && p.sortCode.get.equals(singleBankDetails.sortCode.get)
              })
              val bankDetailsToSave = instructions match {
                case instruction :: _ =>
                  val refNumber = instructions.filter(refNo => refNo.referenceNumber.isDefined).
                    map(instruction => instruction.referenceNumber).min
                  BankDetails(ddiRefNumber = refNumber,
                    accountNumber = instruction.accountNumber,
                    sortCode = instruction.sortCode,
                    accountName = Some(accName))
                case Nil => singleBankDetails.copy(accountName = Some(accName))
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
        () => TTPSubmission(None, None, Some(existingDDBanks), None, None))
        .map(_ => Redirect(toBankSelectionPage))
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  private val toDDCreationPage = routes.DirectDebitController.getDirectDebitConfirmation()
  private val toBankSelectionPage = routes.DirectDebitController.getBankAccountNotFound()
}
