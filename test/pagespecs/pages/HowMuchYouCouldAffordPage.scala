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

class HowMuchYouCouldAffordPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/how-much-you-could-afford"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertZeroIncomeParagraphIsDisplayed(implicit lang: Language): Unit = probing {
    val zeroIncomeParagraph = Expected.ZeroIncomeParagraph().splitIntoLines()
    assertContentMatchesExpectedLines(zeroIncomeParagraph)
  }

  def assertNegativeIncomeParagraphIsDisplayed(implicit lang: Language): Unit = probing {
    val negativeIncomeParagraph = Expected.NegativeIncomeParagraph().splitIntoLines()
    assertContentMatchesExpectedLines(negativeIncomeParagraph)
  }

  def assertPagePathCorrect: Assertion = probing {
    readPath() shouldBe path
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
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

    object ZeroIncomeParagraph {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Your spending is the same as your income
            |As your spending is the same as your income, call us on 0300 123 1813 to discuss your debt. You may also want to seek independent debt advice.
      """.stripMargin

      private val mainTextWelsh =
        s"""Mae’ch gwariant yr un faint a’ch incwm
            |Gan fod eich gwariant yr un faint a’ch incwm, cysylltwch â Gwasanaeth Cwsmeriaid Cymraeg CThEF drwy ffonio 0300 200 1900 i drafod eich dyled. Mae’n bosibl yr hoffech geisio cyngor annibynnol ar ddyledion.
      """.stripMargin
    }

    object NegativeIncomeParagraph {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Your spending is higher than your income
            |As your spending is higher than your income, call us on 0300 123 1813 to discuss your debt. You may also want to seek independent debt advice.
      """.stripMargin

      private val mainTextWelsh =
        s"""Mae’ch gwariant yn fwy na’ch incwm
            |Gan fod eich gwariant yn fwy na’ch incwm, cysylltwch â Gwasanaeth Cwsmeriaid Cymraeg CThEF drwy ffonio 0300 200 1900 i drafod eich dyled. Mae’n bosibl yr hoffech geisio cyngor annibynnol ar ddyledion.
      """.stripMargin
    }
  }
}
