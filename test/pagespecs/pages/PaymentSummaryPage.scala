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
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class PaymentSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/payment-summary"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def clickContinue() = {
    val button = xpath("//*[@id=\"content\"]/article/a[2]")
    click on button
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a payment plan"""
      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Back
          |Payment summary
          |Upfront payment
          |Taken in 3 to 5 days' time
          |£123.0
          |Change Upfront payment
          |Remaining amount to pay
          |£4777.0
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Yn ôl
          |Crynodeb o’r taliadau
          |Taliad ymlaen llaw
          |Wedi’i gymryd ymhen 3 i 5 diwrnod
          |£123.0
          |Newid Taliad ymlaen llaw
          |Swm sy’n weddill i’w dalu
          |£4777.0
          |Yn eich blaen
        """.stripMargin
    }

  }

}
