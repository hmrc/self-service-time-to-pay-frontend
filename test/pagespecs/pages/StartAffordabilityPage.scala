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

class StartAffordabilityPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/start-affordability"

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "You need to provide your income and spending"
    case Language.Welsh   => "Mae angen i chi roi’ch incwm a’ch gwariant"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
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
        s"""You need to provide your income and spending
           |To make sure you can afford a payment plan for the remaining £4,900, we need to ask about your income and spending. This will help you set up a payment plan that is right for you.
           |
           |If you decide to call us, we will still ask you about your income and spending.
           |
           |How we work out your plan
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Mae angen i chi roi’ch incwm a’ch gwariant
           |Er mwyn gwneud yn siŵr eich bod yn gallu fforddio cynllun talu ar gyfer y £4,900 sy’n weddill, mae angen i ni ofyn i chi am eich incwm a’ch gwariant. Bydd hyn yn eich helpu i drefnu cynllun talu sy’n iawn i chi.
           |
           |Os byddwch yn penderfynu ein ffonio, byddwn yn dal i ofyn i chi am eich incwm a’ch gwariant.
           |
           |Sut yr ydym yn cyfrifo’ch cynllun
           |Yn eich blaen
        """.stripMargin
    }
  }
}
