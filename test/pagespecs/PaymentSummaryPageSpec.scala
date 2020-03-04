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
import testsupport.stubs.{AuthStub, IaStub, GgStub, TaxpayerStub}

class PaymentSummaryPageSpec extends ItSpec {

  def beginJourney()() =
    {
      AuthStub.authorise()
      TaxpayerStub.getTaxpayer()
      IaStub.successfulIaCheck
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      taxLiabilitiesPage.clickOnStartNowButton()
      paymentTodayQuestionPage.selectRadioButton(true)
      paymentTodayQuestionPage.clickContinue()
      paymentTodayCalculatorPage.enterAmount("123")
      paymentTodayCalculatorPage.clickContinue()
    }

  "language" in
    {
      beginJourney()
      paymentSummaryPage.assertPageIsDisplayed

      paymentSummaryPage.clickOnWelshLink()
      paymentSummaryPage.assertPageIsDisplayed(Welsh)

      paymentSummaryPage.clickOnEnglishLink()
      paymentSummaryPage.assertPageIsDisplayed(English)
    }

  "continue to monthly payment amount" in
    {
      beginJourney()
      paymentSummaryPage.assertPageIsDisplayed

      paymentSummaryPage.clickContinue()
      monthlyPaymentAmountPage.assertPageIsDisplayedAltPath(-100: Int)
    }

}
