/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class CalculatorInstalmentsAbPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments/ab"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing{
    readPath() shouldBe path
    //  readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def assertPageIsDisplayed(checkState: String)(implicit lang: Language): Assertion = probing{
    readPath() shouldBe path
    //  readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText(checkState).stripSpaces()
  }

  def selectAnOption =
    {
      val radioButton = xpath("/html/body/main/div[2]/article/form/div[1]/div[1]/input")
      click on radioButton
    }

  def clickContinue =
    {
      val button = xpath("//*[@id=\"next\"]")
      click on button
    }

  object Expected {
    object MainText {
      def apply(checkState: String = "unchecked")(implicit language: Language): String = language match {
        case English => mainTextEnglish(checkState)
        case Welsh   => mainTextWelsh(checkState)
      }

      private def mainTextEnglish(checkState: String) =
        s"""
          |BETA This is a new service – your feedback will help us to improve it.
          |English | Cymraeg
          |Back
          |How many months do you want to pay over?
          |${checkState}
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
          |Get help with this page.
        """.stripMargin

      private def mainTextWelsh(checkState: String) =
        s"""
          |BETA Mae hwn yn wasanaeth newydd – bydd eich adborth yn ein helpu i'w wella.
          |English | Cymraeg
          |Yn ôl
          |Dros sawl mis yr hoffech dalu?
          |${checkState}
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
          |Help gyda'r dudalen hon.
        """.stripMargin
    }
  }

  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }
}
