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
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class StartPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Set up a Self Assessment payment plan"
    case Languages.Welsh   => "Trefnu cynllun talu"
  }

  def expectedTitleStartPageOnly(heading: String, lang: Language): String = lang match {
    case Languages.English => s"Set up a Self Assessment payment plan - GOV.UK"
    case Languages.Welsh   => s"Trefnu cynllun talu - GOV.UK"
  }

  def assertInitialPageIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    readPath() shouldBe path
    pageTitle shouldBe expectedTitleStartPageOnly(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def clickOnStartNowButton(): Unit = {
    //    val button = xpath("//*[@id=\"start\"]/div/button")
    val button = id("start-now")
    click on button
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
        """Set up a Self Assessment payment plan
          |A payment plan allows you to pay your tax charges in instalments over a period of time.
          |
          |Your plan covers the tax you owe and, if applicable, the 2 advance payments towards your tax bill. It also covers any penalties or charges against your account. You’ll have to pay interest on the amount you pay late.
          |
          |To be eligible to set up an online payment plan you need to:
          |
          |ensure your tax returns are up to date
          |owe £30,000 or less
          |have no other tax debts
          |have no other HMRC payment plans set up
          |You can use this service within 60 days of the payment deadline.
          |
          |Before you start
          |HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the entire outstanding amount.
          |To set up the payment plan, you’ll need to know your monthly income and spending, and any savings or investments.
          |Start now
        """.stripMargin

      private val mainTextWelsh =
        """Sefydlu cynllun talu ar gyfer Hunanasesiad
          |Mae cynllun talu yn eich galluogi i dalu’ch taliadau treth fesul rhandaliad dros gyfnod o amser.
          |
          |Mae eich cynllun yn cwmpasu’r dreth sydd arnoch ac, os yw’n berthnasol, y ddau daliad ymlaen llaw tuag at eich bil treth. Mae’r cynllun hefyd yn cwmpasu unrhyw gosbau neu daliadau yn erbyn eich cyfrif. Bydd yn rhaid i chi dalu llog ar y swm a dalwch yn hwyr.
          |
          |I fod yn gymwys i sefydlu cynllun talu ar-lein, mae’n rhaid i’r canlynol fod yn wir amdanoch:
          |
          |mae’n rhaid i chi sicrhau bod eich Ffurflenni Treth yn gyfredol
          |mae arnoch £30,000 neu lai
          |nid oes gennych unrhyw ddyledion treth eraill
          |nid ydych wedi sefydlu cynlluniau talu eraill gyda CThEF
          |Gallwch ddefnyddio’r gwasanaeth hwn cyn pen 60 diwrnod i’r dyddiad cau ar gyfer talu.
          |
          |Cyn i chi ddechrau
          |Bwriad CThEF yw y bydd hwn yn gynllun talu un-tro er mwyn rhoi cymorth ychwanegol i chi. Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Os na fyddwch, mae’n bosibl y bydd CThEF yn gofyn i chi dalu’r swm cyfan sy’n weddill.
          |
          |Er mwyn sefydlu’r cynllun talu, bydd angen i chi wybod beth yw’ch incwm a’ch gwariant misol, ac unrhyw gynilion neu fuddsoddiadau.
          |
          |Dechrau nawr
        """.stripMargin
    }

  }

}
