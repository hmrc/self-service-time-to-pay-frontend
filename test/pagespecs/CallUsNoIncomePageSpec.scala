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

import testsupport.Language.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.{GgStub, TaxpayerStub}
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful

class CallUsNoIncomePageSpec extends ItSpec {
  def beginJourney(): Unit = {
    TaxpayerStub.getTaxpayer()
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertInitialPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertInitialPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.assertInitialPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    startAffordabilityPage.assertInitialPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertInitialPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.clickContinue()

  }
  "'Go back to add your income' link returns to 'your monthly income' page" in {
    beginJourney()
    callUsNoIncomePage.clickOnBackToIncomeLink()
    yourMonthlyIncomePage.assertInitialPageIsDisplayed
  }
  "'deal with HMRC if you need some help' link goes to 'Get help from HMRC if you need extra support' page" in {
    beginJourney()
    callUsNoIncomePage.extraSupportLink shouldBe Some("https://www.gov.uk/get-help-hmrc-extra-support")
  }
  "'Relay UK' link goes to Relay UK homepage" in {
    beginJourney()
    callUsNoIncomePage.relayUKLink shouldBe Some("https://www.relayuk.bt.com/")
  }
  "language" in {
    beginJourney()

    callUsNoIncomePage.assertInitialPageIsDisplayed

    callUsNoIncomePage.clickOnWelshLink()
    callUsNoIncomePage.assertInitialPageIsDisplayed(Welsh)

    callUsNoIncomePage.clickOnEnglishLink()
    callUsNoIncomePage.assertInitialPageIsDisplayed(English)
  }

}
