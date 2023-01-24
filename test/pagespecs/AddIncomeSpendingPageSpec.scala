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

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._

class AddIncomeSpendingPageSpec extends ItSpec {

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
    addIncomeSpendingPage.assertAddLinkIsDisplayed("income")
    addIncomeSpendingPage.assertAddLinkIsDisplayed("spending")

  }

  def fillOutIncome(
      monthlyIncome: BigDecimal = 0,
      benefits:      BigDecimal = 0,
      otherIncome:   BigDecimal = 0
  ): Unit = {
    addIncomeSpendingPage.clickOnAddIncome()

    yourMonthlyIncomePage.enterMonthlyIncome(monthlyIncome.toString())
    yourMonthlyIncomePage.enterBenefits(benefits.toString())
    yourMonthlyIncomePage.enterOtherIncome(otherIncome.toString())

    yourMonthlyIncomePage.clickContinue()
  }

  "add income button goes to 'Your monthly income' page" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed
    addIncomeSpendingPage.assertAddLinkIsDisplayed("income")
    addIncomeSpendingPage.assertAddLinkIsDisplayed("spending")

    addIncomeSpendingPage.clickOnAddIncome()

    yourMonthlyIncomePage.assertPagePathCorrect
  }

  "displays income once filled out" - {
    List(English, Welsh).foreach{ lang =>
      implicit val language: Language = lang

      s"in $lang" - {
        "only monthly income filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val monthlyIncome = 2000
          fillOutIncome(monthlyIncome = monthlyIncome)

          addIncomeSpendingPage.assertIncomeFilled(monthlyIncome = monthlyIncome)
          addIncomeSpendingPage.assertCategoriesNotShown(benefits    = true, otherIncome = true)
        }
        "only benefits filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val benefits = 700
          fillOutIncome(benefits = benefits)

          addIncomeSpendingPage.assertIncomeFilled(benefits = benefits)
          addIncomeSpendingPage.assertCategoriesNotShown(monthlyIncome = true, otherIncome = true)
        }
        "only other income filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val otherIncome = 1000
          fillOutIncome(otherIncome = otherIncome)

          addIncomeSpendingPage.assertIncomeFilled(otherIncome = otherIncome)
          addIncomeSpendingPage.assertCategoriesNotShown(monthlyIncome = true, benefits = true)

        }
        "all categories filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val monthlyIncome = 2000
          val benefits = 200
          val otherIncome = 1000
          fillOutIncome(monthlyIncome, benefits, otherIncome)

          addIncomeSpendingPage.assertIncomeFilled(monthlyIncome, benefits, otherIncome)
        }
      }

    }
  }
  "displays 'Change income' link once income has been filled out" - {
    List(English, Welsh).foreach { lang =>
      implicit val language: Language = lang

      s"in $lang" in {
        beginJourney()
        if (lang == Welsh) {
          addIncomeSpendingPage.clickOnWelshLink()
        }

        val monthlyIncome = 2000
        fillOutIncome(monthlyIncome)

        addIncomeSpendingPage.assertChangeLinkIsDisplayed("income")(lang)
      }
    }
  }

  "language" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed

    addIncomeSpendingPage.clickOnWelshLink()
    addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
    addIncomeSpendingPage.assertAddLinkIsDisplayed("income")(Welsh)
    addIncomeSpendingPage.assertAddLinkIsDisplayed("spending")(Welsh)


    addIncomeSpendingPage.clickOnEnglishLink()
    addIncomeSpendingPage.assertPageIsDisplayed(English)
    addIncomeSpendingPage.assertAddLinkIsDisplayed("income")(English)
    addIncomeSpendingPage.assertAddLinkIsDisplayed("spending")(English)

  }

  "back button" in {
    beginJourney()
    startAffordabilityPage.backButtonHref shouldBe Some(s"${baseUrl.value}${startAffordabilityPage.path}")
  }
}
