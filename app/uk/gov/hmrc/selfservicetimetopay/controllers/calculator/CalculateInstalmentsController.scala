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

import java.time.LocalDate

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._



import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector}
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorInput, EligibilityRequest, EligibilityStatus}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class CalculateInstalmentsController(eligibilityConnector: EligibilityConnector,
                                     calculatorConnector: CalculatorConnector) extends TimeToPayController {

  def submit() = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData) =>
        eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), ttpData.taxPayer)).map {
          case EligibilityStatus(true, _) =>
            // TODO - wire up the future, don't return until the calculation is complete
            calculatorConnector.calculatePaymentSchedule(CalculatorInput(ttpData.manualDebits.get))
            Ok
          case _ => Redirect("Route to ineligible page")
        }
      case _ => throw new RuntimeException("No TTP Data in sesson")
    }
  }

  def getCalculateInstalments(monthsOption: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    // create fake schedule
    sessionCache.get.map {
      case Some(TTPSubmission(_, _, _, _, _, _, debits@Some(_), paymentToday@Some(_))) =>
        // TODO replace the below with a call to the orchestration layer (self-service-time-pay service)
        val total = debits.get.map(_.amount).sum
        val form = CalculatorForm.createPaymentTodayForm(total)
        val instalments = Seq(
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 3, 1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 4, 1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 5, 1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 6, 1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 7, 1), 350),
          CalculatorPaymentScheduleInstalment(LocalDate.of(2017, 8, 1), 350))
        val schedule = CalculatorPaymentSchedule(startDate = Some(LocalDate.of(2017, 2, 1)), endDate = Some(LocalDate.of(2017, 12, 1)),
          initialPayment = paymentToday.get.amount,
          amountToPay = total,
          instalmentBalance = total - paymentToday.get.amount,
          totalInterestCharged = total * 0.0275,
          totalPayable = total + total * 0.0275,
          instalments = instalments)

        val instalmentOptionsAscending = Seq(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        Ok(calculate_instalments_form(schedule, CalculatorForm.durationForm, form.fill(paymentToday.get), instalmentOptionsAscending))
      // TODO Redirect to approrpiate page - i.e. start of service
      case _ => Ok(amounts_due_form.render(CalculatorAmountsDue(IndexedSeq.empty), CalculatorForm.amountDueForm, request))
    }

  }
}