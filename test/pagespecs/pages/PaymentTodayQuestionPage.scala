/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class PaymentTodayQuestionPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/payment-today-question"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
  }

  def selectRadioButton(yesOrNo: Boolean) =
    {
      val yesRadioButton = xpath("//*[@id=\"paytoday-true\"]")
      val noRadioButton = xpath("//*[@id=\"paytoday-false\"]")

      if (yesOrNo)
        click on yesRadioButton
      else
        click on noRadioButton
    }

  def clickContinue =
    {
      val button = xpath("//*[@id=\"next\"]")
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
          |Can you make an upfront payment?
          |Making an upfront payment before you set up your plan means your monthly payments will be lower.
          |Yes
          |No
          |Continue
          |Get help with this page.
        """.stripMargin

      private val mainTextWelsh =
        """Yn ôl
          |A allwch wneud taliad ymlaen llaw?
          |Bydd gwneud taliad ymlaen llaw cyn i chi drefnu’ch cynllun yn golygu y bydd eich taliadau misol yn is.
          |Iawn
          |Na
          |Yn eich blaen
          |Help gyda'r dudalen hon.
        """.stripMargin
    }

  }
}
