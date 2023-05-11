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

package pagespecs.pages.legacycalculator

import langswitch.Languages.{English, Welsh}
import langswitch.{Language, Languages}
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import pagespecs.pages.{BaseUrl, HowMuchCanYouPayEachMonthPage}
import testsupport.RichMatchers._

class HowMuchCanYouPayEachMonthPageLegacyCalculator(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends HowMuchCanYouPayEachMonthPage(baseUrl)(webDriver) {

  import WebBrowser._
  override def assertInitialPageIsDisplayed(implicit lang: Language = English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = ExpectedLegacyCalculator.MainText.DefaultCalculations().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  override def assertInitialPageContentIsDisplayed(implicit lang: Language = English): Unit = probing {
    val expectedLines = ExpectedLegacyCalculator.MainText.DefaultCalculations().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  override def customAmountOptionIsDisplayed(implicit lang: Language = Languages.English): Unit = probing {
    val expectedLines = ExpectedLegacyCalculator.MainText.CustomOption().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  override def assertBelowMinimumErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(ExpectedLegacyCalculator.ErrorText.BelowMinimum().stripSpaces())
  }

  override def assertAboveMaximumErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(ExpectedLegacyCalculator.ErrorText.AboveMaximum().stripSpaces())
  }

  override def assertNonNumericErrorIsDisplayed(implicit lang: Language = English): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces() should include(ExpectedLegacyCalculator.ErrorText.NonNumeric().stripSpaces())
  }

  override def assertPageWithCustomAmountContentIsDisplayed(amount:   String,
                                                            months:   Option[String] = None,
                                                            interest: Option[String] = None
  )(implicit lang: Language = English): Unit = probing {
    val expectedLines = ExpectedLegacyCalculator.MainText.CustomAmountDisplayed(amount).stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    optionIsDisplayed(amount, months, interest)
    ()
  }

  object ExpectedLegacyCalculator {

    object MainText {

      object DefaultCalculations {
        def apply()(implicit language: Language): String = language match {
          case English => mainTextEnglish
          case Welsh   => mainTextWelsh
        }

        private val mainTextEnglish =
          s"""How much can you pay each month?
             |Based on your left over income, you can now select a payment plan. The final monthly payment in your plan will be more as it will include interest and any remaining tax you owe.
             |If the plan you choose runs into the next tax year, you still need to pay future tax bills on time.
             |£490 per month over 10 months
             |Includes total interest estimated at £74.30
             |£544.44 per month over 9 months
             |Includes total interest estimated at £67.63
             |£612.50 per month over 8 months
             |Includes total interest estimated at £60.97
             |I cannot afford to make these payments
             |You may still be able to set up a payment plan over the phone. Call us on 0300 123 1813 to discuss your debt.
             |Continue
          """.stripMargin

        private val mainTextWelsh =
          s"""Faint y gallwch ei dalu bob mis?
             |Yn seiliedig ar yr incwm sydd gennych dros ben, gallwch nawr ddewis gynllun talu. Bydd y taliad misol olaf yn eich cynllun yn fwy oherwydd y bydd yn cynnwys llog ac unrhyw dreth sy’n weddill sydd arnoch.
             |Os bydd y cynllun yr ydych yn ei ddewis yn rhedeg i mewn i’r flwyddyn dreth nesaf, bydd dal angen i chi dalu’ch biliau treth yn y dyfodol mewn pryd.
             |£490 y mis, am 10 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £74.30
             |£544.44 y mis, am 9 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £67.63
             |£612.50 y mis, am 8 mis
             |Mae hyn yn cynnwys cyfanswm y llog wedi’i amcangyfrif, sef £60.97
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
          s"""Pay more per month
             |Enter an amount between £612.50 and £2,450 to pay over fewer months. We will suggest a plan that is closest to the amount you enter.
          """.stripMargin

        private val customOptionTextWelsh =
          s"""Talu mwy bob mis
             |Nodwch swm sydd rhwng £612.50 a £2,450 i’w dalu dros lai o fisoedd. Byddwn yn awgrymu cynllun sydd agosaf at y swm y byddwch yn ei nodi.
          """.stripMargin
      }

      object CustomAmountDisplayed {
        def apply(amount: String)(implicit language: Language): String = language match {
          case English => customAmountTextEnglish(amount)
          case Welsh   => customAmountTextWelsh(amount)
        }

        private def customAmountTextEnglish(amount: String) =
          s"""Based on your left over income, you can now select a payment plan. The final monthly payment in your plan will be more as it will include interest and any remaining tax you owe.
             |If the plan you choose runs into the next tax year, you still need to pay future tax bills on time.
      """.stripMargin

        private def customAmountTextWelsh(amount: String) =
          s"""Yn seiliedig ar yr incwm sydd gennych dros ben, gallwch nawr ddewis gynllun talu. Bydd y taliad misol olaf yn eich cynllun yn fwy oherwydd y bydd yn cynnwys llog ac unrhyw dreth sy’n weddill sydd arnoch.
             |Os bydd y cynllun yr ydych yn ei ddewis yn rhedeg i mewn i’r flwyddyn dreth nesaf, bydd dal angen i chi dalu’ch biliau treth yn y dyfodol mewn pryd.
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
             |Enter an amount that is at least £612.50 but no more than £2,450
      """.stripMargin

        private val belowMinimumTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £612.50 ond sydd ddim mwy na £2,450
      """.stripMargin
      }

      object AboveMaximum {
        def apply()(implicit language: Language): String = language match {
          case English => aboveMaximumTextEnglish
          case Welsh   => aboveMaximumTextWelsh
        }

        private val aboveMaximumTextEnglish =
          s"""There is a problem
             |Enter an amount that is at least £612.50 but no more than £2,450
      """.stripMargin

        private val aboveMaximumTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £612.50 ond sydd ddim mwy na £2,450
      """.stripMargin
      }

      object NonNumeric {
        def apply()(implicit language: Language): String = language match {
          case English => nonNumericTextEnglish
          case Welsh   => nonNumericTextWelsh
        }

        private val nonNumericTextEnglish =
          s"""There is a problem
             |Enter an amount that is at least £612.50 but no more than £2,450
      """.stripMargin

        private val nonNumericTextWelsh =
          s"""Mae problem wedi codi
             |Nodwch swm sydd o leiaf £612.50 ond sydd ddim mwy na £2,450
      """.stripMargin
      }
    }
  }
}
