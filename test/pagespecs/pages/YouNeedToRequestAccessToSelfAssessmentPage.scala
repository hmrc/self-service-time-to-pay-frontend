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
import testsupport.RichMatchers._

class YouNeedToRequestAccessToSelfAssessmentPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import org.scalatestplus.selenium.WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/access-your-self-assessment-online"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "You need to request access to Self Assessment"
    case Language.Welsh   => "Mae’n rhaid i chi wneud cais i gael at eich cyfrif Hunanasesiad"
  }

  def clickTheButton() = {
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
        """You have not yet requested access to your Self Assessment online.
          |You need to do this before you can continue to set up your payment plan.
          |Request access to Self Assessment
          |"""
          .stripMargin

      private val mainTextWelsh =
        """Nid ydych wedi gwneud cais i gael at eich cyfrif Hunanasesiad ar-lein hyd yn hyn.
           |Mae angen i chi wneud hyn cyn y gallwch fynd yn eich blaen i drefnu’ch cynllun talu.
           |Gwneud cais i gael mynediad at Hunanasesiad
           |"""
          .stripMargin
    }

  }

}
