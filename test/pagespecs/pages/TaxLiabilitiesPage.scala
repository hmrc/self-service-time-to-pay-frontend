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

class TaxLiabilitiesPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/tax-liabilities"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    //  readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
  }

  def clickOnStartNowButton(): Unit = {
    val button = xpath("/html/body/main/div[2]/article/form/div/button")
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
          |Sign-out
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu
          |Allgofnodi
        """.stripMargin
    }

    object MainText {

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """BETA	This is a new service – your feedback will help us to improve it.
          |English | Cymraeg
          |Back
          |Your Self Assessment account summary
          |Total due £4,900.00
          |Self Assessment breakdown
          |For the tax year ending 5 April 2020
          |First payment on account
          |Your payment is due on 25 November 2019
          |£2,500.00
          |Second payment on account
          |Your payment is due on 25 November 2019
          |£2,400.00
          |Continue
          |Get help with this page.
        """.stripMargin

      private val mainTextWelsh =
        """BETA	Mae hwn yn wasanaeth newydd – bydd eich adborth yn ein helpu i'w wella.
          |English | Cymraeg
          |Yn ôl
          |Crynodeb o’ch cyfrif Hunanasesiad
          |Cyfanswm sy’n ddyledus £4,900.00
          |Dadansoddiad Hunanasesiad
          |Ar gyfer y flwyddyn dreth a ddaeth i ben 5 April 2020
          |Taliad cyntaf ar gyfrif
          |Mae’ch taliad yn ddyledus ar 25 November 2019
          |£2,500.00
          |Ail daliad ar gyfrif
          |Mae’ch taliad yn ddyledus ar 25 November 2019
          |£2,400.00
          |Yn eich blaen
          |Help gyda'r dudalen hon.
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

