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
import ssttpaffordability.model.IncomeCategory
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import testsupport.RichMatchers.{convertToAnyShouldWrapper, include}

class YourMonthlyIncomePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/monthly-income"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Your monthly income"
    case Languages.Welsh   => "Eich incwm misol"
  }

  def clickContinue(): Unit = clickOnContinue()

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
  }

  def assertPagePathCorrect: Assertion = probing {
    readPath() shouldBe path
  }

  def enterMonthlyIncome(value: String): Unit = {
    val monthlyIncome = xpath("//*[@id=\"monthlyIncome\"]")
    click on monthlyIncome
    enter(value)
  }

  def assertMonthlyIncomeValueIsDisplayed(value: String): Assertion = {
    val monthlyIncome = textField("monthlyIncome")
    monthlyIncome.value shouldBe value
  }

  def enterBenefits(value: String): Unit = {
    val benefits = xpath("//*[@id=\"benefits\"]")
    click on benefits
    enter(value)
  }

  def assertBenefitsValueIsDisplayed(value: String): Assertion = {
    val benefits = textField("benefits")
    benefits.value shouldBe value
  }

  def enterOtherIncome(value: String): Unit = {
    val otherIncome = xpath("//*[@id=\"otherIncome\"]")
    click on otherIncome
    enter(value)
  }

  def assertOtherIncomeValueIsDisplayed(value: String): Assertion = {
    val otherIncome = textField("otherIncome")
    otherIncome.value shouldBe value

  }

  def assertErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.SpecificErrorText().stripSpaces())
  }

  def assertNonNumeralErrorIsDisplayed(incomeCategory: IncomeCategory)(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.NonNumericErrorText(incomeCategory).stripSpaces())
  }

  def assertNegativeValueErrorIsDisplayed(incomeCategory: IncomeCategory)(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.NegativeValueError(incomeCategory).stripSpaces())
  }

  def assertMoreThanTwoDecimalPlacesErrorIsDisplayed(incomeCategory: IncomeCategory)(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.MoreThanTwoDecimalPlaces(incomeCategory).stripSpaces())
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

    object SpecificErrorText {

      def apply()(implicit language: Language): String = language match {
        case English => errorTextEnglish
        case Welsh   => errorTextWelsh
      }

      private val errorTextEnglish =
        """There is a problem
          |You must enter your income
          |If you do not have any income, call us on 0300 123 1813
        """.stripMargin

      private val errorTextWelsh =
        """Mae problem wedi codi
          |Mae’n rhaid i chi nodi’ch incwm
          |Os nad oes gennych unrhyw incwm, ffoniwch ni ar 0300 200 1900
        """.stripMargin
    }

    object NonNumericErrorText {

      def apply(incomeCategory: IncomeCategory)(implicit language: Language): String = language match {
        case English => errorTextEnglish(incomeCategory)
        case Welsh   => errorTextWelsh(incomeCategory)
      }

      private def errorTextEnglish(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "Monthly income after tax"
          case Benefits      => "Benefits"
          case OtherIncome   => "Other monthly income"
        }
        s"""There is a problem
          |$incomeCategoryString must be an amount, like £100 or £250.75
          """.stripMargin
      }

      private def errorTextWelsh(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "incwm misol ar ôl treth"
          case Benefits      => "budd-daliadau"
          case OtherIncome   => "incwm misol arall"
        }
        s"""Mae problem wedi codi
          |Mae’n rhaid i $incomeCategoryString fod yn swm, megis £100 neu £250.75
        """.stripMargin
      }
    }

    object NegativeValueError {
      def apply(incomeCategory: IncomeCategory)(implicit language: Language): String = language match {
        case English => errorTextEnglish(incomeCategory)
        case Welsh   => errorTextWelsh(incomeCategory)
      }

      private def errorTextEnglish(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "monthly income after tax"
          case Benefits      => "benefits"
          case OtherIncome   => "other monthly income"
        }
        s"""There is a problem
           |Enter a positive number only for $incomeCategoryString
        """.stripMargin
      }

      private def errorTextWelsh(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "incwm misol ar ôl treth"
          case Benefits      => "budd-daliadau"
          case OtherIncome   => "incwm misol arall"
        }
        s"""Mae problem wedi codi
           |	Nodwch rifau yn unig ar gyfer for $incomeCategoryString
        """.stripMargin
      }
    }

    object MoreThanTwoDecimalPlaces {
      def apply(incomeCategory: IncomeCategory)(implicit language: Language): String = language match {
        case English => errorTextEnglish(incomeCategory)
        case Welsh   => errorTextWelsh(incomeCategory)
      }

      private def errorTextEnglish(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "monthly income after tax"
          case Benefits      => "benefits"
          case OtherIncome   => "other monthly income"
        }
        s"""There is a problem
           |Amount must not contain more than 2 decimal places for $incomeCategoryString
        """.stripMargin
      }

      private def errorTextWelsh(incomeCategory: IncomeCategory) = {
        val incomeCategoryString = incomeCategory match {
          case MonthlyIncome => "incwm misol ar ôl treth"
          case Benefits      => "budd-daliadau"
          case OtherIncome   => "incwm misol arall"
        }
        s"""Mae problem wedi codi
           |Rhaid i’r swm beidio â chynnwys mwy na 2 le degol ar gyfer $incomeCategoryString
        """.stripMargin
      }
    }
  }

}
