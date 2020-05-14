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

class CalculatorTaxLiabilitiesPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/tax-liabilities"

  val headingEnglish: String = "Your Self Assessment tax bill is £4,900.00"
  val headingWelsh: String = "Crynodeb o’ch cyfrif Hunanasesiad £4,900.00"

  def assertPageIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    readPath() shouldBe path
    title() shouldBe expectedTitle(lang)
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
    ()
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

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {

      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Your Self Assessment tax bill is £4,900.00
          |Self Assessment statement
          |Due 25 November 2019
          |First payment on account for tax year 2018 to 2019
          |
          |£2,500.00
          |Due 25 November 2019
          |Second payment on account for tax year 2018 to 2019
          |
          |£2,400.00
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Crynodeb o’ch cyfrif Hunanasesiad £4,900.00
          |Dadansoddiad Hunanasesiad
          |Due 25 November 2019
          |Taliad cyntaf ar gyfrif for tax year 2018 to 2019
          |
          |£2,500.00
          |Due 25 November 2019
          |Ail daliad ar gyfrif for tax year 2018 to 2019
          |
          |£2,400.00
          |Yn eich blaen
        """.stripMargin
    }

  }
}

