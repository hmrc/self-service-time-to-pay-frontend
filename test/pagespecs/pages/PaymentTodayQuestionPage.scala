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
import testsupport.RichMatchers._

class PaymentTodayQuestionPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override val path: String = "/calculator/payment-today-question"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish =
        """GOV.UK
          |Set up a payment plan Sign-out
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu Allgofnodi
        """.stripMargin
    }

    object MainText {

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """BETA	This is a new service – your feedback will help us to improve it. English | Cymraeg
          |Back
          |Can you make an upfront payment?
          |Making an upfront payment before you set up your plan means your
          |monthly payments will be lower.
          |Yes No
          |Continue
          |Get help with this page.
        """.stripMargin

      private val mainTextWelsh =
        """BETA	Mae hwn yn wasanaeth newydd – bydd eich adborth yn ein helpu i'w wella. English | Cymraeg
          |Yn ôl
          |A allwch wneud taliad ymlaen llaw?
          |Bydd gwneud taliad ymlaen llaw cyn i chi drefnu’ch cynllun yn golygu y
          |bydd eich taliadau misol yn is.
          |Iawn Na
          |Yn eich blaen
          |Help gyda'r dudalen hon.
        """.stripMargin
    }

  }

  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }
}
