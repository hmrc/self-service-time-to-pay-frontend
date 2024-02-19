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
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class CheckYourPaymentPlanPage(baseUrl: BaseUrl, paymentDayOfMonthEnglish: String, paymentDayOfMonthWelsh: String)
  (implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/check-your-payment-plan"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces() shouldBe Expected.GlobalHeaderText().stripSpaces()
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Language.English => "Check your payment plan"
    case Language.Welsh   => "Gwirio’ch cynllun talu"
  }

  def clickChangeUpfrontPaymentAnswerLink(): Unit = {
    click on id("upfront-payment")
  }

  def clickChangeUpfrontPaymentAmountLink(): Unit = {
    click on id("upfront-payment-amount")
  }

  def clickChangeCollectionDayLink(): Unit = {
    click on id("collection-day")
  }

  def clickChangeMonthlyAmountLink(): Unit = {
    click on id("monthly-payment")
  }

  def clickContinue(): Unit = {
    clickOnContinue()
  }

  def clickUpfrontPaymentChange(): Unit = {
    val button = xpath("""//*[@id="id_payment"]/dl/div[1]/dd[2]/a""")
    click on button
  }

  def clickOnBackLink(): Unit = goBack()

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

      private def mainTextEnglish: String = {
        s"""Check your payment plan
           |Can you make an upfront payment?
           |No
           |Change
           |Upfront payment
           |Taken within 6 working days
           |£0
           |Change
           |Monthly payments
           |Payments collected on
           |$paymentDayOfMonthEnglish or next working day
           |Change
           |Change the day of the month that payments will be collected on
           |Payment plan
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
           |Included in your plan
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
           |I’w gymryd cyn pen 6 diwrnod gwaith
           |£0
           |Newid
           |Taliadau misol
           |Mae taliadau’n cael eu casglu ar
           |yr $paymentDayOfMonthWelsh neu’r diwrnod gwaith nesaf
           |Newid
           |Cynllun talu
           |Rhagfyr 2019
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
           |Yn gynwysedig yn eich cynllun
           |£74.30
           |Y cyfanswm i’w dalu
           |£4,974.30
           |Cytuno ac yn eich blaen
        """.stripMargin
      }
    }

    object WarningText {
      def apply()(implicit language: Language): String = language match {
        case English => warningTextEnglish
        case Welsh   => warningTextWelsh
      }
    }
  }

  def assertWarningIsDisplayed(implicit lang: Language): Unit = probing {
    val expectedLines = Expected.WarningText().splitIntoLines()
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  private def warningTextEnglish: String = s"""You are choosing a payment plan where your monthly payments are over half of your left over income.
      |Make sure you can afford to pay.
      """.stripMargin

  private def warningTextWelsh: String = s"""Rydych yn dewis cynllun talu lle mae’ch taliadau misol dros hanner eich incwm sydd dros ben.
      |Gwnewch yn siŵr eich bod yn gallu fforddio talu.
      """.stripMargin

}

class CheckYourPaymentPlanPageForPaymentDay28thOfMonth(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends CheckYourPaymentPlanPage(baseUrl, paymentDayOfMonthEnglish = "28th", paymentDayOfMonthWelsh = "28ain")(webDriver)

class CheckYourPaymentPlanPageForPaymentDay11thOfMonth(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends CheckYourPaymentPlanPage(baseUrl, paymentDayOfMonthEnglish = "11th", paymentDayOfMonthWelsh = "11eg")(webDriver)
