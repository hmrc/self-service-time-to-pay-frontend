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

import testsupport.Language.{English, Welsh}
import testsupport.Language
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class PaymentTodayQuestionPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/payment-today-question"

  def assertInitialPageIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Upfront payment"
    case Language.Welsh   => "Taliad ymlaen llaw"
  }

  def selectRadioButton(yesOrNo: Boolean): Unit = {
    val yesRadioButton = xpath("//*[@id=\"paytoday-true\"]")
    val noRadioButton = xpath("//*[@id=\"paytoday-false\"]")

    if (yesOrNo) click on yesRadioButton
    else click on noRadioButton
  }

  def clickContinue(): Unit = probing {
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
        """Upfront payment
          |Your monthly payments will be lower if you can make an upfront payment. This payment will be taken from your bank account within 6 working days.
          |If you have savings, consider using them to make a payment now.
          |Can you make an upfront payment?
          |Yes
          |No
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Taliad ymlaen llaw
          |Bydd eich taliadau misol yn is os gallwch wneud taliad ymlaen llaw. Caiff y taliad hwn ei gymryd o’ch cyfrif banc cyn pen 6 diwrnod gwaith.
          |
          |Os oes gennych gynilion, dylech ystyried eu defnyddio i wneud taliad nawr.
          |A allwch wneud taliad ymlaen llaw?
          |Iawn
          |Na
          |Yn eich blaen
        """.stripMargin
    }

  }
}
