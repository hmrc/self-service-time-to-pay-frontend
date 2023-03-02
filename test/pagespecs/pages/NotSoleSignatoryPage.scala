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
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers._

class NotSoleSignatoryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/not-sole-signatory"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "You cannot set up a Direct Debit online"
    case Languages.Welsh   => "Ni allwch drefnu Debyd Uniongyrchol ar-lein"
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
        """You need a named account holder or someone with authorisation to set up a Direct Debit.
          |
          |If you are not the account holder or you wish to set up a Direct Debit with a multi-signature account, speak to an adviser on 0300 123 1813. You must ensure all account holders are present when calling
        """.stripMargin

      private val mainTextWelsh =
        """Mae angen rhywun sydd wedi’i enwi’n ddeiliad y cyfrif, neu rywun ag awdurdod, er mwyn trefnu Debyd Uniongyrchol.
          |
          |Os nad chi yw deiliad y cyfrif, neu os ydych yn dymuno trefnu Debyd Uniongyrchol gyda chyfrif aml-lofnod, rydym yn argymell eich bod yn siarad ag ymgynghorydd ar 0300 200 1900 yn y Gwasanaeth Cwsmeriaid Cymraeg. Rhaid i chi sicrhau bod holl ddeiliaid y cyfrif yn bresennol wrth ffonio.
        """.stripMargin
    }

  }

}
