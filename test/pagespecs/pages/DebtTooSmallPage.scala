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

import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.Language
import testsupport.Language.{English, Welsh}
import testsupport.RichMatchers._

class DebtTooSmallPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/pay-self-assessment-tax-bill-in-full"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Pay your Self Assessment tax bill in full"
    case Language.Welsh   => "Talu’ch bil treth Hunanasesiad yn llawn"
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
        """Pay your Self Assessment tax bill in full
          |Make a payment online to cover your Self Assessment tax bill in full.
          |Call us on 0300 123 1813 if you are having difficulty making a payment online.
          |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
          |If you need extra support
          |Find out the different ways to deal with HMRC if you need some help.
          |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
          |If you are outside the UK: +44 2890 538 192.
          |Before you call, make sure you have:
          |your Self Assessment Unique Taxpayer Reference (UTR) which is 10 digits long, like 1234567890
          |your bank details
          |We’re likely to ask:
          |what you’ve done to try to pay the bill
          |if you can pay some of the bill now
        """
          .stripMargin

      private val mainTextWelsh =
        """Talu’ch bil treth Hunanasesiad yn llawn
          |Ni allwch drefnu cynllun talu Hunanasesiad ar-lein oherwydd bod eich bil yn rhy fach.
          |Gwnewch daliad ar-lein i dalu’ch bil Hunanasesiad yn llawn.
          |Ffoniwch ni ar 0300 200 1900 os oes angen i chi siarad ag ymgynghorydd.
          |Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc.
          |Os oes angen cymorth ychwanegol arnoch chi
          |Dysgwch am y ffyrdd gwahanol o ddelio â CThEF os oes angen help arnoch chi.
          |Gallwch hefyd ddefnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw.
          |Os ydych y tu allan i’r DU: +44 300 200 1900.
          |Cyn i chi ffonio, sicrhewch fod gennych y canlynol:
          |eich Cyfeirnod Unigryw y Trethdalwr (UTR) ar gyfer Hunanasesiad sy’n 10 digid o hyd, fel 1234567890
          |eich manylion banc
          |Rydym yn debygol o ofyn y canlynol:
          |beth rydych wedi’i wneud i geisio talu’r bil
          |a allwch dalu rhywfaint o’r bil nawr
        """.stripMargin
    }

  }

}
