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

class FileYourTaxReturnPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/file-your-tax-return"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "File your Self Assessment tax return to use this service"
    case Languages.Welsh   => "Cyflwynwch eich Ffurflen Dreth Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn"
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
        """File your Self Assessment tax return to use this service
          |You must file your tax return before you can set up a Self Assessment payment plan online.
          |Call us on 0300 123 1813 if you need to speak to an adviser.
          |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
          |If you need extra support
          |Find out the different ways to deal with HMRC if you need some help.
          |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
          |If you are outside the UK: +44 2890 538 192.
        """
          .stripMargin

      private val mainTextWelsh =
        """Cyflwynwch eich Ffurflen Dreth Hunanasesiad er mwyn defnyddio’r gwasanaeth hwn
          |Mae’n rhaid i chi gyflwyno’ch Ffurflen Dreth cyn i chi allu trefnu cynllun talu ar gyfer Hunanasesiad ar-lein.
          |Ffoniwch ni ar 0300 200 1900 os oes angen i chi siarad ag ymgynghorydd.
          |Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc.
          |Os oes angen cymorth ychwanegol arnoch chi
          |Dysgwch am y ffyrdd gwahanol o ddelio â CThEF os oes angen help arnoch chi.
          |Gallwch hefyd ddefnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw.
          |Os ydych y tu allan i’r DU: +44 300 200 1900.
        """.stripMargin
    }

  }

}
