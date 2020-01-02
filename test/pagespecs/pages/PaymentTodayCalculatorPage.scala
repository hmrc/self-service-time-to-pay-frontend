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

import langswitch.Languages.{English, Welsh}
import langswitch.{Language, Languages}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import testsupport.RichMatchers._
import org.scalatest.selenium.WebBrowser

class PaymentTodayCalculatorPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/payment-today"

  override def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def assertErrorIsDisplayed: Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.TextError().stripSpaces()
  }

  def enterAmount(value: String) =
    {
      val amount = xpath("//*[@id=\"amount\"]")
      click on amount
      enter(value)
    }

  def clickContinue() =
    {
      val button = xpath("//*[@id=\"content\"]/article/form/div/button")
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

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }
      private val mainTextEnglish =
        """Back
          |How much can you pay upfront?
          |£
          |Continue
          |Get help with this page.
    """.stripMargin

      private val mainTextWelsh =
        """Yn ôl
          |Faint y gallwch ei dalu ymlaen llaw?
          |£
          |Yn eich blaen
          |Help gyda'r dudalen hon.
        """.stripMargin
    }

    object TextError {
      def apply(): String = mainErrorEnglish
      private val mainErrorEnglish =
        """Back
          |Something you've entered isn't valid
          |You need to enter an amount less than the amount you owe
          |How much can you pay upfront?
          |Enter the amount you want to pay upfront
          |£
          |Continue
          |Get help with this page.
        """.stripMargin
    }
  }
}
