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
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.connectors.DirectDebitConnector
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement._

import scala.concurrent.Future

object DirectDebitController extends FrontendController {

  val directDebitConnector = DirectDebitConnector

  private def createDirectDebitForm:Form[ArrangementDirectDebit] = {
    Form(mapping(
      "accountHolderName" -> nonEmptyText,
      "sortCode1" -> number(min = 0, max = 99),
      "sortCode2" -> number(min = 0, max = 99),
      "sortCode3" -> number(min = 0, max = 99),
      "accountNumber" -> longNumber(min = 0, max = 999999999),
      "confirmed" -> optional(boolean)
    )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply))
  }

  def directDebitPresent:Action[AnyContent] = Action.async {implicit request =>
    val form = createDirectDebitForm
    Future.successful(Ok(direct_debit_form.render(form, request) ) )
  }

  def directDebitSubmit:Action[AnyContent] = Action.async {implicit request =>
    createDirectDebitForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(direct_debit_form.render(formWithErrors, request)))
      },
      validFormData => {
        //utr = keystoreConnector.fetch[saUtr](KeystoreKeys.UTR)
        directDebitConnector.validateOrRetrieveAccounts(validFormData.sortCode, validFormData.accountNumber.toString, SaUtr("")).map {
          case Left(singleBankDetails) => Redirect(routes.DirectDebitController.directDebitConfirmationPresent())
          case Right(existingDDBanks) => Redirect(routes.DirectDebitController.directDebitPresent())
        }
      }
    )
  }

  def directDebitConfirmationPresent:Action[AnyContent] = Action.async {implicit request =>
    val form:Form[Boolean] = Form(single("confirm" -> boolean))
    Future.successful(Ok(direct_debit_confirmation.render(
      generatePaymentSchedules(BigDecimal("2000.00"), Some(BigDecimal("100.00"))).last,
      arrangementDirectDebit, request))
    )
  }

  def directDebitConfirmationSubmit:Action[AnyContent] = Action.async {implicit request =>
    Future.successful(Redirect(routes.ArrangementController.applicationCompletePresent()))
  }
}