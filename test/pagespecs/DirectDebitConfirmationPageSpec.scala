/*
 * Copyright 2021 HM Revenue & Customs
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
import testsupport.stubs._
import testsupport.testdata.{CalculatorDataGenerator, DirectDebitTd}

class DirectDebitConfirmationPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.assertPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    monthlyPaymentAmountPage.assertPageIsDisplayed()
    monthlyPaymentAmountPage.enterAmount("2000")
    monthlyPaymentAmountPage.clickContinue()

    selectDatePage.assertPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
    calculatorInstalmentsPage28thDay.selectAnOption()
    calculatorInstalmentsPage28thDay.clickContinue()

    instalmentSummaryPage.assertPageIsDisplayed()
    instalmentSummaryPage.clickContinue()

    directDebitPage.assertPageIsDisplayed()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()

    directDebitConfirmationPage.assertPageIsDisplayed()
  }

  "language" in {
    beginJourney()
    directDebitConfirmationPage.assertPageIsDisplayed

    directDebitConfirmationPage.clickOnWelshLink()
    directDebitConfirmationPage.assertPageIsDisplayed(Welsh)

    directDebitConfirmationPage.clickOnEnglishLink()
    directDebitConfirmationPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    directDebitConfirmationPage.backButtonHref mustBe Some(s"${baseUrl.value}${ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()}")
  }

  "change sort code" in {
    beginJourney()
    directDebitConfirmationPage.clickChangeButton()
    directDebitPage.assertPageIsDisplayed
  }

  "click continue" in {
    beginJourney()
    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement
    directDebitConfirmationPage.clickContinue()
    termsAndConditionsPage.assertPageIsDisplayed
  }
}
