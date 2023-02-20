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
    ()
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
           |21 months at £250
           |Total interest: £138.16
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£5,038.16
           |17 months at £300
           |Total interest:	£116.53
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£5,016.53
           |13 months at £400
           |Total interest:	£89.39
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,989.39
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextEnglish =
        s"""How many months do you want to pay over?
           |21 months at £250
           |Total interest: £138.16
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£5,038.16
           |17 months at £300
           |Total interest:	£116.53
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£5,016.53
           |13 months at £400
           |Total interest:	£89.39
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,989.39
           |How we calculate interest
           |We only charge interest on overdue amounts.
           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Dros sawl mis yr hoffech dalu?
           |21 o fisoedd ar £250
           |Cyfanswm y llog: £138.16
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£5,038.16
           |17 o fisoedd ar £300
           |Cyfanswm y llog:	£116.53
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£5,016.53
           |13 o fisoedd ar £400
           |Cyfanswm y llog:	£89.39
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,989.39
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

    // TODO [OPS-8650]: Update Expected.MainText to reflect change to payment plan calculations and reinstate test
    //    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    //    assertContentMatchesExpectedLines(expectedLines)
    ()
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
           |2 months at £2,450
           |Total interest:	£13.74
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,913.74
           |3 months at £1,633.33
           |Total interest:	£20.51
           |Base rate + 2.5%	added to the final payment
           |Total paid:	£4,920.51
           |4 months at £1,225
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
           |2 o fisoedd ar £2,450
           |Cyfanswm y llog:	£13.74
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,913.74
           |3 o fisoedd ar £1,633.33
           |Cyfanswm y llog:	£20.51
           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
           |Cyfanswm a dalwyd:	£4,920.51
           |4 o fisoedd ar £1,225
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
