/*
 * Copyright 2023 HM Revenue & Customs
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

package audit

import journey.Journey
import play.api.libs.json.Json
import play.api.mvc.Request
import ssttpcalculator.CalculatorService
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}

@Singleton
class DataEventFactory @Inject() (
                                 calculatorService: CalculatorService
                                 ) {
  def manualAffordabilityCheck(journey: Journey): ExtendedDataEvent = {
    val detail = Json.obj(
      "totalDebt" -> journey.debits.map(_.amount).sum,
      "spending" -> journey.maybeSpending.map(_.totalSpending).getOrElse(BigDecimal(0)),
      "income" -> journey.maybeIncome.map(_.totalIncome).getOrElse(BigDecimal(0)),
      "halfDisposableIncome" -> journey.remainingIncomeAfterSpending / 2,
      "status" -> notAffordableStatus(journey.remainingIncomeAfterSpending),
      "utr"  -> journey.taxpayer.selfAssessment.utr
    )
    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType = "ManualAffordabilityCheck",
      detail = detail
    )
  }

  private def notAffordableStatus(insufficientRemainingIncomeAfterSpending: BigDecimal): String = {
    if (insufficientRemainingIncomeAfterSpending < 0) "Negative Disposable Income"
    else if (insufficientRemainingIncomeAfterSpending == 0) "Zero Disposable Income"
    else "Total Tax Bill Income Greater than 24 Months"
  }
  def manualAffordabilityPlanSetUp(journey: Journey)(implicit request: Request[_]): ExtendedDataEvent = {
    val bankDetails = Json.obj(
      "accountNumber" -> journey.bankDetails.accountNumber,
      "name" -> journey.bankDetails.accountName,
      "sortCode" -> journey.bankDetails.sortCode
    )
    val detail = Json.obj(
      "bankDetails" -> bankDetails,
      "halfDisposableIncome" -> journey.remainingIncomeAfterSpending / 2,
      "selectionType" -> ???,
      "schedule" -> calculatorService.selectedSchedule(journey),
      "status" -> "Success",
      "paymentReference" -> journey.ddRef,
      "utr" -> journey.taxpayer.selfAssessment.utr
    )
    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType = "ManualAffordabilityPlanSetUp",
      detail = detail
    )
  }
}
