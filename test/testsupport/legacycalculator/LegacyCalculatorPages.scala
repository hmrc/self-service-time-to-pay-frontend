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

package testsupport.legacycalculator

import com.softwaremill.macwire.wire
import pagespecs.pages.legacycalculator.{
  CheckYourPaymentPlanPageForPaymentDay11thOfMonthLegacyCalculator,
  CheckYourPaymentPlanPageForPaymentDay28thOfMonthLegacyCalculator,
  HowMuchCanYouPayEachMonthPageLegCalc,
  ViewsPaymentPlanPageLegacyCalculator
}
import testsupport.ItSpec

trait LegacyCalculatorPages extends ItSpec {
  lazy val howMuchCanYouPayEachMonthPageLegacyCalculator: HowMuchCanYouPayEachMonthPageLegCalc = wire[HowMuchCanYouPayEachMonthPageLegCalc]
  lazy val checkYourPaymentPlanPageLegacyCalculator: CheckYourPaymentPlanPageForPaymentDay28thOfMonthLegacyCalculator =
    wire[CheckYourPaymentPlanPageForPaymentDay28thOfMonthLegacyCalculator]
  lazy val checkYourPaymentPlanPageForPaymentDay11thOfMonthLegacyCalculator: CheckYourPaymentPlanPageForPaymentDay11thOfMonthLegacyCalculator =
    wire[CheckYourPaymentPlanPageForPaymentDay11thOfMonthLegacyCalculator]
  lazy val viewPaymentPlanPageLegacyCalculator: ViewsPaymentPlanPageLegacyCalculator = wire[ViewsPaymentPlanPageLegacyCalculator]

}
