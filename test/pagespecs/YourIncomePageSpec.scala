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
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}

class YourIncomePageSpec extends ItSpec {

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
    addIncomeSpendingPage.clickOnAddIncome()
  }

  "add non-zero positive monthly income and press continue goes to add-income-spending-page" - {
    "without filling out benefits or other monthly income" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("00.01")
      yourMonthlyIncomePage.clickOnContinue()

      addIncomeSpendingPage.assertPageIsDisplayed()
    }
    "filling out non-zero positive benefits but not other monthly income" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("0.01")
      yourMonthlyIncomePage.enterBenefits("0.01")
      yourMonthlyIncomePage.clickOnContinue()

      addIncomeSpendingPage.assertPageIsDisplayed()
    }
    "filling out non-zero positive other monthly income but not benefits" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")
      yourMonthlyIncomePage.clickOnContinue()

      addIncomeSpendingPage.assertPageIsDisplayed()
    }
    "filling out both non-zero positive benefits and other monthly income" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("00.01")
      yourMonthlyIncomePage.enterBenefits("0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")
      yourMonthlyIncomePage.clickOnContinue()

      addIncomeSpendingPage.assertPageIsDisplayed()
    }
  }

  "add zero or negative monthly income and press continue stays on page" - {
    "- zero value for monthly income -" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterPrimaryIncome("0")
        yourMonthlyIncomePage.clickOnContinue()

        yourMonthlyIncomePage.assertPageIsDisplayed
        yourMonthlyIncomePage.assertErrorIsDisplayed
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterPrimaryIncome("0")
        yourMonthlyIncomePage.clickOnContinue()

        yourMonthlyIncomePage.assertPageIsDisplayed
        yourMonthlyIncomePage.assertErrorIsDisplayed
        yourMonthlyIncomePage.assertPrimaryIncomeValueIsDisplayed("0")
      }
    }

  }
  "- negative value for monthly income -" - {
    "displays error message" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("-0.01")
      yourMonthlyIncomePage.clickOnContinue()

      yourMonthlyIncomePage.assertPageIsDisplayed
      yourMonthlyIncomePage.assertErrorIsDisplayed
    }
    "retains values entered" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterPrimaryIncome("-0.01")
      yourMonthlyIncomePage.enterBenefits("0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")

      yourMonthlyIncomePage.clickOnContinue()

      yourMonthlyIncomePage.assertPageIsDisplayed
      yourMonthlyIncomePage.assertErrorIsDisplayed
      yourMonthlyIncomePage.assertPrimaryIncomeValueIsDisplayed("-0.01")
      yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
      yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")

    }
  }
  "not adding monthly income and press continue stays on page" - {
    "and displays error message" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.clickOnContinue()

      yourMonthlyIncomePage.assertPageIsDisplayed
      yourMonthlyIncomePage.assertErrorIsDisplayed
    }
    "retains other values entered" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterBenefits("0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")
      yourMonthlyIncomePage.clickOnContinue()

      yourMonthlyIncomePage.assertPageIsDisplayed
      yourMonthlyIncomePage.assertErrorIsDisplayed
      yourMonthlyIncomePage.assertPrimaryIncomeValueIsDisplayed("")
      yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
      yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
    }
  }

  "language" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed

    addIncomeSpendingPage.clickOnWelshLink()
    addIncomeSpendingPage.assertPageIsDisplayed(Welsh)

    addIncomeSpendingPage.clickOnEnglishLink()
    addIncomeSpendingPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    startAffordabilityPage.backButtonHref shouldBe Some(s"${baseUrl.value}${startAffordabilityPage.path}")
  }
}
