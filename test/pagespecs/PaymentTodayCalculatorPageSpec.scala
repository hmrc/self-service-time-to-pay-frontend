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
import testsupport.stubs.{AuthStub, EligibilityStub, GgStub, TaxpayerStub}

class PaymentTodayCalculatorPageSpec extends ItSpec {
  def beginJourney() = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    EligibilityStub.eligible()
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.clickOnStartNowButton
    paymentTodayQuestionPage.selectRadioButton(true)
    paymentTodayQuestionPage.clickContinue
  }

  "language" in
    {
      beginJourney
      paymentTodayCalculatorPage.assertPageIsDisplayed

      paymentTodayCalculatorPage.clickOnWelshLink()
      paymentTodayCalculatorPage.assertPageIsDisplayed(Welsh)

      paymentTodayCalculatorPage.clickOnEnglishLink()
      paymentTodayCalculatorPage.assertPageIsDisplayed(English)
    }

  "not valid amount" in
    {
      beginJourney
      paymentTodayCalculatorPage.enterAmount("99999")
      paymentTodayCalculatorPage.clickContinue
      paymentTodayCalculatorPage.assertErrorIsDisplayed
    }

  "valid amount and continue" in
    {
      beginJourney
      paymentTodayCalculatorPage.enterAmount("123")
      paymentTodayCalculatorPage.clickContinue
      paymentSummaryPage.assertPageIsDisplayed
    }

}
