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

package pagespecs.pages

import langswitch.Languages.{English, Welsh}
import langswitch.{Language, Languages}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class AddIncomeSpendingPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/add-income-spending"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Add your income and spending"
    case Languages.Welsh   => "Ychwanegu eich incwm a’ch gwariant"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def clickOnAddIncome(): Unit = {
    click on id("monthly-income")
  }

  def clickOnAddSpending(): Unit = {
    click on id("monthly-spending")
  }

  def assertIncomeFilled(
                          monthlyIncome: BigDecimal,
                          benefits: BigDecimal,
                          otherIncome: BigDecimal
                        )(implicit lang: Language): Unit = probing {
    assertPathHeaderTitleCorrect
    val expectedLines = Expected.IncomeFilledText(
      monthlyIncome,
      benefits,
      otherIncome
    ).stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Add your income and spending
           |Add income
           |1.Income
           |Add spending
           |2.Spending
      """.stripMargin

      private val mainTextWelsh =
        s"""Ychwanegu eich incwm a’ch gwariant
           |Ychwanegu incwm
           |1.Incwm
           |Ychwanegu gwariant
           |2.Gwariant
      """.stripMargin
    }

    object IncomeFilledText {
      def apply(
                 monthlyIncome: BigDecimal,
                 benefits: BigDecimal,
                 otherIncome: BigDecimal
               )(implicit language: Language): String = {
        val totalIncome = (monthlyIncome + benefits + otherIncome).toString()
        language match {
          case English => incomeFilledTextEnglish(totalIncome)
          case Welsh => incomeFilledTextWelsh(totalIncome)
        }
      }

      private def incomeFilledTextEnglish(totalIncome: String) =
        s"""Add your income and spending
           |Add income
           |1.Income
           |Total income £$totalIncome
           |Add spending
           |2.Spending
      """.stripMargin

      private def incomeFilledTextWelsh(totalIncome: String) =
        s"""Ychwanegu eich incwm a’ch gwariant
           |Ychwanegu incwm
           |1.Incwm
           |Cyfanswm eich incwm £$totalIncome
           |Ychwanegu gwariant
           |2.Gwariant
      """.stripMargin
    }
  }
}
