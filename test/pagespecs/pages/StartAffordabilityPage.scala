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
import org.scalatestplus.selenium.WebBrowser
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers._

class StartAffordabilityPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/start-affordability"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "We need to check you can afford the payment plan"
    case Languages.Welsh   => "Mae angen i ni wirio eich bod yn gallu fforddio’r cynllun talu"
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
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
        s"""We need to check you can afford the payment plan
           |To make sure you can afford your payment plan for the remaining £4,900, we need to ask about your income and spending. We will use this information to check how much income you have left over after your essential monthly spending.
           |
           |You may need to look at bank statements or utility bills before you continue. HMRC may ask for proof of your income and spending.
           |
           |If you decide to call us, we will still ask you about your income and spending.
           |
           |How we work out your plan
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Mae angen i ni wirio eich bod yn gallu fforddio’r cynllun talu
           |I wneud yn siŵr eich bod yn gallu fforddio’r cynllun talu ar gyfer y £4,900 sy’n weddill, mae angen i ni ofyn i chi am eich incwm a’ch gwariant. Byddwn yn defnyddio’r wybodaeth hon i wirio faint o incwm sy’n weddill gennych yn dilyn eich gwariant misol hanfodol.
           |
           |Mae’n bosibl y bydd angen i chi edrych ar gyfriflenni banc neu filiau cyfleustodau cyn i chi fynd yn eich blaen. Mae’n bosibl y bydd CThEF yn gofyn am dystiolaeth o’ch incwm a’ch gwariant.
           |
           |Os byddwch yn penderfynu ein ffonio, byddwn yn dal i ofyn i chi am eich incwm a’ch gwariant.
           |
           |Sut yr ydym yn cyfrifo’ch cynllun
           |Yn eich blaen
        """.stripMargin
    }
  }
}
