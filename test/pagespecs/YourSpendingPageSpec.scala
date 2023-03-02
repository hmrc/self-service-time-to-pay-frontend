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

class YourSpendingPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
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
    addIncomeSpendingPage.clickOnAddChangeSpending()
  }

  "add non-zero positive spending input and press continue goes to add-income-spending-page" - {
    "adding housing" - {
      "without filling out any other field" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("0.01")
        yourMonthlySpendingPage.clickContinue()

        addIncomeSpendingPage.assertInitialPageIsDisplayed()
      }
    }
  }

  "add non-numeric input and press continue stays on page" - {
    "non-numeric housing spending" - {
      "displays error message" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("nonNumerals")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertNonNumericErrorIsDisplayed
      }
      "retains value entered" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("nonNumerals")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertHousingValueIsDisplayed("nonNumerals")
      }

    }
  }

  "add negative input and press continue stays on page" - {
    "negative housing spending" - {
      "displays error message" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("-10")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertNegativeNumberErrorIsDisplayed
      }
      "retains value entered" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("-10")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertHousingValueIsDisplayed("-10")
      }

    }
  }

  "add too many decimals and press continue stays on page" - {
    "too many decimals in housing spending" - {
      "displays error message" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("10.123")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertTooManyDecimalsErrorIsDisplayed
      }
      "retains value entered" in {
        beginJourney()
        yourMonthlySpendingPage.assertInitialPageIsDisplayed
        yourMonthlySpendingPage.enterHousing("10.123")
        yourMonthlySpendingPage.clickContinue()
        yourMonthlySpendingPage.assertHousingValueIsDisplayed("10.123")
      }

    }
  }

  "language" in {
    beginJourney()
    yourMonthlySpendingPage.assertInitialPageIsDisplayed
    yourMonthlySpendingPage.clickOnWelshLink()
    yourMonthlySpendingPage.assertInitialPageIsDisplayed(Welsh)
    yourMonthlySpendingPage.clickOnEnglishLink()
    yourMonthlySpendingPage.assertInitialPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    yourMonthlySpendingPage.backButtonHref shouldBe Some(s"${baseUrl.value}${addIncomeSpendingPage.path}")
  }
}
