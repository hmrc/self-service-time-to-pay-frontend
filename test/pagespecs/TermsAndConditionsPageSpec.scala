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
import testsupport.testdata.DirectDebitTd

class TermsAndConditionsPageSpec extends ItSpec {

  def beginJourney(): Unit =
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
      calculatorInstalmentsPage.selectAnOption()
      calculatorInstalmentsPage.clickContinue()
      instalmentSummarySelectDatePage.selectFirstOption()
      instalmentSummarySelectDatePage.clickContinue()
      instalmentSummaryPage.clickContinue()
      directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
      DirectDebitStub.getBank(port, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
      DirectDebitStub.getBanksIsSuccessful
      directDebitPage.clickContinue()
      directDebitConfirmationPage.assertPageIsDisplayed
      directDebitConfirmationPage.clickContinue()
    }

  "language" in
    {
      beginJourney()
      termsAndConditionsPage.assertPageIsDisplayed

      termsAndConditionsPage.clickOnWelshLink()
      termsAndConditionsPage.assertPageIsDisplayed(Welsh)

      termsAndConditionsPage.clickOnEnglishLink()
      termsAndConditionsPage.assertPageIsDisplayed(English)
    }

  "click continue" in
    {
      beginJourney()
      DirectDebitStub.postPaymentPlan
      ArrangementStub.postTtpArrangement
      termsAndConditionsPage.clickContinue()
      arrangementSummaryPage.assertPageIsDisplayed

    }
}
