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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser.tagName
import testsupport.WireMockSupport
import testsupport.RichMatchers._

class DebtTooLargePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/debt-large/call-us"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing{
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish =
        """GOV.UK
          |Set up a payment plan
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu
        """.stripMargin
    }

    object MainText {

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Back
          |Please call us
          |To be eligible to set up a payment plan online the tax you owe must be £10,000 or less.
          |For further support you can contact the Business Support Service and speak to an adviser on 0300 200 3835.
          |Before you call, make sure you have:
          |information on any savings or investments you have
          |your bank details
          |We're likely to ask:
          |what you've done to try to pay the bill
          |if you can pay some of the bill now
          |Our opening times are:
          |Monday to Friday: 8am to 8pm
          |Saturday and Sunday: 8am to 4pm"""
          .stripMargin

      private val mainTextWelsh =
        """
          |Yn ôl
          |Ffoniwch ni
          |I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid i’r dreth sydd arnoch fod yn £10,000 neu lai.
          |
          |Am gymorth pellach, gallwch gysylltu â’r Gwasanaeth Cymorth Busnes a siarad ag ymgynghorydd ar 0300 200 1900.
          |
          |Cyn i chi ffonio, sicrhewch fod gennych y canlynol:
          |gwybodaeth am unrhyw gynilion neu fuddsoddiadau sydd gennych
          |eich manylion banc
          |Rydym yn debygol o ofyn:
          |beth rydych wedi’i wneud i geisio talu’r bil
          |a allwch dalu rhywfaint o’r bil nawr
          |Ein horiau agor yw:
          |Dydd Llun i ddydd Gwener: 08:30 – 17:00
        """.stripMargin
    }

  }

}