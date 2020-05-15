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
    case Languages.Welsh => "Trefnu cynllun talu"
  }

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
    pageTitle shouldBe expectedHeading(expectedHeadingContent(lang), lang)
    ()
  }

  def clickOnStartNowButton(): Unit = {
    val button = xpath("//*[@id=\"start\"]/div/button")
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
          |A payment plan allows you to pay your tax charges over a number of months. The plan covers the amounts you need to pay now and your next payment amount.
          |
          |HMRC intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the entire outstanding amount.
          |
          |Before you start
          |To be eligible to set up an online payment plan you need to:
          |
          |owe £10,000 or less
          |have no other tax debts
          |have no other HMRC payment plans set up
          |Start now
        """.stripMargin

      private val mainTextWelsh =
        """Trefnu cynllun talu
          |Mae cynllun talu yn eich galluogi i dalu’r dreth sydd arnoch fesul rhandaliad. Mae’r cynllun yn cwmpasu’r symiau y mae’n rhaid i chi eu talu nawr.
          |
          |Ein bwriad yw y bydd hwn yn gynllun talu un-tro i roi cymorth ychwanegol i chi. Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Os na fyddwch, mae’n bosibl y bydd CThEM yn gofyn i chi dalu’r swm cyfan sydd heb ei dalu.
          |
          |Cyn i chi ddechrau
          |Mae’r cynllun talu hwn ar gyfer treth Hunanasesiad yn unig.
          |
          |mae arnoch £10,000 neu lai
          |nid oes gennych unrhyw ddyledion treth eraill
          |nid ydych wedi trefnu cynlluniau talu eraill â CThEM
          |Dechrau nawr
        """.stripMargin
    }

  }

}
