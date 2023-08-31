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
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers._

class SetUpPlanWithAdviserPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/set-up-payment-plan-adviser"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Set up a payment plan with an adviser"
    case Languages.Welsh   => "Set up a payment plan with an adviser"
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
        """The payment plan you have selected needs to be set up with an adviser.
          |
          |You can go back to check your income and spending.
          |
          |Connect to an adviser
          |Use our webchat to set up a payment plan with an adviser online.
          |
          |You can also call us on 0300 123 1813 to set up a payment plan over the phone.
          |
          |Keep this page open when you connect to an adviser.
          |
          |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
          |
          |If you need extra support
          |Find out the different ways to deal with HMRC if you need some help.
          |
          |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
          |
          |If you are outside the UK: +44 2890 538 192.
        """.stripMargin

      private val mainTextWelsh =
        """The payment plan you have selected needs to be set up with an adviser.
          |
          |You can go back to check your income and spending.
          |
          |Connect to an adviser
          |Use our webchat to set up a payment plan with an adviser online.
          |
          |You can also call us on 0300 123 1813 to set up a payment plan over the phone.
          |
          |Keep this page open when you connect to an adviser.
          |
          |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
          |
          |If you need extra support
          |Find out the different ways to deal with HMRC if you need some help.
          |
          |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
          |
          |If you are outside the UK: +44 2890 538 192.
        """.stripMargin
    }

  }

}
