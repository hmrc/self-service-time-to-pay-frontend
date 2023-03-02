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

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers._

class NeedToFilePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/you-need-to-file"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "File your return to use this service"
    case Languages.Welsh   => "Cyflwynwch eich Ffurflen Dreth er mwyn defnyddio’r gwasanaeth hwn"
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
        """File your return to use this service
          |To be eligible to set up a payment plan online you need to have filed your Self Assessment tax return. Once you have done this, you can return to the service.
          |
          |Go to your tax account to file your Self Assessment tax return.
          |
          |For further support, you can contact us on 0300 123 1813 to speak to an adviser.
          |
          |If you cannot use speech recognition software
          |Our opening times are:
          |Monday to Friday: 8am to 6pm
        """
          .stripMargin

      private val mainTextWelsh =
        """Cyflwynwch eich Ffurflen Dreth er mwyn defnyddio’r gwasanaeth hwn
          |I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid i chi fod wedi cyflwyno’ch Ffurflen Dreth Hunanasesiad. Pan fyddwch wedi gwneud hyn, gallwch ddychwelyd i’r gwasanaeth.
          |
          |Ewch i’ch cyfrif treth er mwyn cyflwyno’ch Ffurflen Dreth Hunanasesiad.
          |
          |I gael cymorth pellach, gallwch gysylltu â Gwasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900 i siarad ag ymgynghorydd.
          |
          |Os na allwch ddefnyddio meddalwedd adnabod lleferydd
          |Ein horiau agor yw:
          |Dydd Llun i ddydd Gwener: 08:30 i 17:00 (rydym ar gau ar benwythnosau a gwyliau banc)
        """.stripMargin
    }

  }

}
