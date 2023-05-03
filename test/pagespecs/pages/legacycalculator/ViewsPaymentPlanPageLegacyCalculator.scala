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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser.pageTitle
import pagespecs.pages.{BaseUrl, ViewPaymentPlanPage}
import testsupport.RichMatchers._

class ViewsPaymentPlanPageLegacyCalculator(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends ViewPaymentPlanPage(baseUrl)(webDriver) {

  override def assertInitialPageIsDisplayed(implicit lang: Language = English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)

    val expectedLines = ExpectedWithLegacyCalculator.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  object ExpectedWithLegacyCalculator {

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh => mainTextWelsh
      }

      private val mainTextEnglish =
        """Your payment plan
          |Payment reference	123ABC123
          |Upfront payment amount
          |Monthly payments
          |Payments collected on
          |
          |December 2019
          |£490
          |January 2020
          |£490
          |February 2020
          |£490
          |March 2020
          |£490
          |April 2020
          |£490
          |May 2020
          |£490
          |June 2020
          |£490
          |July 2020
          |£490
          |August 2020
          |£490
          |September 2020
          |£564.30
          |
          |Estimated total interest
          |included in your plan
          |£74.30
          |
          |Total to pay
          |£4,974.30
          |
          |Print a copy of your payment plan
        """.stripMargin
      private val mainTextWelsh =
        """Eich cynllun talu
          |Cyfeirnod y taliad	123ABC123
          |Taliadau misol
          |Mae taliadau’n cael eu casglu ar	28ain neu ar y diwrnod gwaith nesaf
          |
          |Amcangyfrif o gyfanswm y llog
          |wedi’i gynnwys yn y taliadau misol
          |
          |Y cyfanswm i’w dalu
          |Argraffwch gopi o’ch cynllun talu
        """.stripMargin
    }
  }
}
