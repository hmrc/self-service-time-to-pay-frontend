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
import org.scalatestplus.selenium.WebBrowser.pageTitle
import testsupport.RichMatchers._

class CalculatorInstalmentsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)

  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How many months do you want to pay over?"
    case Languages.Welsh   => "Dros sawl mis yr hoffech dalu?"
  }

  def selectAnOption(): Unit = probing {
    val radioButton = xpath("/html/body/main/div[2]/article/form/fieldset/div/div[1]/input")
    click on radioButton
  }

  def clickContinue(): Unit = probing{
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

      private val mainTextEnglishNew =
        s"""How many months do you want to pay over?
           |2 months at £2,450.00
           |Total interest:	£21.60
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,921.60
           |3 months at £1,633.33
           |Total interest:	£28.36
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,928.36
           |4 months at £1,225.00
           |Total interest:	£34.90
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,934.90
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextEnglish =
        s"""How many months do you want to pay over?
           |2 months at £2,450.00
           |Total interest: £16.36
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,916.36
           |3 months at £1,633.33
           |Total interest: £23.12
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,923.12
           |4 months at £1,225.00
           |Total interest: £29.67
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,929.67
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Dros sawl mis yr hoffech dalu?
           |2 o fisoedd ar £2,450.00
           |Cyfanswm y llog:	£16.36
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,916.36
           |3 o fisoedd ar £1,633.33
           |Cyfanswm y llog:	£23.12
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,923.12
           |4 o fisoedd ar £1,225.00
           |Cyfanswm y llog:	£29.67
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,929.67
           |Sut rydym yn cyfrifo llog
           |Rydym yn codi llog ar symiau hwyr yn unig.
           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
           |Yn eich blaen
        """.stripMargin
    }
  }
}

class CalculatorInstalmentsPageDayOfMonth28th(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends CalculatorInstalmentsPage(baseUrl) {

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected28th.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  object Expected28th {

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
           |2 months at £2,450.00
           |Total interest:	£21.60
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,921.60
           |3 months at £1,633.33
           |Total interest:	£28.36
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,928.36
           |4 months at £1,225.00
           |Total interest:	£34.90
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,934.90
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""TODO""".stripMargin
    }
  }
}
