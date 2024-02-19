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
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._

class HowMuchYouCouldAffordPageSpec extends ItSpec {

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
  }

  "page is displayed when both income and spending are filled in" in {
    beginJourney()

    addIncomeSpendingPage.enterIncome("2000")
    addIncomeSpendingPage.enterSpending("1000")

    howMuchYouCouldAffordPage.assertPagePathCorrect
  }

  "page is not displayed if only income is filled in" in {
    beginJourney()

    addIncomeSpendingPage.enterIncome("2000")

    addIncomeSpendingPage.assertPagePathCorrect
  }

  "page is not displayed if only spending is filled in" in {
    beginJourney()

    addIncomeSpendingPage.enterSpending("1000")

    addIncomeSpendingPage.assertPagePathCorrect
  }

  "when income is higher than spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("2000")
      addIncomeSpendingPage.enterSpending("1000")

      howMuchYouCouldAffordPage.assertInitialPageIsDisplayed()
    }
  }

  "when income is equal to spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("2000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.assertZeroIncomeParagraphIsDisplayed(English)
      howMuchYouCouldAffordPage.clickOnWelshLink()
      howMuchYouCouldAffordPage.assertZeroIncomeParagraphIsDisplayed(Welsh)
    }
  }

  "when income is lower than spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("1000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.assertNegativeIncomeParagraphIsDisplayed(English)
      howMuchYouCouldAffordPage.clickOnWelshLink()
      howMuchYouCouldAffordPage.assertNegativeIncomeParagraphIsDisplayed(Welsh)
    }
  }

}
