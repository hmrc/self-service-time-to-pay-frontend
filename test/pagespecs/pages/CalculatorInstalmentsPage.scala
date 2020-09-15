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

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class CalculatorInstalmentsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    println(s"\n\nExpected.MainText().stripSpaces().split(\\n) is:\n${Expected.MainText().stripSpaces()}\n\n")
    println(s"\n\nContent is:\n$content\n\n")

    Expected.MainText().stripSpaces().split("\n").foreach { expectedLine =>
      withClue(s"The page content should include '$expectedLine'"){
        content should include(expectedLine)
      }
    }
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How many months do you want to pay over?"
    case Languages.Welsh   => "Dros sawl mis yr hoffech dalu?"
  }

  def selectAnOption(): Unit = {
    val radioButton = xpath("/html/body/main/div[2]/article/form/fieldset/div/div[1]/input")
    click on radioButton
  }

  def clickContinue(): Unit = {
    val button = xpath("//*[@id=\"next\"]")
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
        s"""How many months do you want to pay over?
           |unchecked 4 months at £1,225.00
           |Total interest:
           |Base rate + 2.5%
           |£10.25
           |added to the final payment
           |Total paid:
           |£4,910.25
           |unchecked 3 months at £1,633.33
           |Total interest:
           |Base rate + 2.5%
           |£17.02
           |added to the final payment
           |Total paid:
           |£4,917.02
           |unchecked 4 months at £1,225.00
           |Total interest:
           |Base rate + 2.5%
           |£23.56
           |added to the final payment
           |Total paid:
           |£4,923.56
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Dros sawl mis yr hoffech dalu?
           |unchecked 4 o fisoedd ar £1,225.00
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£10.25
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£4,910.25
           |unchecked 3 o fisoedd ar £1,633.33
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£17.02
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£4,917.02
           |unchecked 4 o fisoedd ar £1,225.00
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£23.56
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£4,923.56
           |Sut rydym yn cyfrifo llog
           |Rydym yn codi llog ar symiau hwyr yn unig.
           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
           |Yn eich blaen
        """.stripMargin
    }
  }
}
