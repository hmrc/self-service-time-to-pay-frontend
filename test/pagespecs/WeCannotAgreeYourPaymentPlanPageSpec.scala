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
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._

class WeCannotAgreeYourPaymentPlanPageSpec extends ItSpec {

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
    selectDatePage.clickContinue()

    startAffordabilityPage.assertPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertPageIsDisplayed()
  }

  "when income is higher than spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("2000")
      addIncomeSpendingPage.enterSpending("1500")

      howMuchYouCouldAffordPage.assertPageIsDisplayed()

      howMuchYouCouldAffordPage.clickContinue()

      calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
    }
  }

  "when income is equal to spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("2000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.assertPageIsDisplayed()
    }
  }

  "when income is lower than spending" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("10")
      addIncomeSpendingPage.enterSpending("2")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.assertPageIsDisplayed(English)
    }
  }

  "when the payment plan is longer than 24 months" - {
    "language" in {
      beginJourney()

      addIncomeSpendingPage.assertPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("1000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.clickOnWelshLink()
      weCannotAgreeYourPaymentPlanPage.assertPageIsDisplayed(Welsh)
    }
  }

  "back button" in {
    beginJourney()
    addIncomeSpendingPage.assertPageIsDisplayed

    addIncomeSpendingPage.clickOnWelshLink()
    addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

    addIncomeSpendingPage.clickOnEnglishLink()
    addIncomeSpendingPage.assertPageIsDisplayed(English)

    addIncomeSpendingPage.enterIncome("1000")
    addIncomeSpendingPage.enterSpending("2000")

    howMuchYouCouldAffordPage.clickContinue()

    weCannotAgreeYourPaymentPlanPage.backButtonHref shouldBe Some(s"${baseUrl.value}${howMuchYouCouldAffordPage.path}")
  }
}
