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

package pagespecs.legacycalculator

import pagespecs.CheckYourPaymentPlanPageBaseSpec
import pagespecs.pages.{CheckYourPaymentPlanPage, HowMuchCanYouPayEachMonthPage}
import ssttpcalculator.CalculatorType.Legacy
import testsupport.legacycalculator.LegacyCalculatorPages

class CheckYourPaymentPlanPageLegacyCalculatorSpec extends CheckYourPaymentPlanPageBaseSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  val pageUnderTest: CheckYourPaymentPlanPage = checkYourPaymentPlanPageLegacyCalculator
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPageLegacyCalculator
}