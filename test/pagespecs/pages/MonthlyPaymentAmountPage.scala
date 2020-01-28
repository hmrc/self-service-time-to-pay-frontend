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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class MonthlyPaymentAmountPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/monthly-payment-amount"

  def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
  }

  def assertPageIsDisplayedAltPath(difference: Int)(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText(difference).stripSpaces
  }

  def assertErrorPageIsDisplayed(implicit value: String): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def enterAmout(value: String) =
    {
      val amountField = xpath("//*[@id=\"amount\"]")
      click on amountField
      enter(value)
    }

  def clickContinue() =
    {
      val button = xpath("//*[@id=\"monthlyPaymentForm\"]/div/button")
      click on button
    }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish =
        """GOV.UK
          |Set up a payment plan
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu
        """.stripMargin
    }

    object MainText {
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish(increase)
        case Welsh   => mainTextWelsh(increase)
      }

      private def mainTextEnglish(increase: Int) =
        s"""Back
          |How much can you afford to pay each month?
          |Enter an amount between £${600 + increase} and £${2500 + increase}
          |£
          |Continue
        """.stripMargin

      private def mainTextWelsh(increase: Int) =
        s"""Yn ôl
          |Faint y gallwch fforddio ei dalu bob mis?
          |Nodwch swm sydd rhwng £${600 + increase}. a £${2500 + increase}
          |£
          |Yn eich blaen
        """.stripMargin
    }

    object ErrorText {
      def apply()(implicit value: String): String = errorText(value)

      private def errorText(value: String) =
        s"""Back
           |Something you've entered isn't valid
           |Enter a figure between the given range
           |How much can you afford to pay each month?
           |Enter an amount between £600 and £2500
           |£ Enter a figure between the given range 1
           |Continue
        """.stripMargin
    }
  }
}
