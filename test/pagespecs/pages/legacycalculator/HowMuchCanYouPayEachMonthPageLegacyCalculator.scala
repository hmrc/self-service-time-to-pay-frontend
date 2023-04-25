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

package pagespecs.pages.legacycalculator

import langswitch.Languages.{English, Welsh}
import langswitch.Language
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import pagespecs.pages.{BaseUrl, HowMuchCanYouPayEachMonthPage}
import testsupport.RichMatchers._

class HowMuchCanYouPayEachMonthPageLegacyCalculator(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends HowMuchCanYouPayEachMonthPage(baseUrl)(webDriver) {

  import WebBrowser._
  override def assertInitialPageIsDisplayed(implicit lang: Language = English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = ExpectedLegacyCalculator.MainText.DefaultCalculations().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  override def assertInitialPageContentIsDisplayed(implicit lang: Language = English): Unit = probing {
    val expectedLines = ExpectedLegacyCalculator.MainText.DefaultCalculations().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def assertContentDoesNotContainOrSeparator(implicit lang: Language = English): Unit = probing {
    val expectedLines = ExpectedLegacyCalculator.MainText.OrSeparator().stripSpaces().split("\n")
    assertContentDoesNotContainLines(expectedLines)
    ()
  }

  object ExpectedLegacyCalculator {

    object MainText {

      object DefaultCalculations {
        def apply()(implicit language: Language): String = language match {
          case English => mainTextEnglish
          case Welsh   => mainTextWelsh
        }

        private val mainTextEnglish =
          s"""How much can you pay each month?
             |Based on your left over income, this is how much we think you could pay each month. Your final monthly payment may be more or less if the interest rate changes.
             |If the plan you choose runs into the next tax year, you still need to pay future tax bills on time.
             |£490 per month over 10 months
             |Includes total interest estimated at £74.30
             |£544.44 per month over 9 months
             |Includes total interest estimated at £67.63
             |£612.50 per month over 8 months
             |Includes total interest estimated at £60.97
             |I cannot afford to make these payments
             |You may still be able to set up a payment plan over the phone. Call us on 0300 123 1813 to discuss your debt.
             |Continue
          """.stripMargin

        private val mainTextWelsh =
          s"""Faint y gallwch ei dalu bob mis?
             |Yn seiliedig ar eich incwm sydd dros ben, rydym o’r farn y byddech yn gallu talu’r swm hwn bob mis. Os bydd y gyfradd llog yn newid, mae’n bosibl y bydd eich taliad misol olaf yn fwy neu’n llai na’r swm hwn.
             |Os bydd y cynllun yr ydych yn ei ddewis yn rhedeg i mewn i’r flwyddyn dreth nesaf, bydd dal angen i chi dalu’ch biliau treth yn y dyfodol mewn pryd.
             |£490 y mis, am 10 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £74.30
             |£544.44 y mis, am 9 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £67.63
             |£612.50 y mis, am 8 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £60.97
             |Nid wyf yn gallu fforddio’r taliadau hyn
             |Mae’n bosibl y byddwch yn dal i allu trefnu cynllun talu dros y ffôn. Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900 i drafod eich opsiynau.
             |Yn eich blaen
          """.stripMargin
      }

      object OrSeparator {
        def apply()(implicit language: Language): String = language match {
          case English => orSeparatorTextEnglish
          case Welsh   => orSeparatorTextWelsh
        }

        private val orSeparatorTextEnglish =
          s"""or""".stripMargin

        private val orSeparatorTextWelsh =
          s"""neu""".stripMargin
      }
    }
  }
}
