/*
 * Copyright 2022 HM Revenue & Customs
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

  def assertPageIsDisplayed(implicit lang: Language): Unit = assertPageIsDisplayedAltPath(410, 2450)



  def assertPageIsDisplayedAltPath(lowerAmount: Int, upperAmount: Int)(implicit lang: Language = English): Unit = probing {
    readPath() should startWith(path)
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val expectedLines = Expected.MainText(lowerAmount, upperAmount).stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertErrorPageIsDisplayed(): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def enterAmount(value: String): Unit = {
    val amountField = xpath("//*[@id=\"amount\"]")
    click on amountField
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
      def apply(lowerAmount: Int, upperAmount: Int)(implicit language: Language): String = language match {
        case English => mainTextEnglish(lowerAmount, upperAmount)
        case Welsh   => mainTextWelsh(lowerAmount, upperAmount)
      }

      private def format(value: Double) = value.formatted("%,1.2f")

      // "How much can you pay upfront in Pound Sterling" has css class visually hidden but is still read by content scraper
      private def mainTextEnglish(lowerAmount: Int, upperAmount: Int) =
        s"""Enter an amount between £${format(lowerAmount)} and £${format(upperAmount)}
           |How much can you pay monthly in Pound Sterling
           |£
           |Continue
        """.stripMargin

      private def mainTextWelsh(lowerAmount: Int, upperAmount: Int) =
        s"""Faint y gallwch fforddio ei dalu bob mis?
           |Nodwch swm sydd rhwng £${format(lowerAmount)} a £${format(upperAmount)}
           |How much can you pay monthly in Pound Sterling
           |£
           |Yn eich blaen
        """.stripMargin
    }

    object ErrorText {
      def apply(): String =
        s"""There is a problem
           |Enter a figure between the given range
           |Enter an amount between £410.00 and £2,450.00
           |How much can you pay monthly in Pound Sterling
           |Error: Enter a figure between the given range
           |£
           |Continue
        """.stripMargin

    }

  }

}
