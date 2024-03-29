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
import pagespecs.pages.{HowMuchCanYouPayEachMonthPage, SetUpPlanWithAdviserPage}
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{DateCalculatorStub, GgStub, TaxpayerStub}

import java.time.LocalDate

class SetUpPlanWithAdviserPageSpec extends ItSpec {

  val pageUnderTest: SetUpPlanWithAdviserPage = setUpPlanWithAdviserPage
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPage
  override val frozenTimeString: String = "2023-06-09T00:00:00.880"

  def beginJourney(): Unit = {
    TaxpayerStub.getTaxpayerNddsRejects()
    GgStub.signInPage(port)
    DateCalculatorStub.stubAddWorkingDays(LocalDate.now().plusDays(10))
    getBanksIsSuccessful()

    startPage.open()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.path shouldBe "/pay-what-you-owe-in-instalments/calculator/tax-liabilities"
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.path shouldBe "/pay-what-you-owe-in-instalments/calculator/payment-today-question"
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.path shouldBe "/pay-what-you-owe-in-instalments/arrangement/instalment-summary/select-date"
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    startAffordabilityPage.path shouldBe "/pay-what-you-owe-in-instalments/start-affordability"
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.path shouldBe "/pay-what-you-owe-in-instalments/add-income-spending"
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.path shouldBe "/pay-what-you-owe-in-instalments/monthly-income"
    yourMonthlyIncomePage.enterMonthlyIncome("2000")
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.path shouldBe "/pay-what-you-owe-in-instalments/add-income-spending"
    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.path shouldBe "/pay-what-you-owe-in-instalments/monthly-spending"
    yourMonthlySpendingPage.enterHousing("1700")
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.path shouldBe "/pay-what-you-owe-in-instalments/how-much-you-could-afford"
    howMuchYouCouldAffordPage.clickContinue()
    inUseHowMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    inUseHowMuchCanYouPayEachMonthPage.clickContinue()
  }

  "language" in {
    beginJourney()

    pageUnderTest.clickOnWelshLink()
    pageUnderTest.assertInitialPageIsDisplayed(Welsh)
    pageUnderTest.clickOnEnglishLink()
    pageUnderTest.assertInitialPageIsDisplayed(English)
  }
}
