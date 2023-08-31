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
import pagespecs.pages.{CheckYourPaymentPlanPage, HowMuchCanYouPayEachMonthPage, SetUpPlanWithAdviserPage}
import ssttpcalculator.CalculatorType.PaymentOptimised
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

class SetUpPlanWithAdviserPageSpec extends ItSpec {

  val pageUnderTest: SetUpPlanWithAdviserPage = setUpPlanWithAdviserPage
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPage
  override val frozenTimeString: String = "2023-06-09T00:00:00.880"

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayerNddsRejects()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.enterMonthlyIncome("2000")
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.enterHousing("1700")
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    inUseHowMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    inUseHowMuchCanYouPayEachMonthPage.clickContinue()
  }

  "language" in {
    beginJourney()

    //      pageUnderTest.clickOnWelshLink()
    //      pageUnderTest.assertInitialPageIsDisplayed(Welsh)
    //
    //      pageUnderTest.clickOnEnglishLink()

    pageUnderTest.assertInitialPageIsDisplayed(English)
  }
}
