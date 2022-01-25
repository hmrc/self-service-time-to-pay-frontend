/*
 * Copyright 2022 HM Revenue & Customs
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

abstract class CalculatorInstalmentsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How many months do you want to pay over?"
    case Languages.Welsh   => "Dros sawl mis yr hoffech dalu?"
  }

  def selectAnOption(): Unit = probing {
    val radioButton = xpath("//*[@type=\"radio\"]")
    click on radioButton
  }

  def clickContinue(): Unit = probing{
    val button = xpath("//*[@id=\"next\"]")
    click on button
  }
}

class CalculatorInstalmentsPage28thDay(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends CalculatorInstalmentsPage(baseUrl) {
  import WebBrowser._

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

      private val mainTextEnglishNew =
        s"""How many months do you want to pay over?
           |2 months at £2,450.00
           |Total interest:	£21.16
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,921.16
           |3 months at £1,633.33
           |Total interest:	£27.92
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,927.92
           |4 months at £1,225.00
           |Total interest:	£34.47
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,934.47
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextEnglish =
        s"""How many months do you want to pay over?
           |2 months at £2,450.00
           |Total interest: £21.16
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,921.16
           |3 months at £1,633.33
           |Total interest: £27.92
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,927.92
           |4 months at £1,225.00
           |Total interest: £34.47
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,934.47
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Dros sawl mis yr hoffech dalu?
           |2 o fisoedd ar £2,450.00
           |Cyfanswm y llog: £21.16
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,921.16
           |3 o fisoedd ar £1,633.33
           |Cyfanswm y llog:	£27.92
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,927.92
           |4 o fisoedd ar £1,225.00
           |Cyfanswm y llog:	£34.47
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,934.47
           |Sut rydym yn cyfrifo llog
           |Rydym yn codi llog ar symiau hwyr yn unig.
           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
           |Yn eich blaen
        """.stripMargin
    }
  }
}

class CalculatorInstalmentsPage11thDay(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends CalculatorInstalmentsPage(baseUrl) {

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
        s"""How many months do you want to pay over?
           |2 months at £2,450.00
           |Total interest:	£13.74
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,913.74
           |3 months at £1,633.33
           |Total interest:	£20.51
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,920.51
           |4 months at £1,225.00
           |Total interest:	£27.05
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,927.05
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
           """.stripMargin

      private val mainTextWelsh =
        s"""Dros sawl mis yr hoffech dalu?
           |2 o fisoedd ar £2,450.00
           |Cyfanswm y llog:	£13.74
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,913.74
           |3 o fisoedd ar £1,633.33
           |Cyfanswm y llog:	£20.51
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,920.51
           |4 o fisoedd ar £1,225.00
           |Cyfanswm y llog:	£27.05
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,927.05
           |Sut rydym yn cyfrifo llog
           |Rydym yn codi llog ar symiau hwyr yn unig.
           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
           |Yn eich blaen
        """.stripMargin
    }
  }
}
