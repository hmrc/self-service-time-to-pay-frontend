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
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class TaxLiabilitiesPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/tax-liabilities"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
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
        """Yn ôl
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
}

