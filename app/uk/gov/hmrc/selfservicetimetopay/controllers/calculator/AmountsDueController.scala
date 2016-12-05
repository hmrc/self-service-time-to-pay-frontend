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

package uk.gov.hmrc.selfservicetimetopay.controllers.calculator

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorAmountsDue, CalculatorInput, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

class AmountsDueController extends TimeToPayController {

  def start: Action[AnyContent] = Action { request =>
    Redirect(routes.AmountsDueController.getAmountsDue())
  }

  def getAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) =>
        Ok(amounts_due_form.render(CalculatorAmountsDue(debits), CalculatorForm.amountDueForm, request))
      case _ => Ok(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm, request))
    }
  }

  def submitAddAmountDue: Action[AnyContent] = Action.async { implicit request =>
    CalculatorForm.amountDueForm.bindFromRequest().fold(
      formWithErrors =>
        sessionCache.get.map {
          case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _)))=>
            BadRequest(amounts_due_form.render(CalculatorAmountsDue(debits), formWithErrors, request))
          case _ => BadRequest(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), formWithErrors, request))
        },

      validFormData => {
        sessionCache.get.map {
          case Some(ttpSubmission @ TTPSubmission(_, _, _, _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _))) =>
            ttpSubmission.copy(calculatorData = cd.copy(debits = debits :+ validFormData))
          case Some(ttpSubmission @ TTPSubmission(_, _, _, _, Some(_),Some(_), cd)) =>
            ttpSubmission.copy(calculatorData = cd.copy(debits = IndexedSeq(validFormData)))
          case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq(validFormData)))
        }.map { ttpData =>
          Logger.info(ttpData.toString)
          sessionCache.put(ttpData)
        }.map { _ =>
          Redirect(routes.AmountsDueController.getAmountsDue())
        }
      }
    )
  }

  def submitRemoveAmountDue: Action[AnyContent] = Action.async { implicit request =>
    val index = CalculatorForm.removeAmountDueForm.bindFromRequest()
    sessionCache.get.map {
      case Some(ttpSubmission @ TTPSubmission(_, _, _, _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _))) =>
        ttpSubmission.copy(calculatorData = cd.copy(debits = debits.patch(index.value.get, Nil, 1)))
      case _ => TTPSubmission(calculatorData = CalculatorInput.initial.copy(debits = IndexedSeq.empty))
    }.map { ttpData => sessionCache.put(ttpData)}
      .map { _ => Redirect(routes.AmountsDueController.getAmountsDue())}
  }

  def submitAmountsDue: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, CalculatorInput(debits, _, _, _, _, _))) => Some(debits)
      case _ => Some(IndexedSeq.empty)
    }.map { ttpData =>
      if(ttpData.isEmpty) BadRequest(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty),
        CalculatorForm.amountDueForm.withGlobalError("You need to add at least one amount due"), request))
      else Redirect(routes.PaymentTodayController.getPaymentToday())
    }
  }
}