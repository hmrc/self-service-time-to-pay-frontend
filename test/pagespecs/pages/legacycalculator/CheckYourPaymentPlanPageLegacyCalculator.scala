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
import langswitch.Language
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import pagespecs.pages.{BaseUrl, CheckYourPaymentPlanPage}
import testsupport.RichMatchers._

class CheckYourPaymentPlanPageLegacyCalculator(
    baseUrl:                  BaseUrl,
    paymentDayOfMonthEnglish: String,
    paymentDayOfMonthWelsh:   String
)(implicit webDriver: WebDriver)
  extends CheckYourPaymentPlanPage(baseUrl, paymentDayOfMonthEnglish, paymentDayOfMonthWelsh)(webDriver) {

  import WebBrowser._

  override def assertInitialPageIsDisplayed(implicit lang: Language = English): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = ExpectedLegacyCalculator.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  object ExpectedLegacyCalculator {
    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private def mainTextEnglish: String = {
        s"""Check your payment plan
           |Can you make an upfront payment?
           |No
           |Change
           |Upfront payment
           |Taken within 10 working days
           |£0
           |Change
           |Monthly payments
           |Payments collected on
           |$paymentDayOfMonthEnglish or next working day
           |Change
           |10 monthly payments of
           |£490
           |Change the day of the month that payments will be collected on
           |Payment schedule
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
           |Estimated total interest
           |included in monthly payments
           |£74.30
           |Total to pay
           |£4,974.30
           |Agree and continue
        """.stripMargin
      }

      private def mainTextWelsh: String = {
        s"""Gwirio’ch cynllun talu
           |A allwch wneud taliad ymlaen llaw?
           |Na
           |£0
           |Taliad ymlaen llaw
           |I’w gymryd cyn pen 10 diwrnod gwaith
           |£0
           |Newid
           |Taliadau misol
           |Mae taliadau’n cael eu casglu ar
           |yr $paymentDayOfMonthWelsh neu’r diwrnod gwaith nesaf
           |Newid
           Rhagfyr 2019
           |£490
           |Ionawr 2020
           |£490
           |Chwefror 2020
           |£490
           |Mawrth 2020
           |£490
           |Ebrill 2020
           |£490
           |Mai 2020
           |£490
           |Mehefin 2020
           |£490
           |Gorffennaf 2020
           |£490
           |Awst 2020
           |£490
           |Medi 2020
           |£564.30
           |Amcangyfrif o gyfanswm y llog
           |wedi’i gynnwys yn y taliadau misol
           |£74.30
           |Y cyfanswm i’w dalu
           |£4,974.30
           |Cytuno ac yn eich blaen
        """.stripMargin
      }
    }
  }
}

class CheckYourPaymentPlanPageForPaymentDay28thOfMonthLegacyCalculator(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends CheckYourPaymentPlanPageLegacyCalculator(baseUrl, paymentDayOfMonthEnglish = "28th", paymentDayOfMonthWelsh = "28ain")(webDriver)

class CheckYourPaymentPlanPageForPaymentDay11thOfMonthLegacyCalculator(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends CheckYourPaymentPlanPageLegacyCalculator(baseUrl, paymentDayOfMonthEnglish = "11th", paymentDayOfMonthWelsh = "11eg")(webDriver)
