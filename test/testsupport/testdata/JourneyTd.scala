/*
 * Copyright 2019 HM Revenue & Customs
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

package testsupport.testdata

import journey.{Journey, JourneyId}
import testsupport.testdata.CalculatorTd.{calculatorInput, calculatorPaymentSchedule}
import testsupport.testdata.DirectDebitTd._
import testsupport.testdata.TdAll._
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, NoDebt}

object JourneyTd {

  val eligibilityStatus = EligibilityStatus(
    eligible = true,
    reasons  = List(NoDebt)
  )

  val journeyId = new JourneyId("12345A")

  val journey = new Journey(
    _id                    = journeyId,
    maybeAmount            = Some(200: BigDecimal),
    schedule               = Some(calculatorPaymentSchedule),
    bankDetails            = Some(bankDetails),
    existingDDBanks        = Some(directDebitBank),
    maybeTaxpayer          = Some(taxpayer),
    maybeCalculatorData    = Some(calculatorInput),
    durationMonths         = 2: Int,
    maybeEligibilityStatus = Some(eligibilityStatus),
    debitDate              = Some("2019-04-05"),
    ddRef                  = Some("1234567A"))
}
