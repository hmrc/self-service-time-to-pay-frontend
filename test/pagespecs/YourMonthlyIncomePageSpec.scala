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

class YourMonthlyIncomePageSpec extends ItSpec {

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
    addIncomeSpendingPage.clickOnAddChangeIncome()
  }

  "add non-zero positive income input and press continue goes to add-income-spending-page" - {
    "adding monthly income" - {
      "without filling out benefits or other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
      "and filling out non-zero positive benefits but not other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
      "and filling out non-zero positive other income but not benefits" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
      "and filling out both non-zero positive benefits and other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("00.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
    }
    "adding benefit" - {
      "without filling out monthly income or other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
      "and filling out non-zero positive other income but not monthly income" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
    }
    "adding other income" - {
      "without filling out monthly income or benefits" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertPageIsDisplayed()
      }
    }
  }

  "add non-numeric inputs and press continue stays on page" - {
    "non-numeric monthly income" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed
      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("word")
      }

    }
    "non-numeric benefits" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed
      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("word")
      }
    }
    "non-numeric other income" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed

      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("word")
      }
    }

  }
  // TODO: OPS-9325 Add validation for negative values
  //  "- negative value for monthly income -" - {
  //    "displays error message" in {
  //      beginJourney()
  //
  //      yourMonthlyIncomePage.assertPageIsDisplayed
  //
  //      yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
  //      yourMonthlyIncomePage.clickContinue()
  //
  //      yourMonthlyIncomePage.assertErrorIsDisplayed
  //    }
  //    "retains values entered" in {
  //      beginJourney()
  //
  //      yourMonthlyIncomePage.assertPageIsDisplayed
  //
  //      yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
  //      yourMonthlyIncomePage.enterBenefits("0.01")
  //      yourMonthlyIncomePage.enterOtherIncome("0.01")
  //
  //      yourMonthlyIncomePage.clickContinue()
  //
  //      yourMonthlyIncomePage.assertErrorIsDisplayed
  //      yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("-0.01")
  //      yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
  //      yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
  //    }
  //  }
  "inputs not adding up to positive income and press continue stays on page" - {
    "and displays error message" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
      yourMonthlyIncomePage.enterBenefits("-0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")

      yourMonthlyIncomePage.clickContinue()

      yourMonthlyIncomePage.assertErrorIsDisplayed
    }
    "retains values entered" in {
      beginJourney()

      yourMonthlyIncomePage.assertPageIsDisplayed

      yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
      yourMonthlyIncomePage.enterBenefits("-0.01")
      yourMonthlyIncomePage.enterOtherIncome("0.01")
      yourMonthlyIncomePage.clickContinue()

      yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("-0.01")
      yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("-0.01")
      yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
    }
  }

  "language" in {
    beginJourney()

    yourMonthlyIncomePage.assertPageIsDisplayed

    yourMonthlyIncomePage.clickOnWelshLink()
    yourMonthlyIncomePage.assertPageIsDisplayed(Welsh)

    yourMonthlyIncomePage.clickOnEnglishLink()
    yourMonthlyIncomePage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    yourMonthlyIncomePage.backButtonHref shouldBe Some(s"${baseUrl.value}${addIncomeSpendingPage.path}")
  }
}