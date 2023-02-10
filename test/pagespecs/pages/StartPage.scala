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
    case Languages.Welsh   => "Sefydlu cynllun talu ar gyfer Hunanasesiad"
  }

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
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
          |A payment plan allows you to pay your tax charges in instalments, over a period of up to 12 months.
          |
          |Your plan covers your balancing payment, your first and second payment on account, and any other penalties or charges against your account. You’ll have to pay interest on the amount you pay late.
          |
          |To be eligible to set up an online payment plan you need to:
          |
          |have filed your 2020 to 2021 tax return
          |owe £30,000 or less
          |have no other tax debts
          |have no other HMRC payment plans set up
          |You can use this service within 60 days of the payment deadline.
          |
          |Start now
          |Before you start
          |HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the entire outstanding amount.
        """.stripMargin

      private val mainTextWelsh =
        """Sefydlu cynllun talu ar gyfer Hunanasesiad
          |Mae cynllun talu’n eich galluogi i dalu’ch taliadau treth fesul rhandaliad, dros gyfnod o hyd at 12 mis.
          |
          |Mae’ch cynllun yn cynnwys eich taliad mantoli, eich taliad ar gyfrif cyntaf a’ch ail daliad ar gyfrif, yn ogystal ag unrhyw gosbau neu daliadau eraill yn erbyn eich cyfrif. Bydd yn rhaid i chi dalu llog ar y swm a dalwch yn hwyr.
          |
          |I fod yn gymwys i sefydlu cynllun talu ar-lein, mae’n rhaid bod y canlynol yn wir:
          |
          |rydych wedi cyflwyno’ch Ffurflen Dreth ar gyfer 2019 i 2020
          |mae arnoch £30,000 neu lai
          |nid oes gennych unrhyw ddyledion treth eraill
          |nid ydych wedi sefydlu cynlluniau talu eraill gyda CThEM
          |Gallwch ddefnyddio’r gwasanaeth hwn cyn pen 60 diwrnod o’r dyddiad cau ar gyfer talu.
          |
          |Dechrau nawr
          |Cyn i chi ddechrau
          |Bwriad CThEM yw bod hwn yn gynllun talu un-tro i roi cymorth ychwanegol i chi. Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Os na fyddwch yn gwneud hynny, mae’n bosibl y bydd CThEM yn gofyn i chi dalu’r swm cyfan sydd heb ei dalu.
          |
          |""".stripMargin
    }

  }

}
