/*
 * Copyright 2021 HM Revenue & Customs
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

class ArrangementSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/summary"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    href("survey-link") shouldBe Some("http://localhost:9514/feedback/PWYOII/personal")

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)

  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Your payment plan is set up"
    case Languages.Welsh   => "Cais yn llwyddiannus"
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
          |Your Direct Debit mandate reference:
          |123ABC123
          |Print your payment plan
          |What happens next?
          |HMRC will send you a letter within 5 days with your payment dates.
          |Your upfront payment will be taken within 7 working days. Your next payment will be taken on 28th December 2019.
          |If you need to change your payment plan
          |Call the HMRC Helpline on 0300 322 7015.
          |What did you think of this service? (takes 30 seconds)
        """.stripMargin

      private val mainTextWelsh =
        """Cais yn llwyddiannus
          |Cyfeirnod mandad Debyd Uniongyrchol:
          |123ABC123
          |Argraffu crynodeb o’ch taliadau
          |Yr hyn sy’n digwydd nesaf
          |Bydd CThEM yn anfon llythyr atoch cyn pen 5 diwrnod gyda’ch dyddiadau talu.
          |Caiff eich taliad ymlaen llaw ei gymryd cyn pen 7 diwrnod gwaith. Caiff eich taliad nesaf ei gymryd ar 28ain Rhagfyr 2019.
          |Os oes angen i chi newid eich cynllun talu
          |Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEM ar 0300 200 1900.
          |Beth oedd eich barn am y gwasanaeth hwn? (mae’n cymryd 30 eiliad)
        """.stripMargin
    }

  }

}
