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
import model.enumsforforms.{IsSoleSignatory, TypesOfBankAccount}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.DirectDebitTd
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class ViewPaymentPlanPageSpec extends ItSpec {

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement

    startPage.open()
    startPage.assertPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.assertPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    startAffordabilityPage.assertPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.assertPageIsDisplayed
    yourMonthlyIncomePage.enterMonthlyIncome(remainingIncomeAfterSpending.toString)
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertPageIsDisplayed
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed
    calculatorInstalmentsPage28thDay.selectASpecificOption("50")
    calculatorInstalmentsPage28thDay.clickContinue()

    checkYourPaymentPlanPage.assertPageIsDisplayed()
    checkYourPaymentPlanPage.clickContinue()

    aboutBankAccountPage.assertPageIsDisplayed()
    aboutBankAccountPage.selectTypeOfAccountRadioButton(TypesOfBankAccount.Personal)
    aboutBankAccountPage.selectIsAccountHolderRadioButton(IsSoleSignatory.Yes)
    aboutBankAccountPage.clickContinue()

    directDebitPage.assertPageIsDisplayed()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksIsSuccessful()
    directDebitPage.clickContinue()

    directDebitConfirmationPage.assertPageIsDisplayed()
    directDebitConfirmationPage.clickContinue()
  }

  "language English" in {
    beginJourney()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.clickLink()
    viewPaymentPlanPage.assertPageIsDisplayed(English)
  }

  "language Welsh" in {
    beginJourney()
    termsAndConditionsPage.clickOnWelshLink()
    termsAndConditionsPage.clickContinue()
    arrangementSummaryPage.clickLink()
    viewPaymentPlanPage.assertPageIsDisplayed(Welsh)
  }

  "back button" in {
    beginJourney()
    viewPaymentPlanPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttpdirectdebit.routes.DirectDebitController.getDirectDebitConfirmation()}")
  }
}
