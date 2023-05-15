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
import testsupport.RichMatchers._

class DirectDebitErrorPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/direct-debit-error"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Call us about a payment plan"
    case Languages.Welsh   => "Ffoniwch ni ynghylch cynllun talu"
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
        """Call us about a payment plan
          |We’re having problems submitting your Direct Debit instruction.
          |Call us on 0300 123 1813 as you may be able to set up a plan over the phone.
          |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
          |If you need extra support
          |Find out the different ways to deal with HMRC if you need some help.
          |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
          |If you are outside the UK: +44 2890 538 192
          """
          .stripMargin

      private val mainTextWelsh =
        """Ffoniwch ni ynghylch cynllun talu
          |Rydym yn cael anawsterau wrth gyflwyno’ch cyfarwyddyd Debyd Uniongyrchol.
          |Ffoniwch ni ar 0300 200 1900 oherwydd mae’n bosibl y gallwch drefnu cynllun dros y ffôn.
          |Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc.
          |Os oes angen cymorth ychwanegol arnoch chi
          |Dysgwch am y ffyrdd gwahanol o ddelio â CThEF os oes angen help arnoch chi.
          |Gallwch hefyd ddefnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw.
          |Gallwch hefyd ddefnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw.
          |Os ydych y tu allan i’r DU: +44 300 200 1900
        """.stripMargin
    }

  }

}
