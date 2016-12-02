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
            calculatorConnector.calculatePaymentSchedule(CalculatorInput(ttpData.manualDebits))
            Ok
          case _ => Redirect("Route to ineligible page")
        }
      case _ => throw new RuntimeException("No TTP Data in sesson")
    }
  }
}