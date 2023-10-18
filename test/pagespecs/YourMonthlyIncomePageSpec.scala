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
import org.scalatestplus.selenium.Chrome.goBack
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs.{AuthStub, GgStub, TaxpayerStub}

class YourMonthlyIncomePageSpec extends ItSpec {

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
    addIncomeSpendingPage.clickOnAddChangeIncome()
  }

  "add non-zero positive income input and press continue goes to add-income-spending-page" - {
    "adding monthly income" - {
      "without filling out benefits or other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
      "and filling out non-zero positive benefits but not other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
      "and filling out non-zero positive other income but not benefits" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
      "and filling out both non-zero positive benefits and other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("00.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
    }
    "adding benefit" - {
      "without filling out monthly income or other income" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
      "and filling out non-zero positive other income but not monthly income" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
    }
    "adding other income" - {
      "without filling out monthly income or benefits" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("00.01")
        yourMonthlyIncomePage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
    }
  }

  "add non-numeric inputs and press continue stays on page" - {
    "non-numeric monthly income" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed(MonthlyIncome)
      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("word")
      }

    }
    "non-numeric benefits" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed(Benefits)
      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("word")
      }
    }
    "non-numeric other income" - {
      "displays error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNonNumeralErrorIsDisplayed(OtherIncome)

      }
      "retains value entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("word")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("word")
      }
    }

  }
  "Negative value for on any field displays error message" - {
    "monthly income" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(MonthlyIncome)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("-0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(MonthlyIncome)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("-0.01")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
      }
    }
    "benefits" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("-0.01")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(Benefits)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("-0.01")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(Benefits)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("-0.01")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
      }
    }
    "other income" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("-0.01")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(OtherIncome)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("-0.01")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertNegativeValueErrorIsDisplayed(OtherIncome)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("-0.01")
      }
    }
  }
  "More than two decimal places on any field displays error message" - {
    "monthly income" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.0111")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(MonthlyIncome)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterMonthlyIncome("0.011")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(MonthlyIncome)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0.011")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
      }
    }
    "benefits" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("0.011")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(Benefits)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterBenefits("0.011")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterOtherIncome("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(Benefits)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.011")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.01")
      }
    }
    "other income" - {
      "error message" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("0.011")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")
        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(OtherIncome)
      }
      "retains values entered" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed

        yourMonthlyIncomePage.enterOtherIncome("0.011")
        yourMonthlyIncomePage.enterMonthlyIncome("0.01")
        yourMonthlyIncomePage.enterBenefits("0.01")

        yourMonthlyIncomePage.clickContinue()

        yourMonthlyIncomePage.assertMoreThanTwoDecimalPlacesErrorIsDisplayed(OtherIncome)
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0.01")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0.011")
      }
    }
  }
  "No income and press continue goes to 'Call us about a payment plan' page" - {
    "fields left empty" in {
      beginJourney()

      yourMonthlyIncomePage.assertInitialPageIsDisplayed
      yourMonthlyIncomePage.clickContinue()

      callUsNoIncomePage.assertPagePathCorrect
    }
    "zero inputs" in {
      beginJourney()

      yourMonthlyIncomePage.assertInitialPageIsDisplayed

      yourMonthlyIncomePage.enterMonthlyIncome("0")
      yourMonthlyIncomePage.enterBenefits("0")
      yourMonthlyIncomePage.enterOtherIncome("0")

      yourMonthlyIncomePage.clickContinue()

      callUsNoIncomePage.assertPagePathCorrect
    }
    "writes zero values which display when returning to page" - {
      "via back link" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed
        yourMonthlyIncomePage.clickContinue()

        callUsNoIncomePage.clickOnBackLink()
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0")

      }
      "via text hyperlink" in {
        beginJourney()

        yourMonthlyIncomePage.assertInitialPageIsDisplayed
        yourMonthlyIncomePage.clickContinue()

        callUsNoIncomePage.clickOnBackToIncomeLink()
        yourMonthlyIncomePage.assertMonthlyIncomeValueIsDisplayed("0")
        yourMonthlyIncomePage.assertBenefitsValueIsDisplayed("0")
        yourMonthlyIncomePage.assertOtherIncomeValueIsDisplayed("0")

      }
    }
  }

  "language" in {
    beginJourney()

    yourMonthlyIncomePage.assertInitialPageIsDisplayed

    yourMonthlyIncomePage.clickOnWelshLink()
    yourMonthlyIncomePage.assertInitialPageIsDisplayed(Welsh)

    yourMonthlyIncomePage.clickOnEnglishLink()
    yourMonthlyIncomePage.assertInitialPageIsDisplayed(English)
  }

}
