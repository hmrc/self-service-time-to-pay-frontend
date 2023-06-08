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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}

class PaymentTodayCalculatorPageSpec extends ItSpec {
  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertInitialPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertInitialPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(true)
    paymentTodayQuestionPage.clickContinue()
    paymentTodayCalculatorPage.assertInitialPageIsDisplayed()
  }

  "language" in {
    beginJourney()
    paymentTodayCalculatorPage.assertInitialPageIsDisplayed

    paymentTodayCalculatorPage.clickOnWelshLink()
    paymentTodayCalculatorPage.assertInitialPageIsDisplayed(Welsh)

    paymentTodayCalculatorPage.clickOnEnglishLink()
    paymentTodayCalculatorPage.assertInitialPageIsDisplayed(English)
  }

  "not valid amount" in {
    beginJourney()
    paymentTodayCalculatorPage.enterAmount("99999")
    paymentTodayCalculatorPage.clickContinue()
    paymentTodayCalculatorPage.assertErrorIsDisplayed
  }

  "not correctly formatted amount" in {
    beginJourney()
    paymentTodayCalculatorPage.enterAmount("ab -3")
    paymentTodayCalculatorPage.clickContinue()
    paymentTodayCalculatorPage.assertFormatErrorIsDisplayed
  }

  "valid amount and continue" in {
    beginJourney()
    paymentTodayCalculatorPage.enterAmount("123")
    paymentTodayCalculatorPage.clickContinue()
    paymentSummaryPage.assertInitialPageIsDisplayed
  }
}
