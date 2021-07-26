/*
 * Copyright 2021 HM Revenue & Customs
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
import testsupport.RichMatchers._
import org.scalatestplus.selenium.WebBrowser

class PaymentTodayCalculatorPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/payment-today"

  override def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How much can you pay upfront?"
    case Languages.Welsh   => "Faint y gallwch ei dalu ymlaen llaw?"
  }

  def assertErrorIsDisplayed: Assertion = probing {
    readPath() shouldBe path

    println("actual")
    println(readMain().stripSpaces)
    println("expected")
    println(Expected.TextError())

    readMain().stripSpaces shouldBe Expected.TextError().stripSpaces()
  }

  def enterAmount(value: String): Unit = {
    val amount = xpath("//*[@id=\"amount\"]")
    click on amount
    enter(value)
  }

  def clickContinue(): Unit = {
    clickOnContinue()
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
        """How much can you pay upfront?
          |£
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Faint y gallwch ei dalu ymlaen llaw?
          |£
          |Yn eich blaen
        """.stripMargin
    }

    object TextError {
      def apply(): String =
        """There is a problem
          |You need to enter an amount less than the amount you owe
          |How much can you pay upfront?
          |How much can you pay upfront in Pound Sterling
          |Error: You need to enter an amount less than the amount you owe
          |£
          |Continue
        """.stripMargin
    }
  }
}
