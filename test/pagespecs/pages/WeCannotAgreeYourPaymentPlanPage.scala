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
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class WeCannotAgreeYourPaymentPlanPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/we-cannot-agree-your-payment-plan"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPagePathCorrect: Assertion = probing {
    readPath() shouldBe path
  }

  def assertPathHeaderTitleCorrect(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "We cannot agree your payment plan online"
    case Languages.Welsh   => "Ni allwn gytuno ar eich cynllun talu ar-lein"
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
        s"""We cannot agree your payment plan online
           |The amounts you have entered for your income and spending mean you cannot set up a payment plan online.
           |
           |Have you entered the information correctly? You can change your income and spending.
           |
           |You may be able to set up a payment plan over the phone. Call us on 0300 123 1813 to discuss your options.
           |
           |Our opening times are Monday to Friday, 8am to 6pm. We are closed on weekends and bank holidays.
           |
           |If you need extra support
           |Find out the different ways to deal with HMRC if you need some help.
           |
           |You can also use Relay UK if you cannot hear or speak on the phone: dial 18001 then 0345 300 3900.
           |
           |If you are outside the UK: +44 2890 538 192
      """.stripMargin

      private val mainTextWelsh =
        s"""Ni allwn gytuno ar eich cynllun talu ar-lein
           |Mae’r symiau rydych wedi’u nodi ar gyfer eich incwm a’ch gwariant yn golygu na allwch sefydlu cynllun talu ar-lein.
           |
           |A ydych wedi nodi’r wybodaeth yn gywir? Gallwch newid eich incwm a’ch gwariant.
           |
           |Mae’n bosibl y byddwch yn gallu sefydlu cynllun talu dros y ffôn. Ffoniwch y Gwasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900 i drafod eich opsiynau.
           |
           |Ein horiau agor yw Dydd Llun i Ddydd Gwener, 08:30 i 17:00. Rydym ar gau ar benwythnosau a gwyliau banc.
           |
           |Os oes angen cymorth ychwanegol arnoch chi
           |Dysgwch am y ffyrdd gwahanol o ddelio â CThEF os oes angen help arnoch chi.
           |
           |Gallwch hefyd ddefnyddio Relay UK os na allwch glywed na siarad dros y ffôn: deialwch 18001 ac yna 0345 300 3900.
           |
           |Os ydych y tu allan i’r DU: +44 300 200 1900.
      """.stripMargin
    }

  }
}
