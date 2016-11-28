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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors.{DirectDebitConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.forms.DirectDebitForm._
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitBank}
import views.html.selfservicetimetopay.arrangement._

import scala.concurrent.Future

class DirectDebitController(directDebitConnector: DirectDebitConnector,
                            sessionCache: SessionCacheConnector,
                            authConnector: AuthConnector) extends FrontendController {

  def directDebitPresent:Action[AnyContent] = Action { implicit request =>
    Ok(direct_debit_form.render(createDirectDebitForm, request) )
  }

  def directDebitConfirmationPresent:Action[AnyContent] = Action {implicit request =>
    val form:Form[Boolean] = Form(single("confirm" -> boolean))
    Ok(direct_debit_confirmation.render(
      generatePaymentSchedules(BigDecimal("2000.00"), Some(BigDecimal("100.00"))).last,
      arrangementDirectDebit, request))
  }

  def directDebitConfirmationSubmit:Action[AnyContent] = Action {implicit request =>
    Redirect(routes.ArrangementController.submit())
  }

  def directDebitSubmit:Action[AnyContent] = Action.async {implicit request =>
    createDirectDebitForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(direct_debit_form.render(formWithErrors, request))),
      validFormData =>
        authConnector.currentAuthority.flatMap {
          case Some(authority) if authority.accounts.sa.exists(_ => true) =>
            directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode, validFormData.accountNumber.toString, authority.accounts.sa.get.utr)
          case _ => throw new RuntimeException("Current user does not have SA enrolment")
        }.map(directDebitSubmitRouting)
    )
  }

  private def directDebitSubmitRouting(implicit hc: HeaderCarrier): PartialFunction[Either[BankDetails, DirectDebitBank], Result] = {
    case Left(singleBankDetails) => Redirect(routes.DirectDebitController.directDebitConfirmationPresent())
    case Right(existingDDBanks) => Redirect(routes.DirectDebitController.directDebitPresent())
  }

  def scheduleSummaryDayOfMonthSubmit: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.DirectDebitController.directDebitPresent())
  }
}