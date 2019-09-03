/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class StartPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readArticle().stripSpaces shouldBe Expected.ArticleText().stripSpaces
  }

  def readArticle(): String = xpath("//*[@id=\"content\"]/article").element.text

  def readGlobalHeader(): String = id("global-header").element.text

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

      private val globalHeaderTextEnglish =
        """GOV.UK
          |Set up a payment plan
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu
        """.stripMargin
    }

    object ArticleText {

      def apply()(implicit language: Language): String = language match {
        case English => articleTextEnglish
        case Welsh   => articleTextWelsh
      }

      private val articleTextEnglish =
        """Set up a payment plan
          |A payment plan allows you to pay the tax you owe in instalments. The plan covers the amounts you need to pay now.
          |
          |We intend this as a one-off payment plan to give you extra support. You must keep up to date with your payments. If you do not, HMRC may ask you to pay the entire outstanding amount.
          |
          |Before you start
          |This payment plan is for Self Assessment tax only.
          |
          |To be eligible to set up an online payment plan you need to:
          |
          |owe £10,000 or less
          |have no other tax debts
          |have no other HMRC payment plans set up
          |Start now
        """.stripMargin

      private val articleTextWelsh =
        """
          |Trefnu cynllun talu
          |Mae cynllun talu yn eich galluogi i dalu’r dreth sydd arnoch fesul rhandaliad. Mae’r cynllun yn cwmpasu’r symiau y mae’n rhaid i chi eu talu nawr.
          |
          |Ein bwriad yw y bydd hwn yn gynllun talu un-tro i roi cymorth ychwanegol i chi. Mae’n rhaid i chi sicrhau eich bod yn gwneud eich taliadau mewn pryd. Os na fyddwch, mae’n bosibl y bydd CThEM yn gofyn i chi dalu’r swm cyfan sydd heb ei dalu.
          |
          |Cyn i chi ddechrau
          |Mae’r cynllun talu hwn ar gyfer treth Hunanasesiad yn unig.
          |
          |I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid bod y canlynol yn wir:
          |
          |mae arnoch £10,000 neu lai
          |nid oes gennych unrhyw ddyledion treth eraill
          |nid ydych wedi trefnu cynlluniau talu eraill â CThEM
          |Dechrau nawr
        """.stripMargin
    }

  }

  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }

}
