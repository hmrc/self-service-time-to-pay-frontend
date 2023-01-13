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
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers.convertToAnyShouldWrapper


class YourMonthlyIncomePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/your-monthly-income"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Your monthly income"
    case Languages.Welsh => "Eich incwm misol"
  }

  override def clickOnContinue(): Unit = clickOnContinue()

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def enterPrimaryIncome(value: String): Unit = {
    val primaryIncome = xpath("//*[@id=\"primary-income\"]")
    click on primaryIncome
    enter(value)
  }

  def assertPrimaryIncomeValueIsDisplayed(value: String): Unit = {
    val primaryIncome = textField("primary-income")
    primaryIncome.value shouldBe value
  }

  def enterBenefits(value: String): Unit = {
    val benefits = xpath("//*[@id=\"benefits\"]")
    click on benefits
    enter(value)
  }

  def assertBenefitsValueIsDisplayed(value: String): Unit = {
    val benefits = textField("benefits")
    benefits.value shouldBe value
  }

  def enterOtherIncome(value: String): Unit = {
    val otherIncome = xpath("//*[@id=\"other-income\"]")
    click on otherIncome
    enter(value)
  }

  def assertOtherIncomeValueIsDisplayed(value: String): Unit = {
    val otherIncome = textField("other-income")
    otherIncome.value shouldBe value
  }

  def assertErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.TextError().stripSpaces()
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Your monthly income
           |Add information about yourself only.
           |Monthly income after tax
           |If you do not have a regular monthly income, add an estimate.
           |Benefits
           |For example, Universal Credit, tax credits
           |Other monthly income
           |For example, rental income, dividends
        """.stripMargin

      private val mainTextWelsh =
        s"""Eich incwm misol
           |Ychwanegwch wybodaeth amdanoch chi’ch hun yn unig.
           |Incwm misol ar ôl treth
           |Os nad oes gennych incwm misol rheolaidd, nodwch amcangyfrif.
           |Budd-daliadau
           |Er enghraifft, Credyd Cynhwysol, credydau treth
           |Incwm misol arall
           |Er enghraifft, incwm rhent, difidendau
        """.stripMargin
    }

    object TextError {
      def apply()(implicit language: Language): String = language match {
        case English => errorTextEnglish
        case Welsh => errorTextWelsh
      }

      private val errorTextEnglish =
        """There is a problem
          |You must enter an income figure.
          |If you do not have any income call us on 0300 200 3835.
        """.stripMargin

      private val errorTextWelsh =
        """Mae problem wedi codi
          |Mae’n rhaid i chi nodi ffigur ar gyfer yr incwm
          |Os nad oes gennych unrhyw incwm, ffoniwch ni ar 0300 200 1900
        """.stripMargin
    }
  }
}