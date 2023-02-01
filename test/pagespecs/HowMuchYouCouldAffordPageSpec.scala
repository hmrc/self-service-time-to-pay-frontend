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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import ssttpaffordability.model.Expense._
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model._
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._

class HowMuchYouCouldAffordPageSpec extends ItSpec {

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

    selectDatePage.assertPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickOnTempButton()

    startAffordabilityPage.assertPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertPageIsDisplayed()
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed
    addIncomeSpendingPage.assertAddSpendingLinkIsDisplayed
  }

  def fillOutIncome(
      monthlyIncome: String = "",
  ): Unit = {
    addIncomeSpendingPage.clickOnAddChangeIncome()
    yourMonthlyIncomePage.enterMonthlyIncome(monthlyIncome)
    yourMonthlyIncomePage.clickContinue()
  }

  def fillOutSpending(
                       housing: String = "",
                     ): Unit = {
    addIncomeSpendingPage.clickOnAddChangeSpending()
    yourMonthlySpendingPage.enterHousing(housing)
    yourMonthlySpendingPage.clickContinue()
  }

  "page is displayed when both income and spending are filled in" in {
    beginJourney()

    val monthlyIncome = 2000
    val housing = 1000

    fillOutIncome(monthlyIncome.toString)
    fillOutSpending(housing.toString)

    howMuchYouCouldAffordPage.assertPagePathCorrect
  }

  "page is not displayed if only income is filled in" in {
    beginJourney()

    val monthlyIncome = 2000
    fillOutIncome(monthlyIncome.toString)

    addIncomeSpendingPage.assertPagePathCorrect
  }

  "page is not displayed if only spending is filled in" in {
    beginJourney()

    val housing = 1000
    fillOutSpending(housing.toString)

    addIncomeSpendingPage.assertPagePathCorrect
  }


  "language" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed

    addIncomeSpendingPage.clickOnWelshLink()
    addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

    addIncomeSpendingPage.clickOnEnglishLink()
    addIncomeSpendingPage.assertPageIsDisplayed(English)

    val monthlyIncome = 2000
    val housing = 1000

    fillOutIncome(monthlyIncome.toString)
    fillOutSpending(housing.toString)

    howMuchYouCouldAffordPage.assertPageIsDisplayed()

  }

  "back button" in {
    beginJourney()
    startAffordabilityPage.backButtonHref shouldBe Some(s"${baseUrl.value}${startAffordabilityPage.path}")
  }
}
