/*
 * Copyright 2020 HM Revenue & Customs
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

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class MonthlyPaymentAmountPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/monthly-payment-amount"

  def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How much can you afford to pay each month?"
    case Languages.Welsh   => "Faint y gallwch fforddio ei dalu bob mis?"
  }

  def assertPageIsDisplayedAltPath(difference: Int)(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText(difference).stripSpaces
  }

  def assertErrorPageIsDisplayed(implicit value: String): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def enterAmount(value: String): Unit = {
    val amountField = xpath("//*[@id=\"amount\"]")
    click on amountField
    enter(value)
  }

  def clickContinue(): Unit = {
    val button = xpath("//*[@id=\"monthlyPaymentForm\"]/div/button")
    click on button
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
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish(increase)
        case Welsh   => mainTextWelsh(increase)
      }

      private def format(value: Double) = value.formatted("%,1.2f")

      // "How much can you pay upfront in Pound Sterling" has css class visually hidden but is still read by content scraper
      private def mainTextEnglish(increase: Int) =
        s"""How much can you afford to pay each month?
           |Enter an amount between £${format(400.00 + increase)} and £${format(2400.00 + increase)}
           |£ How much can you pay monthly in Pound Sterling
           |Continue
        """.stripMargin

      private def mainTextWelsh(increase: Int) =
        s"""Faint y gallwch fforddio ei dalu bob mis?
           |Nodwch swm sydd rhwng £${format(400.00 + increase)} a £${format(2400.00 + increase)}
           |£ How much can you pay monthly in Pound Sterling
           |Yn eich blaen
        """.stripMargin
    }

    object ErrorText {
      def apply()(implicit value: String): String = errorText(value)

      private def errorText(value: String) =
        s"""There is a problem
           |Enter a figure between the given range
           |How much can you afford to pay each month?
           |Enter an amount between £400.00 and £2,400.00
           |Enter numbers only
           |£ How much can you pay monthly in Pound Sterling
           |Continue
        """.stripMargin
    }

  }

}
