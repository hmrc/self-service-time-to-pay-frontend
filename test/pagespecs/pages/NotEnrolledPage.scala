/*
 * Copyright 2020 HM Revenue & Customs
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

class NotEnrolledPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/not-enrolled"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    ()
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Please call us"
    case Languages.Welsh => "Ffoniwch ni"
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
        """Please call us
          |You may still be able to set up a payment plan over the phone, but you are not eligible for an online payment plan.
          |
          |We recommend you speak to an adviser on 0300 200 3835 at the Business Support Service to talk about your payment options.
          |
          |Before you call, make sure you have:
          |information on any savings or investments you have
          |your bank details
          |We're likely to ask:
          |what you've done to try to pay the bill
          |if you can pay some of the bill now
          |Our opening times are:
          |Monday to Friday: 8am to 4pm"""
          .stripMargin

      private val mainTextWelsh =
        """Ffoniwch ni
          |Efallai y gallwch drefnu cynllun talu dros y ffôn, ond nid ydych yn gymwys i gael cynllun talu ar-lein.
          |
          |Rydym yn argymell eich bod yn siarad ag ymgynghorydd ar 0300 200 1900 yng Ngwasanaeth Cwsmeriaid Cymraeg CThEM i drafod eich opsiynau talu.
          |
          |Cyn i chi ffonio, sicrhewch fod gennych y canlynol:
          |gwybodaeth am unrhyw gynilion neu fuddsoddiadau sydd gennych
          |eich manylion banc
          |Rydym yn debygol o ofyn:
          |beth rydych wedi’i wneud i geisio talu’r bil
          |a allwch dalu rhywfaint o’r bil nawr
          |Ein horiau agor yw:
          |Dydd Llun i ddydd Gwener: 08:30 – 16:00
        """.stripMargin
    }

  }

}
