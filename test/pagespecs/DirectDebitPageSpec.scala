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
import testsupport.{AccountName, ItSpec}
import testsupport._
import testsupport.stubs._
import testsupport.testdata.{DirectDebitTd, TdAll}

class DirectDebitPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.clickOnStartNowButton()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()
    monthlyPaymentAmountPage.enterAmount("2450")
    CalculatorStub.generateSchedule
    monthlyPaymentAmountPage.clickContinue()
    calculatorInstalmentsPage.selectAnOption()
    calculatorInstalmentsPage.clickContinue()
    instalmentSummarySelectDatePage.selectFirstOption()
    instalmentSummarySelectDatePage.clickContinue()
    instalmentSummaryPage.clickContinue()
  }

  "language" in {
    beginJourney()
    directDebitPage.assertPageIsDisplayed

    directDebitPage.clickOnWelshLink()
    directDebitPage.assertPageIsDisplayed(Welsh)

    directDebitPage.clickOnEnglishLink()
    directDebitPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    directDebitPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttparrangement.routes.ArrangementController.getInstalmentSummary()}")
  }

  "enter invalid Account Name" in {
    beginJourney()
    directDebitPage.fillOutForm("123ede23efr4efr4ew32ef3r4", DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(AccountName())
  }

  "enter invalid Sort Code " in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, "fqe23fwef322few23r", DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(SortCode())
  }

  "enter invalid Account Number" in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, "24wrgedfbgt423twergdfb")
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(AccountNumber())
  }

  "enter invalid bank account " in {
    beginJourney()
    directDebitPage.fillOutForm("Mr John Campbell", "12-34-56", "12345678")
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(InvalidBankDetails())

  }

  "enter valid bank account " in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBank(port, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksIsSuccessful
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertPageIsDisplayed
  }

  "enter valid bank account given business partner not found succeeds" in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBank(port, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksReturns404BPNotFound(TdAll.Sautr)
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertPageIsDisplayed
  }
}
