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
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers.{convertToAnyShouldWrapper, include}

class YourMonthlySpendingPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/monthly-spending"

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Your monthly spending"
    case Language.Welsh   => "Eich gwariant misol"
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

  def enterHousing(value: String): Unit = {
    val housing = xpath("//*[@id=\"housing\"]")
    click on housing
    enter(value)
  }

  def enterPensionContributions(value: String): Unit = {
    val pensionContributions = xpath("//*[@id=\"pension-contributions\"]")
    click on pensionContributions
    enter(value)
  }

  def enterCouncilTax(value: String): Unit = {
    val councilTax = xpath("//*[@id=\"council-tax\"]")
    click on councilTax
    enter(value)
  }

  def assertHousingValueIsDisplayed(value: String): Assertion = {
    val housing = textField("housing")
    housing.value shouldBe value
  }

  def assertNonNumericErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.NonNumericErrorText().stripSpaces())
  }

  def assertNegativeNumberErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.NegativeNumberErrorText().stripSpaces())
  }

  def assertTooManyDecimalsErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.TooManyDecimalsErrorText().stripSpaces())
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
        s"""Your monthly spending
           |Add information about yourself only. If you live with other people, input your share of the spending. You can leave a box empty if it does not apply to you.
           |
           |Housing
           |For example, mortgage, rent, repairs
           |Pension contributions
           |Council tax
           |Utilities
           |For example, energy, water, phone, broadband
           |Debt repayments
           |For example, loan, court order payments
           |Travel
           |For example, vehicles, fuel, season ticket
           |Childcare costs
           |For example, education, maintenance payments
           |Insurance
           |For example, health, car, home
           |Groceries
           |Health
           |For example, prescriptions, private healthcare
           |
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Eich gwariant misol
           |Ychwanegwch wybodaeth amdanoch chi’ch hun yn unig. Os ydych yn byw gyda phobl eraill, nodwch eich cyfran chi o’r gwariant. Gallwch adael blwch yn wag os nad yw’n berthnasol i chi.
           |
           |Tai
           |Er enghraifft, morgais, rhent, atgyweiriadau
           |Cyfraniadau pensiwn
           |Treth Gyngor
           |Cyfleustodau
           |Er enghraifft, ynni, dŵr, ffôn, band eang
           |Ad-daliadau dyledion
           |Er enghraifft, benthyciad, taliadau Gorchymyn Llys
           |Teithio
           |Er enghraifft, cerbydau, tanwydd, tocynnau tymor
           |Costau gofal plant
           |Er enghraifft, addysg, taliadau cynhaliaeth
           |Yswiriant
           |Er enghraifft, iechyd, car, cartref
           |Nwyddau Groser
           |Iechyd
           |Er enghraifft, presgripsiynau, gofal iechyd preifat
           |
           |Yn eich blaen
        """.stripMargin
    }

    object NonNumericErrorText {
      def apply()(implicit language: Language): String = language match {
        case English => errorTextEnglish
        case Welsh   => errorTextWelsh
      }

      private val errorTextEnglish =
        """There is a problem
          |Housing must be an amount, like £100 or £250.75
        """.stripMargin

      private val errorTextWelsh =
        """Mae problem wedi codi
          |Mae’n rhaid i daliadau tai fod yn swm, megis £100 neu £250.75
        """.stripMargin
    }

    object NegativeNumberErrorText {
      def apply()(implicit language: Language): String = language match {
        case English => errorTextEnglish
        case Welsh   => errorTextWelsh
      }

      private val errorTextEnglish =
        """There is a problem
          |Enter a positive number only for housing
        """.stripMargin

      private val errorTextWelsh =
        """Mae problem wedi codi
          |Nodwch rif positif yn unig ar gyfer tai
        """.stripMargin
    }

    object TooManyDecimalsErrorText {
      def apply()(implicit language: Language): String = language match {
        case English => errorTextEnglish
        case Welsh   => errorTextWelsh
      }

      private val errorTextEnglish =
        """There is a problem
          |Amount must not contain more than 2 decimal places for housing
        """.stripMargin

      private val errorTextWelsh =
        """Mae problem wedi codi
          |Rhaid i’r swm beidio â chynnwys mwy na 2 le degol ar gyfertai
        """.stripMargin
    }
  }
}

