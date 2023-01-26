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
import ssttpaffordability.model._
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
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed
    addIncomeSpendingPage.assertAddSpendingLinkIsDisplayed
  }

  def fillOutIncome(
      monthlyIncome: String = "",
      benefits:      String = "",
      otherIncome:   String = ""
  ): Unit = {
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.enterMonthlyIncome(monthlyIncome)
    yourMonthlyIncomePage.enterBenefits(benefits)
    yourMonthlyIncomePage.enterOtherIncome(otherIncome)

    yourMonthlyIncomePage.clickContinue()
  }

  "add income button goes to 'Your monthly income' page" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed

    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.assertPagePathCorrect
  }

  "displays income once filled out" - {
    List(English, Welsh).foreach{ lang =>
      implicit val language: Language = lang

      s"in $lang" - {
        "only monthly income filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val monthlyIncomeAmount = 2000
          fillOutIncome(monthlyIncome = monthlyIncomeAmount.toString)

          addIncomeSpendingPage.assertPathHeaderTitleCorrect
          addIncomeSpendingPage.assertIncomeTableDisplayed(MonthlyIncome(monthlyIncomeAmount))
          addIncomeSpendingPage.assertZeroIncomeCategoriesNotDisplayed(Benefits(), OtherIncome())
        }
        "only benefits filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val benefitsAmount = 700
          fillOutIncome(benefits = benefitsAmount.toString)

          addIncomeSpendingPage.assertPathHeaderTitleCorrect
          addIncomeSpendingPage.assertIncomeTableDisplayed(Benefits(benefitsAmount))
          addIncomeSpendingPage.assertZeroIncomeCategoriesNotDisplayed(MonthlyIncome(), OtherIncome())
        }
        "only other income filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val otherIncomeAmount = 1000
          fillOutIncome(otherIncome = otherIncomeAmount.toString)

          addIncomeSpendingPage.assertPathHeaderTitleCorrect
          addIncomeSpendingPage.assertIncomeTableDisplayed(OtherIncome(otherIncomeAmount))
          addIncomeSpendingPage.assertZeroIncomeCategoriesNotDisplayed(MonthlyIncome(), Benefits())
        }
        "all categories filled out" in {
          beginJourney()
          if (lang == Welsh) { addIncomeSpendingPage.clickOnWelshLink() }

          val monthlyIncomeAmount = 2000
          val benefitsAmount = 200
          val otherIncomeAmount = 1000
          fillOutIncome(monthlyIncomeAmount.toString, benefitsAmount.toString, otherIncomeAmount.toString)

          addIncomeSpendingPage.assertPathHeaderTitleCorrect
          addIncomeSpendingPage.assertIncomeTableDisplayed(
            MonthlyIncome(monthlyIncomeAmount),
            Benefits(benefitsAmount),
            OtherIncome(otherIncomeAmount)
          )
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
        fillOutIncome(monthlyIncome.toString)

        addIncomeSpendingPage.assertChangeIncomeLinkIsDisplayed(lang)
      }
    }
  }

  "displays spending once filled out" - {
    List(English, Welsh).foreach { lang =>
      implicit val language: Language = lang

      s"in $lang" - {
        "housing and pension contributions filled out" in {
          beginJourney()
          if (lang == Welsh) {
            addIncomeSpendingPage.clickOnWelshLink()
          }

          val housingAmount = 1000
          val pensionContributionsAmount = 500
          fillOutSpending(housingAmount.toString, pensionContributionsAmount.toString)

          addIncomeSpendingPage.assertPathHeaderTitleCorrect
          addIncomeSpendingPage.assertSpendingTableDisplayed(
            Housing(housingAmount),
            PensionContributions(pensionContributionsAmount)
          )
          addIncomeSpendingPage.assertZeroSpendingCategoriesNotDisplayed(CouncilTax(),
                                                                         Utilities(), DebtRepayments(), Travel(), Childcare(), Insurance(), Groceries(), Health())
        }
      }

    }
  }

  "displays 'Change spending' link once spending has been filled out" - {
    List(English, Welsh).foreach { lang =>
      implicit val language: Language = lang

      s"in $lang" in {
        beginJourney()
        if (lang == Welsh) {
          addIncomeSpendingPage.clickOnWelshLink()
        }

        val housing = 1000
        fillOutSpending(housing.toString)

        addIncomeSpendingPage.assertChangeSpendingLinkIsDisplayed(lang)
      }
    }
  }

  "add spending button goes to 'Your monthly spending' page" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed
    addIncomeSpendingPage.assertAddSpendingLinkIsDisplayed

    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertPagePathCorrect
  }

  def fillOutSpending(
      housing:              String = "",
      pensionContributions: String = "",
      councilTax:           String = ""
  ): Unit = {
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.enterHousing(housing)
    yourMonthlySpendingPage.enterPensionContributions(pensionContributions)
    yourMonthlySpendingPage.enterCouncilTax(councilTax)

    yourMonthlySpendingPage.clickContinue()
  }

  "language" in {
    beginJourney()

    addIncomeSpendingPage.assertPageIsDisplayed

    addIncomeSpendingPage.clickOnWelshLink()
    addIncomeSpendingPage.assertPageIsDisplayed(Welsh)
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

    addIncomeSpendingPage.clickOnEnglishLink()
    addIncomeSpendingPage.assertPageIsDisplayed(English)
    addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(English)

  }

  "back button" in {
    beginJourney()
    startAffordabilityPage.backButtonHref shouldBe Some(s"${baseUrl.value}${startAffordabilityPage.path}")
  }
}
