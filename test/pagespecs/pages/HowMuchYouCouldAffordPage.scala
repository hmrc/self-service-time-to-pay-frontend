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
import ssttpaffordability.model.Expense._
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model._
import testsupport.RichMatchers._

import java.text.DecimalFormat

class HowMuchYouCouldAffordPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/how-much-you-could-afford"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPagePathCorrect: Assertion = probing {
    readPath() shouldBe path
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How much you could afford"
    case Languages.Welsh   => "Faint y gallech ei fforddio"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  def clickOnAddChangeIncome(): Unit = {
    click on id("monthly-income")
  }

  def clickOnAddChangeSpending(): Unit = {
    click on id("monthly-spending")
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""How much you could afford
           |1.Income
           |Monthly income after tax	£2,000
           |Total income	£2,000
           |
           |2.Spending
           |Housing	£1,000
           |Total spending	£1,000
           |
           |3.Left over income
           |Total left over income	£1,000
           |Half of left over income	£500
      """.stripMargin

      private val mainTextWelsh =
        s"""Faint y gallech ei fforddio
           |1.Incwm
           |Incwm misol ar ôl treth	£2,000
           |Cyfanswm eich incwm	£2,000
           |
           |2.Gwariant
           |Tai	£1,000
           |Cyfanswm y gwariant	£1,000
           |
           |3.Incwm sydd dros ben
           |Cyfanswm yr incwm sydd dros ben	£1,000
           |Hanner yr incwm sydd dros ben	£500
      """.stripMargin
    }
  }
}
