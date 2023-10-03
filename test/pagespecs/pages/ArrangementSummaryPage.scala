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
import org.scalatestplus.selenium.WebBrowser.{pageTitle, click, id}
import testsupport.RichMatchers._

class ArrangementSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/summary"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    href("survey-link") shouldBe Some("http://localhost:9514/feedback/PWYOII/personal")

    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)

  }

  def clickLink(): Unit = {
    val link = id("print")
    click on link
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Your payment plan is set up"
    case Languages.Welsh   => "Mae’ch cynllun talu wedi’i sefydlu"
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
        """Your payment plan is set up
          |Your payment reference is
          |123ABC123
          |What happens next
          |We will send a letter by post confirming the set up of your Direct Debit instruction within 10 working days.
          |
          |Your next payment will be taken on 28th December 2019 or the next working day.
          |
          |Your tax account will be updated with your payment plan within 24 hours.
          |
          |View your payment plan
          |If you need to change your payment plan
          |Call the HMRC Helpline on 0300 123 1813.
          |Go to tax account
          |What did you think of this service? (takes 30 seconds)
        """.stripMargin

      private val mainTextWelsh =
        """Mae’ch cynllun talu wedi’i sefydlu
          |Eich cyfeirnod talu yw
          |123ABC123
          |Yr hyn sy’n digwydd nesaf
          |Byddwn yn anfon llythyr atoch drwy’r post i gadarnhau bod eich cyfarwyddyd Debyd Uniongyrchol wedi’i sefydlu cyn pen 10 diwrnod gwaith.
          |
          |Caiff eich taliad nesaf ei gymryd ar 28ain Rhagfyr 2019 neu ar y diwrnod gwaith nesaf.
          |
          |Bydd eich cyfrif treth yn cael ei ddiweddaru gyda’ch cynllun talu cyn pen 24 awr.
          |
          |Bwrw golwg dros eich cynllun talu
          |
          |Os oes angen i chi newid eich cynllun talu
          |
          |Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900.
          |
          |Ewch i’r cyfrif treth
          |Beth oedd eich barn am y gwasanaeth hwn? (mae’n cymryd 30 eiliad)
        """.stripMargin
    }

  }

}
