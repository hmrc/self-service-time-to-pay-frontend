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

import testsupport.Language.{English, Welsh}
import testsupport.Language
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class CalculatorTaxLiabilitiesPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override val path: String = "/pay-what-you-owe-in-instalments/calculator/tax-liabilities"

  def assertInitialPageIsDisplayed(implicit lang: Language = Language.English): Unit = probing {
    cssSelector("#content > details > summary > span").webElement.click()

    readPath() should include (path)
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Your Self Assessment tax bill is £4,900"
    case Language.Welsh   => "Mae’ch bil treth Hunanasesiad yn dod i gyfanswm o £4,900"
  }

  def clickOnStartNowButton(): Unit = {
    clickOnContinue()
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
        """Your Self Assessment tax bill is £4,900
          |Self Assessment statement
          |Due 25th November 2019
          |First payment on account for tax year 2019 to 2020
          |
          |£2,500
          |(includes interest added to date)
          |
          |Due 25th November 2019
          |Second payment on account for tax year 2019 to 2020
          |
          |£2,400
          |(includes interest added to date)
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Mae’ch bil treth Hunanasesiad yn dod i gyfanswm o £4,900
          |Datganiad Hunanasesiad
          |Yn ddyledus erbyn 25ain Tachwedd 2019
          |Taliad ar gyfrif cyntaf ar gyfer blwyddyn dreth 2019 i 2020
          |
          |£2,500
          |(yn cynnwys llog a ychwanegwyd hyd yn hyn)
          |
          |Yn ddyledus erbyn 25ain Tachwedd 2019
          |Ail daliad ar gyfrif ar gyfer blwyddyn dreth 2019 i 2020
          |
          |£2,400
          |(yn cynnwys llog a ychwanegwyd hyd yn hyn)
          |
          |Yn eich blaen
        """.stripMargin
    }

  }
}

