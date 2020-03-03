/*
 * Copyright 2020 HM Revenue & Customs
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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs._

class CalculatorInstalmentsPageSpec extends ItSpec {

  def beginJourney() =
    {
      AuthStub.authorise()
      TaxpayerStub.getTaxpayer()
      IaStub.successfulIaCheck
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      taxLiabilitiesPage.clickOnStartNowButton()
      paymentTodayQuestionPage.selectRadioButton(false)
      paymentTodayQuestionPage.clickContinue()
      monthlyPaymentAmountPage.enterAmout("2450")
      CalculatorStub.generateSchedule
      monthlyPaymentAmountPage.clickContinue()
    }

  "language" in
    {
      beginJourney()

      calculatorInstalmentsPage.assertPageIsDisplayed

      calculatorInstalmentsPage.clickOnWelshLink()
      calculatorInstalmentsPage.assertPageIsDisplayed(Welsh)

      calculatorInstalmentsPage.clickOnEnglishLink()
      calculatorInstalmentsPage.assertPageIsDisplayed(English)
    }

  "select an option and continue" in
    {
      beginJourney()
      calculatorInstalmentsPage.selectAnOption()
      calculatorInstalmentsPage.clickContinue()
      instalmentSummarySelectDatePage.assertPageIsDisplayed
    }
}
