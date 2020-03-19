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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class CalculatorInstalmentsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces
    Expected.MainText().stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
  }

  def selectAnOption() = {
    val radioButton = xpath("/html/body/main/div[2]/article/form/div[1]/div[1]/input")
    click on radioButton
  }

  def clickContinue() = {
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
        s"""Back
           |How many months do you want to pay over?
           |unchecked
           |4 months at £300.00
           |Total interest:
           |Base rate + 2.5%
           |£200.00
           |added to the final payment
           |Total paid:
           |£200.00
           |unchecked
           |4 months at £300.00
           |Total interest:
           |Base rate + 2.5%
           |£200.00
           |added to the final payment
           |Total paid:
           |£200.00
           |unchecked
           |4 months at £300.00
           |Total interest:
           |Base rate + 2.5%
           |£200.00
           |added to the final payment
           |Total paid:
           |£200.00
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Yn ôl
           |Dros sawl mis yr hoffech dalu?
           |unchecked
           |4 o fisoedd ar £300.00
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£200.00
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£200.00
           |unchecked
           |4 o fisoedd ar £300.00
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£200.00
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£200.00
           |unchecked
           |4 o fisoedd ar £300.00
           |Cyfanswm y llog:
           |Cyfradd sylfaenol + 2.5%
           |£200.00
           |wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:
           |£200.00
           |Sut rydym yn cyfrifo llog
           |Rydym yn codi llog ar symiau hwyr yn unig.
           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
           |Yn eich blaen
        """.stripMargin
    }

  }

}
