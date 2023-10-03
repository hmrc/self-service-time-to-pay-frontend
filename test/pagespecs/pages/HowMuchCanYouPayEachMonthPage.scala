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
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import ssttpcalculator.model.PaymentPlanOption
import testsupport.RichMatchers._

class HowMuchCanYouPayEachMonthPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How much can you pay each month?"
    case Languages.Welsh   => "Faint y gallwch ei dalu bob mis?"
  }

  def expectedHeadingContentWithErrorPrefix(language: Language): String = language match {
    case Languages.English => "Error: " + expectedHeadingContent(English)
    case Languages.Welsh   => "Gwall: " + expectedHeadingContent(Welsh)
  }

  def selectAnOption(): Unit = probing {
    val radioButton = xpath("//*[@type=\"radio\"]")
    click on radioButton
  }

  def selectASpecificOption(paymentPlanOption: PaymentPlanOption): Unit = probing {
    val specificRadioButton = xpath(s"""//*[@id="${paymentPlanOption.entryName}"]""")
    click on specificRadioButton
  }

  def clickContinue(): Unit = probing{
    clickOnContinue()
  }

  def clickOnBackLink(): Unit = goBack()

  def selectCustomAmountOption(): Unit = probing {
    val customAmountOptionRadioButton = xpath("//*[@id=\"customAmountOption\"]")
    click on customAmountOptionRadioButton
  }

  def enterCustomAmount(value: String = ""): Unit = {
    val customAmountInputField = xpath("//*[@id=\"custom-amount-input\"]")
    click on customAmountInputField
    enter(value)
  }

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    assertInitialPageContentIsDisplayed(lang)
  }

  def assertInitialPageContentIsDisplayed(implicit lang: Language = English): Unit = probing {
    val expectedLines = Expected.MainText.DefaultCalculations().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def optionIsDisplayed(
      amount:   String,
      months:   Option[String] = None,
      interest: Option[String] = None
  )(implicit language: Language = Languages.English): Unit = (months, interest) match {
    case (Some(months), Some(interest)) =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount, months, interest).splitIntoLines()
      assertContentMatchesExpectedLines(expectedLines)
    case _ =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount).splitIntoLines()
      assertContentMatchesExpectedLines(expectedLines)
  }

  def optionIsNotDisplayed(
      amount:   String,
      months:   Option[String] = None,
      interest: Option[String] = None
  )(implicit language: Language = Languages.English): Unit = (months, interest) match {
    case (Some(months), Some(interest)) =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount, months, interest).splitIntoLines()
      assertContentDoesNotContainLines(expectedLines)
    case _ =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount).splitIntoLines()
      assertContentDoesNotContainLines(expectedLines)
  }

  def customAmountOptionIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    val expectedLines = Expected.MainText.CustomOption().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def customAmountOptionNotDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    val expectedLines = Expected.MainText.CustomOption().splitIntoLines()
    assertContentDoesNotContainLines(expectedLines)
    ()
  }

  def assertPageWithCustomAmountIsDisplayed(amount:   String,
                                            months:   Option[String] = None,
                                            interest: Option[String] = None
  )(implicit lang: Language = English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    assertPageWithCustomAmountContentIsDisplayed(amount, months, interest)(lang)
    ()
  }

  def assertPageWithCustomAmountContentIsDisplayed(amount:   String,
                                                   months:   Option[String] = None,
                                                   interest: Option[String] = None
  )(implicit lang: Language = English): Unit = probing {
    val expectedLines = Expected.MainText.CustomAmountDisplayed(amount).splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
    optionIsDisplayed(amount, months, interest)
    ()
  }

  def assertExpectedHeadingContentWithErrorPrefix(implicit lang: Language = English): Assertion = probing {
    pageTitle shouldBe expectedTitle(expectedHeadingContentWithErrorPrefix(lang), lang)
  }

  def assertNoOptionSelectedErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.NoOptionSelected().stripSpaces())
  }

  def assertBelowMinimumErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.BelowMinimum().stripSpaces())
  }

  def assertAboveMaximumErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.AboveMaximum().stripSpaces())
  }

  def assertNoInputErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.NoInput().stripSpaces())
  }

  def assertNonNumericErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.NonNumeric().stripSpaces())
  }

  def assertDecimalPlacesErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.DecimalPlaces().stripSpaces())
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
      object DefaultCalculations {
        def apply()(implicit language: Language): String = language match {
          case English => mainTextEnglish
          case Welsh   => mainTextWelsh
        }

        private val mainTextEnglish =
          s"""How much can you pay each month?
             |Based on your left over income, this is how much we think you could pay each month. Your final monthly payment may be more or less if the interest rate changes.
             |How we calculate interest
             |We charge interest on all overdue amounts.
             |We charge the Bank of England base rate plus 2.5% per year.
             |If the interest rate changes during your payment plan, you may need to settle any difference at the end. We will contact you if this is the case.
             |If the plan you choose runs into the next tax year, you still need to pay future tax bills on time.
             |£500 per month over 10 months
             |Includes total interest estimated at £73.08
             |£600 per month over 9 months
             |Includes total interest estimated at £62.20
             |£800 per month over 7 months
             |Includes total interest estimated at £48.65
             |I cannot afford to make these payments
             |You may still be able to set up a payment plan over the phone. Call us on 0300 123 1813 to discuss your debt.
             |Continue
          """.stripMargin

        private val mainTextWelsh =
          s"""Faint y gallwch ei dalu bob mis?
             |Yn seiliedig ar eich incwm sydd dros ben, rydym o’r farn y byddech yn gallu talu’r swm hwn bob mis. Os bydd y gyfradd llog yn newid, mae’n bosibl y bydd eich taliad misol olaf yn fwy neu’n llai na’r swm hwn.
             |Sut rydym yn cyfrifo llog
             |Rydym yn codi llog ar bob swm sy’n hwyr.
             |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5% y flwyddyn.
             |Os bydd y gyfradd llog yn newid yn ystod eich cynllun talu, efallai bydd yn rhaid i chi setlo unrhyw wahaniaeth ar y diwedd. Byddwn yn cysylltu â chi os yw hyn yn wir.
             |Os bydd y cynllun yr ydych yn ei ddewis yn rhedeg i mewn i’r flwyddyn dreth nesaf, bydd dal angen i chi dalu’ch biliau treth yn y dyfodol mewn pryd.
             |£500 y mis, am 10 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £73.08
             |£600 y mis, am 9 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £62.20
             |£800 y mis, am 7 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £48.65
             |Nid wyf yn gallu fforddio’r taliadau hyn
             |Mae’n bosibl y byddwch yn dal i allu trefnu cynllun talu dros y ffôn. Ffoniwch ni ar 0300 200 1900 i drafod eich dyled.
             |Yn eich blaen
          """.stripMargin
      }

      object CustomOption {
        def apply()(implicit language: Language): String = language match {
          case English => customtOptionTextEnglish
          case Welsh   => customOptionTextWelsh
        }

        private val customtOptionTextEnglish =
          s"""A different monthly amount
             |Enter an amount that is at least £500 but no more than £4,914.40
          """.stripMargin

        private val customOptionTextWelsh =
          s"""Swm misol gwahanol
             |Rhowch swm sydd o leiaf £500 ond heb fod yn fwy na £4,914.40
          """.stripMargin
      }

      object DefaultOptionsText {

        def apply(amount: String)(implicit language: Language): String = {
          language match {
            case English => partialOptionsTextEnglish(amount)
            case Welsh   => partialOptionsTextWelsh(amount)
          }
        }

        private def partialOptionsTextEnglish(amount: String): String =
          s"""£$amount per month over
          """.stripMargin

        private def partialOptionsTextWelsh(amount: String): String =
          s"""£$amount y mis, am
          """.stripMargin

        def apply(amount: String, months: String, interest: String)(implicit language: Language): String = {
          language match {
            case English => optionsTextEnglish(amount, months, interest)
            case Welsh   => optionsTextWelsh(amount, months, interest)
          }
        }

        private def optionsTextEnglish(amount: String, months: String, interest: String): String =
          s"""£$amount per month over $months months
             |Includes total interest estimated at £$interest
          """.stripMargin

        private def optionsTextWelsh(amount: String, months: String, interest: String): String =
          s"""£$amount y mis, am $months mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £$interest
          """.stripMargin
      }

      object CustomAmountDisplayed {
        def apply(amount: String)(implicit language: Language): String = language match {
          case English => customAmountTextEnglish(amount)
          case Welsh   => customAmountTextWelsh(amount)
        }

        private def customAmountTextEnglish(amount: String) =
          s"""You have chosen to pay £$amount per month. Your final monthly payment may be more or less if the interest rate changes.
      """.stripMargin

        private def customAmountTextWelsh(amount: String) =
          s"""Rydych wedi dewis talu £$amount y mis. Os bydd y gyfradd llog yn newid, mae’n bosibl y bydd eich taliad misol olaf yn fwy neu’n llai na’r swm hwn.
      """.stripMargin
      }
    }

    object ErrorText {

      object NoOptionSelected {
        def apply()(implicit language: Language): String = language match {
          case English => noOptionSelectedTextEnglish
          case Welsh   => noOptionSelectedTextWelsh
        }

        private val noOptionSelectedTextEnglish =
          s"""There is a problem
             |Select how much you can pay each month
      """.stripMargin

        private val noOptionSelectedTextWelsh =
          s"""Mae problem wedi codi
             |Dewiswch opsiwn
      """.stripMargin
      }

      object BelowMinimum {
        def apply()(implicit language: Language): String = language match {
          case English => belowMinimumTextEnglish
          case Welsh   => belowMinimumTextWelsh
        }

        private val belowMinimumTextEnglish =
          s"""There is a problem
             |Enter an amount that is at least £500 but no more than £4,914.40
      """.stripMargin

        private val belowMinimumTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £500 ond sydd ddim mwy na £4,914.40
      """.stripMargin
      }

      object AboveMaximum {
        def apply()(implicit language: Language): String = language match {
          case English => aboveMaximumTextEnglish
          case Welsh   => aboveMaximumTextWelsh
        }

        private val aboveMaximumTextEnglish =
          s"""There is a problem
             |Enter an amount that is at least £500 but no more than £4,914.40
      """.stripMargin

        private val aboveMaximumTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £500 ond sydd ddim mwy na £4,914.40
      """.stripMargin
      }

      object NoInput {
        def apply()(implicit language: Language): String = language match {
          case English => noInputTextEnglish
          case Welsh   => noInputTextWelsh
        }

        private val noInputTextEnglish =
          s"""There is a problem
             |Enter an amount
      """.stripMargin

        private val noInputTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm
      """.stripMargin
      }

      object NonNumeric {
        def apply()(implicit language: Language): String = language match {
          case English => nonNumericTextEnglish
          case Welsh   => nonNumericTextWelsh
        }

        private val nonNumericTextEnglish =
          s"""There is a problem
             |Enter an amount that is at least £500 but no more than £4,914.40
      """.stripMargin

        private val nonNumericTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £500 ond sydd ddim mwy na £4,914.40
      """.stripMargin
      }

      object DecimalPlaces {
        def apply()(implicit language: Language): String = language match {
          case English => belowMinimumTextEnglish
          case Welsh   => belowMinimumTextWelsh
        }

        private val belowMinimumTextEnglish =
          s"""There is a problem
             |Amount must not contain more than 2 decimal places
      """.stripMargin

        private val belowMinimumTextWelsh =
          s"""Mae problem wedi codi
             |Rhaid i’r swm beidio â chynnwys mwy na 2 le degol
      """.stripMargin
      }
    }
  }
}
