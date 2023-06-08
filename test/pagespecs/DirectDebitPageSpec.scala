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
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import ssttpcalculator.CalculatorType.PaymentOptimised
import model.enumsforforms.{IsSoleSignatory, TypesOfBankAccount}
import ssttpcalculator.model.PaymentPlanOption
import testsupport.stubs._
import testsupport.testdata.DirectDebitTd
import testsupport.testdata.TdAll.{defaultRemainingIncomeAfterSpending, saUtr}
import testsupport.{AccountName, ItSpec, _}

class DirectDebitPageSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> PaymentOptimised.value
  )

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement

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

    yourMonthlyIncomePage.assertInitialPageIsDisplayed
    yourMonthlyIncomePage.enterMonthlyIncome(remainingIncomeAfterSpending.toString)
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertInitialPageIsDisplayed
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    howMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    howMuchCanYouPayEachMonthPage.clickContinue()

    checkYourPaymentPlanPage.assertInitialPageIsDisplayed()
    checkYourPaymentPlanPage.clickContinue()

    aboutBankAccountPage.assertInitialPageIsDisplayed()
    aboutBankAccountPage.selectTypeOfAccountRadioButton(TypesOfBankAccount.Personal)
    aboutBankAccountPage.selectIsAccountHolderRadioButton(IsSoleSignatory.Yes)
    aboutBankAccountPage.clickContinue()
  }

  "language" in {
    beginJourney()

    directDebitPage.clickOnWelshLink()
    directDebitPage.assertInitialPageIsDisplayed(Welsh)

    directDebitPage.clickOnEnglishLink()
    directDebitPage.assertInitialPageIsDisplayed(English)
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
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
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
    BarsStub.validateBankFail(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(InvalidBankDetails())
  }

  "enter invalid Sort Code - SortCodeOnDenyList " in {
    beginJourney()
    directDebitPage.fillOutForm("Mr John Campbell", "201147", "12345678")
    BarsStub.validateBankFailSortCodeOnDenyList(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(SortCodeOnDenyList())
  }

  "enter account not supporting direct debits " - {
    "in English" in {
      beginJourney()
      directDebitPage.fillOutForm("Mr John Campbell", "12-34-56", "12345678")
      BarsStub.validateBankDDNotSupported(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
      directDebitPage.clickContinue()
      directDebitPage.assertErrorPageIsDisplayed(DirectDebitNotSupported())
    }
    "in Welsh" in {
      beginJourney()
      directDebitPage.clickOnWelshLink()
      directDebitPage.fillOutForm("Mr John Campbell", "12-34-56", "12345678")
      BarsStub.validateBankDDNotSupported(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
      directDebitPage.clickContinue()
      directDebitPage.assertErrorPageIsDisplayed(DirectDebitNotSupported(), Welsh)
    }


  }

  "enter valid bank account " in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksIsSuccessful()
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertInitialPageIsDisplayed()
  }

  "enter valid bank account given business partner not found succeeds" in {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksBPNotFound(saUtr)
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertInitialPageIsDisplayed()
  }
}
