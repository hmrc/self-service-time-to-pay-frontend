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
    case Languages.Welsh   => "Trefnu cynllun talu ag ymgynghorydd"
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
        """|Mae’n angen i’r cynllun talu rydych wedi’i ddewis gael ei drefnu ag ymgynghorydd.
           |
           |Gallwch fynd yn ôl i wirio’ch incwm a’ch gwariant.
           |
           |Cysylltu ag ymgynghorydd
           |Ffoniwch ni ar 0300 200 1900 i sefydlu cynllun talu dros y ffôn.
           |
           |Mae ein llinellau ffôn ar agor o 08:30 tan 17:00, ddydd Llun i ddydd Gwener. Rydym yn ar gau ar benwythnosau a gwyliau banc.
           |
           |Gallwch hefyd ddefnyddio ein gwasanaeth sgwrsio dros y we i drefnu cynllun talu ar-lein ag ymgynghorydd. Mae’r gwasanaeth i sgwrsio dros y we ar gael yn Saesneg yn unig.
           |
           |Mae’r gwasanaeth i sgwrsio dros y we ar gael o ddydd Llun i ddydd Gwener, o 08:00 i 18:00. Mae ar gau ar benwythnosau a gwyliau banc.
           |
           |Cadwch y dudalen hon ar agor pan fyddwch wedi cysylltu ag ymgynghorydd.
           |
           |Os oes angen cymorth ychwanegol arnoch chi
           |Dysgwch am y ffyrdd gwahanol o ddelio â CThEF os oes angen help arnoch chi.
           |
           |Gallwch hefyd defnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900. Sylwer – dim ond galwadau ffôn Saesneg eu hiaith y mae Relay UK yn gallu ymdrin â nhw.
           |
           |Os ydych y tu allan i’r DU: +44 2890 538 192.
        """.stripMargin
    }

  }

}
