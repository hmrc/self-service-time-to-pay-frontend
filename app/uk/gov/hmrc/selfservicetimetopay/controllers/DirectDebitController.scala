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

import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.DirectDebitConnector
import uk.gov.hmrc.selfservicetimetopay.controllerVariables.fakeBankDetails
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models.{Debit, BankDetails, DirectDebitBank, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._

import scala.concurrent.Future

class DirectDebitController(directDebitConnector: DirectDebitConnector) extends TimeToPayController {

  def getDirectDebit: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      Future.successful(Ok(direct_debit_form.render(directDebitForm, request)))
  }

  def getDirectDebitAssistance: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(submission @ TTPSubmission(Some(schedule), _, _, _, _, _, _)) =>
        Ok(direct_debit_assistance.render(List(
          Debit(Some("ASST"), 99, LocalDate.now()),
          Debit(Some("DPI"), 2323, LocalDate.now())), request))
//        Ok(direct_debit_assistance.render(submission.taxPayer.get.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()), request))
      //      case _ => throw new RuntimeException("No data found")
      case _ =>
        Ok(direct_debit_assistance.render(List(
          Debit(Some("ASST"), 99, LocalDate.now()),
          Debit(Some("DPI"), 2323, LocalDate.now())), request))
    }
  }

  def getDirectDebitError: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      sessionCache.get.map {
        case Some(TTPSubmission(_, _, banks@Some(_), _, _, _, _)) =>
          Ok(direct_debit_error.render(directDebitForm, banks, request))
        case _ => Ok(direct_debit_error.render(directDebitForm, None, request))
      }
  }

  def getDirectDebitConfirmation: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      sessionCache.get.map {
        case Some(submission@TTPSubmission(Some(schedule), Some(bankDetails), _, _, _, _, _)) =>
          Ok(showDDConfirmation(schedule, submission.arrangementDirectDebit.get, request))
        case _ => throw new RuntimeException("No data found")
      }
  }

  def getBankAccountNotFound: Action[AnyContent] = Action { implicit request =>
    Ok(account_not_found(existingBankAccountForm, fakeBankDetails))
  }

  def submitBankAccountNotFound: Action[AnyContent] = Action { implicit request =>
    Ok(account_not_found(existingBankAccountForm, fakeBankDetails))
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      directDebitForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(bankDetailsFormErrorPage(formWithErrors, request))),
        validFormData =>
          directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode,
            validFormData.accountNumber.toString, authContext.principal.accounts.sa.get.utr)
            .flatMap(directDebitSubmitRouting))
  }

  private def directDebitSubmitRouting(implicit hc: HeaderCarrier): PartialFunction[Either[BankDetails, DirectDebitBank], Future[Result]] = {
    case Left(singleBankDetails) =>
      updateOrCreateInCache(found => found.copy(bankDetails = Some(singleBankDetails)),
        () => TTPSubmission(bankDetails = Some(singleBankDetails)))
        .map(_ => Redirect(toDDCreationPage))

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