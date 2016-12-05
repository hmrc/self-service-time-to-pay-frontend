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
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, EligibilityConnector}
import uk.gov.hmrc.selfservicetimetopay.forms.CalculatorForm
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorInput, EligibilityRequest, EligibilityStatus, _}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.calculator._

import scala.concurrent.Future

class CalculateInstalmentsController(eligibilityConnector: EligibilityConnector,
                                     calculatorConnector: CalculatorConnector) extends TimeToPayController {
  def getCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      case Some(ttpData @ TTPSubmission(_, _, _, _, _, _, Some(debits), Some(paymentToday))) =>
        eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), ttpData.taxPayer)).flatMap {
          case EligibilityStatus(true, _) =>
            val total = debits.map(_.amount).sum
            val form = CalculatorForm.createPaymentTodayForm(total).fill(paymentToday)

            calculatorConnector.calculatePaymentSchedule(CalculatorInput(debits)).map {
              case Some(Seq(schedule)) =>
                Ok(calculate_instalments_form(schedule, CalculatorForm.durationForm, form, 2 to 11))
              case _ => throw new RuntimeException("Failed to get schedule")
            }
          case _ => Future.successful(Redirect("Route to ineligible page"))
        }
      case _ => throw new RuntimeException("No TTP Data in sesson")
    }
  }
}