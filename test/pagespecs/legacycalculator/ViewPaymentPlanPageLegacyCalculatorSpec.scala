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

import langswitch.Languages.{English, Welsh}
import model.enumsforforms.{IsSoleSignatory, TypesOfBankAccount}
import pagespecs.ViewPaymentPlanPageBaseSpec
import pagespecs.pages.{CheckYourPaymentPlanPage, HowMuchCanYouPayEachMonthPage, ViewPaymentPlanPage}
import ssttpcalculator.CalculatorType.Legacy
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.legacycalculator.LegacyCalculatorPages
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.DirectDebitTd
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class ViewPaymentPlanPageLegacyCalculatorSpec extends ViewPaymentPlanPageBaseSpec with LegacyCalculatorPages {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  val pageUnderTest: ViewPaymentPlanPage = viewPaymentPlanPageLegacyCalculator
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPageLegacyCalculator
  val inUseCheckYourPaymentPlanPage: CheckYourPaymentPlanPage = checkYourPaymentPlanPageLegacyCalculator

}
