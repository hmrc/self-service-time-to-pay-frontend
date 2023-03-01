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
import testsupport.RichMatchers._

abstract class CalculatorInstalmentsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._
  override def path: String = "/pay-what-you-owe-in-instalments/calculator/instalments"

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "How much can you pay each month?"
    case Languages.Welsh   => "Faint y gallwch ei dalu bob mis?"
  }

  def selectAnOption(): Unit = probing {
    val radioButton = xpath("//*[@type=\"radio\"]")
    click on radioButton
  }

  def selectASpecificOption(id: String) = probing {
    val specificRadioButton = xpath(s"//*[@id=$id]")
    click on specificRadioButton
  }

  def clickContinue(): Unit = probing{
    val button = xpath("//*[@id=\"next\"]")
    click on button
  }

  def selectCustomAmountOption(): Unit = probing {
    val customAmountOptionRadioButton = xpath("//*[@id=\"customAmountOption\"]")
    click on customAmountOptionRadioButton
  }

  def enterCustomAmount(value: String = ""): Unit = {
    val customAmountInputField = xpath("//*[@id=\"custom-amount-input\"]")
    click on customAmountInputField
    enter(value)
  }

  def clickOnBackButton(): Unit = click on id("back-link")

}

class CalculatorInstalmentsPage28thDay(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends CalculatorInstalmentsPage(baseUrl) {
  import WebBrowser._

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = Expected.MainText.DefaultCalculations().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def optionIsDisplayed(
      amount:   String,
      months:   Option[String] = None,
      interest: Option[String] = None
  )(implicit language: Language = Languages.English): Unit = (months, interest) match {
    case (Some(months), Some(interest)) =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount, months, interest).stripSpaces().split("\n")
      assertContentMatchesExpectedLines(expectedLines)
    case _ =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount).stripSpaces().split("\n")
      assertContentMatchesExpectedLines(expectedLines)
  }

  def optionIsNotDisplayed(
      amount:   String,
      months:   Option[String] = None,
      interest: Option[String] = None
  )(implicit language: Language = Languages.English): Unit = (months, interest) match {
    case (Some(months), Some(interest)) =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount, months, interest).stripSpaces().split("\n")
      assertContentDoesNotContainLines(expectedLines)
    case _ =>
      val expectedLines = Expected.MainText.DefaultOptionsText(amount).stripSpaces().split("\n")
      assertContentDoesNotContainLines(expectedLines)
  }

  def customAmountOptionIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    val expectedLines = Expected.MainText.CustomAmountOption().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
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

  def assertNegativeAmountErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(Expected.ErrorText.NegativeAmount().stripSpaces())
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
             |If the plan you choose runs into the next tax year, you still need to pay future tax bills on time.
             |£250 per month over 21 months
             |Includes total interest estimated at £138.16
             |£300 per month over 17 months
             |Includes total interest estimated at £116.53
             |£400 per month over 13 months
             |Includes total interest estimated at £89.39
             |A different monthly amount
             |Enter an amount that is at least £250 but no more than
             |I cannot afford to make these payments
             |You may still be able to set up a payment plan over the phone. Call us on 0300 123 1813 to discuss your debt.
             |Continue
          """.stripMargin

        private val mainTextWelsh =
          s"""Faint y gallwch ei dalu bob mis?
             |Yn seiliedig ar eich incwm sydd dros ben, rydym o’r farn y byddech yn gallu talu’r swm hwn bob mis. Os bydd y gyfradd llog yn newid, mae’n bosibl y bydd eich taliad misol olaf yn fwy neu’n llai na’r swm hwn.
             |Os bydd y cynllun yr ydych yn ei ddewis yn rhedeg i mewn i’r flwyddyn dreth nesaf, bydd dal angen i chi dalu’ch biliau treth yn y dyfodol mewn pryd.
             |£250 y mis, am 21 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £138.16
             |£300 y mis, am 17 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £116.53
             |£400 y mis, am 13 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £89.39
             |Swm misol gwahanol
             |Rhowch swm sydd o leiaf £250 ond heb fod yn fwy na
             |Nid wyf yn gallu fforddio’r taliadau hyn
             |Mae’n bosibl y byddwch yn dal i allu trefnu cynllun talu dros y ffôn. Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEF ar 0300 200 1900 i drafod eich opsiynau.
             |Yn eich blaen
          """.stripMargin
      }

      object CustomAmountOption {
        def apply()(implicit language: Language): String = language match {
          case English => customAmountOptionTextEnglish
          case Welsh   => customAmountOptionTextWelsh
        }

        private val customAmountOptionTextEnglish =
          s"""A different monthly amount
             |Enter an amount that is at least £250 but no more than £4,900
          """.stripMargin

        private val customAmountOptionTextWelsh =
          s"""Swm misol gwahanol
             |Rhowch swm sydd o leiaf £250 ond heb fod yn fwy na £4,900
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
    }

    object ErrorText {
      object BelowMinimum {
        def apply()(implicit language: Language): String = language match {
          case English => belowMinimumTextEnglish
          case Welsh   => belowMinimumTextWelsh
        }

        private val belowMinimumTextEnglish =
          s"""There is a problem
             |That amount is too low, enter an amount that is at least £250 but no more than £4,900
      """.stripMargin

        private val belowMinimumTextWelsh =
          s"""Mae problem wedi codi
             |Mae’r swm hwnnw’n rhy isel, rhowch swm sydd o leiaf £250 ond heb fod yn fwy na £4,900
      """.stripMargin
      }

      object AboveMaximum {
        def apply()(implicit language: Language): String = language match {
          case English => aboveMaximumTextEnglish
          case Welsh   => aboveMaximumTextWelsh
        }

        private val aboveMaximumTextEnglish =
          s"""There is a problem
             |That amount is too high, enter an amount that is at least £250 but no more than £4,900
      """.stripMargin

        private val aboveMaximumTextWelsh =
          s"""Mae problem wedi codi
             |Mae’r swm hwnnw’n rhy uchel, rhowch swm sydd o leiaf £250 ond heb fod yn fwy na £4,900
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
             |Enter numbers only
      """.stripMargin

        private val nonNumericTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch rifau yn unig
      """.stripMargin
      }

      object NegativeAmount {
        def apply()(implicit language: Language): String = language match {
          case English => negativeAmountTextEnglish
          case Welsh   => negativeAmountTextWelsh
        }

        private val negativeAmountTextEnglish =
          s"""There is a problem
             |Enter a positive number only
      """.stripMargin

        private val negativeAmountTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch rif positif yn uni
      """.stripMargin
      }
    }
  }
}

//class CalculatorInstalmentsPage11thDay(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends CalculatorInstalmentsPage(baseUrl) {
//
//  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
//    readPath() shouldBe path
//    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
//    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
//
//    // TODO [OPS-8650]: Update Expected.MainText to reflect change to payment plan calculations and reinstate test
//    //    val expectedLines = Expected.MainText().stripSpaces().split("\n")
//    //    assertContentMatchesExpectedLines(expectedLines)
//    ()
//  }
//
//  object Expected {
//
//    object GlobalHeaderText {
//
//      def apply()(implicit language: Language): String = language match {
//        case English => globalHeaderTextEnglish
//        case Welsh   => globalHeaderTextWelsh
//      }
//
//      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""
//
//      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
//    }
//
//    object MainText {
//      def apply()(implicit language: Language): String = language match {
//        case English => mainTextEnglish
//        case Welsh   => mainTextWelsh
//      }
//
//      private val mainTextEnglish =
//        s"""How many months do you want to pay over?
//           |2 months at £2,450
//           |Total interest:	£13.74
//           |Base rate + 2.5%	added to the final payment
//           |Total paid:	£4,913.74
//           |3 months at £1,633.33
//           |Total interest:	£20.51
//           |Base rate + 2.5%	added to the final payment
//           |Total paid:	£4,920.51
//           |4 months at £1,225
//           |Total interest:	£27.05
//           |Base rate + 2.5%	added to the final payment
//           |Total paid:	£4,927.05
//           |How we calculate interest
//           |We only charge interest on overdue amounts.
//           |We charge the Bank of England base rate plus 2.5%, calculated as simple interest.
//           |If the interest rate changes during your plan, your monthly payments will not change. If we need to, we'll settle the difference at the end of the plan.
//           |Continue
//           """.stripMargin
//
//      private val mainTextWelsh =
//        s"""Dros sawl mis yr hoffech dalu?
//           |2 o fisoedd ar £2,450
//           |Cyfanswm y llog:	£13.74
//           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
//           |Cyfanswm a dalwyd:	£4,913.74
//           |3 o fisoedd ar £1,633.33
//           |Cyfanswm y llog:	£20.51
//           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
//           |Cyfanswm a dalwyd:	£4,920.51
//           |4 o fisoedd ar £1,225
//           |Cyfanswm y llog:	£27.05
//           |Cyfradd sylfaenol + 2.5%	wedi’i ychwanegu at y taliad terfynol
//           |Cyfanswm a dalwyd:	£4,927.05
//           |Sut rydym yn cyfrifo llog
//           |Rydym yn codi llog ar symiau hwyr yn unig.
//           |Rydym yn codi cyfradd sylfaenol Banc Lloegr ynghyd â 2.5%, a gyfrifir fel llog syml.
//           |Os bydd y gyfradd llog yn newid yn ystod eich cynllun, ni fydd eich taliadau misol yn newid. Os bydd angen, byddwn yn setlo’r gwahaniaeth ar ddiwedd y cynllun.
//           |Yn eich blaen
//        """.stripMargin
//    }
//
//
//  }
//}
