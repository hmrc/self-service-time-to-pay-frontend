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

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.{DirectDebitConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement._

import scala.concurrent.Future

class DirectDebitController(directDebitConnector: DirectDebitConnector,
                            sessionCacheConnector: SessionCacheConnector,
                            authConnector: AuthConnector) extends FrontendController {

  def getDirectDebit: Action[AnyContent] = Action { implicit request =>
    Ok(direct_debit_form.render(createDirectDebitForm, request))
  }

  def getDirectDebitConfirmation: Action[AnyContent] = Action.async { implicit request =>
    val form: Form[Boolean] = Form(single("confirm" -> boolean))
    sessionCacheConnector.get.map {
      // TODO - the gets, the gets!
      case Some(ttpSubmission) => Ok(showDDConfirmation(ttpSubmission.schedule.get, ttpSubmission.arrangementDirectDebit.get, request))
      case None => throw new RuntimeException("No data found")
    }
  }

  def submitDirectDebitConfirmation: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def submitDirectDebit: Action[AnyContent] = Action.async { implicit request =>
    createDirectDebitForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(bankDetailsFormErrorPage(formWithErrors, request))),
      validFormData =>
        authConnector.currentAuthority.flatMap {
          case Some(authority) if authority.accounts.sa.exists(_ => true) =>
            directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode, validFormData.accountNumber.toString, authority.accounts.sa.get.utr)
          case Some(authority) => throw new RuntimeException("Current user does not have SA enrolment")
          case _ => throw new RuntimeException("Current user is unauthorised")
        }.flatMap(directDebitSubmitRouting))
  }

  private def directDebitSubmitRouting(implicit hc: HeaderCarrier): PartialFunction[Either[BankDetails, DirectDebitBank], Future[Result]] = {
    case Left(singleBankDetails) =>
      updateOrCreateInCache(found => TTPSubmission(schedule = found.schedule,
                                      bankDetails = Some(singleBankDetails),
                                      existingDDBanks = found.existingDDBanks,
                                      taxPayer = found.taxPayer,
                                      eligibilityTypeOfTax = found.eligibilityTypeOfTax,
                                      eligibilityExistingTtp = found.eligibilityExistingTtp),
        () => TTPSubmission(bankDetails = Some(singleBankDetails)))
        .map(_ => Redirect(toDDCreationPage))

    case Right(existingDDBanks) =>
      updateOrCreateInCache(found => TTPSubmission(schedule = found.schedule,
                                      bankDetails = found.bankDetails,
                                      existingDDBanks = Some(existingDDBanks),
                                      taxPayer = found.taxPayer,
                                      eligibilityTypeOfTax = found.eligibilityTypeOfTax,
                                      eligibilityExistingTtp = found.eligibilityExistingTtp),
        () => TTPSubmission(None, None, Some(existingDDBanks), None))
        .map(_ => Redirect(toBankSelectionPage))
  }

  private def updateOrCreateInCache(found: (TTPSubmission) => TTPSubmission, notFound: () => TTPSubmission)(implicit hc: HeaderCarrier) = {
    sessionCacheConnector.get.flatMap {
      case Some(ttpSubmission) =>
        Logger.info("TTP data found - merging record")
        sessionCacheConnector.put(found(ttpSubmission))
      case None =>
        Logger.info("No TTP Submission data found in cache")
        sessionCacheConnector.put(notFound())
    }
  }

  private val showDDConfirmation = direct_debit_confirmation.render _
  private val bankDetailsFormErrorPage = direct_debit_form.render _
  private val toDDCreationPage = routes.DirectDebitController.getDirectDebitConfirmation()
  private val toBankSelectionPage = routes.DirectDebitController.getDirectDebit()
}