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
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class ViewPaymentPlanPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/view-payment-plan"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Your payment plan"
    case Languages.Welsh   => "Eich cynllun talu"
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
        """Your payment plan
          |Payment reference	123ABC123
          |Upfront payment amount
          |Monthly payments
          |Payments collected on
          |
          |December 2019
          |£500
          |January 2020
          |£500
          |February 2020
          |£500
          |March 2020
          |£500
          |April 2020
          |£500
          |May 2020
          |£500
          |June 2020
          |£500
          |July 2020
          |£500
          |August 2020
          |£500
          |September 2020
          |£473.08
          |Estimated total interest
          |Included in your plan
          |£73.08
          |Total to pay
          |£4,973.08
          |
          |Print a copy of your payment plan
        """.stripMargin
      private val mainTextWelsh =
        """Eich cynllun talu
          |Cyfeirnod y taliad	123ABC123
          |Taliadau misol
          |Rhagfyr 2019
          |£500
          |Ionawr 2020
          |£500
          |Chwefror 2020
          |£500
          |Mawrth 2020
          |£500
          |Ebrill 2020
          |£500
          |Mai 2020
          |£500
          |Mehefin 2020
          |£500
          |Gorffennaf 2020
          |£500
          |Awst 2020
          |£500
          |Medi 2020
          |£473.08
          |Amcangyfrif o gyfanswm y llog
          |£73.08
          |Yn gynwysedig yn eich cynllun
          |£4,973.08
          |Argraffwch gopi o’ch cynllun talu
        """.stripMargin
    }
  }
}
