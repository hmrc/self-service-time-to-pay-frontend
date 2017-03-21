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
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.DirectDebitConnector
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._

import scala.collection.immutable.::
import scala.concurrent.Future

class DirectDebitController @Inject()(val messagesApi: play.api.i18n.MessagesApi, directDebitConnector: DirectDebitConnector)
  extends TimeToPayController with play.api.i18n.I18nSupport {

  def getDirectDebit: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case submission@TTPSubmission(Some(schedule), _, _, Some(taxpayer), _, _, calcData, _, _, _)
          if areEqual(taxpayer.selfAssessment.get.debits, calcData.debits) =>
          Future.successful(Ok(direct_debit_form(submission.calculatorData.debits, schedule, directDebitForm, submission.taxpayer.isDefined)))
        case _ => Future.successful(redirectOnError)
      }
  }

  def getDirectDebitAssistance: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _) =>
          Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule)))
        case _ => Future.successful(redirectOnError)
      }
  }

  def getDirectDebitError: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case TTPSubmission(Some(schedule), _, _, Some(Taxpayer(_, _, Some(sa))), _, _, _, _, _, _) =>
          Future.successful(Ok(direct_debit_assistance(sa.debits.sortBy(_.dueDate.toEpochDay()), schedule, showErrorNotification = true)))
        case _ => Future.successful(redirectOnError)
      }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp {
        case submission@TTPSubmission(_, _, Some(existingDDBanks), _, _, _, _, _, _, _) =>
          Future.successful(Redirect(routes.DirectDebitController.getDirectDebit()))
        case submission@TTPSubmission(Some(schedule), Some(_), _, _, _, _, _, _, _, _) =>
          Future.successful(Ok(direct_debit_confirmation(submission.calculatorData.debits,
            schedule, submission.arrangementDirectDebit.get, submission.taxpayer.isDefined)))
        case _ =>
          Logger.error(s"Bank details missing from cache on Direct Debit Confirmation page")
          Future.successful(redirectOnError)
      }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      authorizedForSsttp { submission =>
        directDebitForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(direct_debit_form(submission.calculatorData.debits,
            submission.schedule.get, filterSortCodeErrors(formWithErrors)))),
          validFormData => {
            directDebitConnector.getBank(validFormData.sortCode,
              validFormData.accountNumber.toString).flatMap {
              case Some(bankDetails) => checkBankDetails(bankDetails, validFormData.accountName)
              case None =>
                val (sc1 :: sc2 :: sc3 :: _) = validFormData.sortCode.grouped(2).toList
                Future.successful(BadRequest(direct_debit_form(submission.calculatorData.debits,
                  submission.schedule.get, directDebitFormWithBankAccountError.copy(data = Map("accountName" -> validFormData.accountName,
                    "accountNumber" -> validFormData.accountNumber, "sortCode1" -> sc1, "sortCode2" -> sc2, "sortCode3" -> sc3)),
                  isBankError = true)
                ))
            }
          }
        )
      }
  }

  def filterSortCodeErrors(form: Form[ArrangementDirectDebit]): Form[ArrangementDirectDebit] = {
    val sortCodeRequiredMessage = Seq("ssttp.direct-debit.form.error.sortCode.required")

    def removeEnterSortCodeErrors(errors: Seq[FormError]): Seq[FormError] = {
      errors.map(e => if (e.messages == sortCodeRequiredMessage) {
        e.copy(messages = Seq("ssttp.direct-debit.form.error.sortCode.not-valid"))
      } else e)
    }

    val countOfSortCodeEnter = form.errors.count(_.messages == sortCodeRequiredMessage)
    val checkSort = if (countOfSortCodeEnter < 3) form.copy(errors = removeEnterSortCodeErrors(form.errors)) else form
    val formErrorsNoDuplicates = checkSort.errors.foldLeft[Seq[FormError]](Nil) {
      (acc, formError) => {
        if (acc.exists(_.message == formError.message)) acc :+ formError.copy(messages = Seq(" ")) else acc :+ formError
      }
    }
    form.copy(errors = formErrorsNoDuplicates)
  }

  private def checkBankDetails(bankDetails: BankDetails, accName: String)(implicit hc: HeaderCarrier) = {
    sessionCache.get.flatMap {
      _.fold(Future.successful(redirectToStartPage))(ttp => {
        val taxpayer = ttp.taxpayer.getOrElse(throw new RuntimeException("No taxpayer"))
        val sa = taxpayer.selfAssessment.getOrElse(throw new RuntimeException("No self assessment"))

        directDebitConnector.getBanks(SaUtr(sa.utr.get)).flatMap {
          directDebitBank => {
            val instructions: Seq[DirectDebitInstruction] = directDebitBank.directDebitInstruction.filter(p => {
              p.accountNumber.get.equalsIgnoreCase(bankDetails.accountNumber.get) && p.sortCode.get.equals(bankDetails.sortCode.get)
            })
            val bankDetailsToSave = instructions match {
              case instruction :: _ =>
                val refNumber = instructions.filter(refNo => refNo.referenceNumber.isDefined).
                  map(instruction => instruction.referenceNumber).min
                BankDetails(ddiRefNumber = refNumber,
                  accountNumber = instruction.accountNumber,
                  sortCode = instruction.sortCode,
                  accountName = Some(accName))
              case Nil => bankDetails.copy(accountName = Some(accName))
            }
            sessionCache.put(ttp.copy(bankDetails = Some(bankDetailsToSave))).map {
              _ => Redirect(toDDCreationPage)
            }
          }
        }
      })
    }
  }


  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  private val toDDCreationPage = routes.DirectDebitController.getDirectDebitConfirmation()
}
