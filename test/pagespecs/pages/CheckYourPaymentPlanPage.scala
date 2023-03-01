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
import language.Dates
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

import java.time.LocalDate

class CheckYourPaymentPlanPage(baseUrl: BaseUrl, paymentDayOfMonthEnglish: String, paymentDayOfMonthWelsh: String)
  (implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/check-your-payment-plan"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
    ()
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Check your payment plan"
    case Languages.Welsh   => "Gwirio’ch cynllun talu"
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

  def clickOnBackButton(): Unit = click on id("back-link")

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
           |Taken within 10 working days
           |£0
           |Change
           |Monthly payments
           |Payments collected on
           |$paymentDayOfMonthEnglish or next working day
           |Change
           |21 monthly payments of
           |£250
           |Change the day of the month that payments will be collected on
           |Payment schedule
           |December 2019
           |£250
           |January 2020
           |£250
           |February 2020
           |£250
           |March 2020
           |£250
           |April 2020
           |£250
           |May 2020
           |£250
           |June 2020
           |£250
           |July 2020
           |£250
           |August 2020
           |£250
           |September 2020
           |£250
           |October 2020
           |£250
           |November 2020
           |£250
           |December 2020
           |£250
           |January 2021
           |£250
           |February 2021
           |£250
           |March 2021
           |£250
           |April 2021
           |£250
           |May 2021
           |£250
           |June 2021
           |£250
           |July 2021
           |£250
           |August 2021
           |£38.16
           |Estimated total interest
           |included in monthly payments
           |£5,038.16
           |Total to pay
           |£5,038.16
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
           |£250
           |Ionawr 2020
           |£250
           |Chwefror 2020
           |£250
           |Mawrth 2020
           |£250
           |Ebrill 2020
           |£250
           |Mai 2020
           |£250
           |Mehefin 2020
           |£250
           |Gorffennaf 2020
           |£250
           |Awst 2020
           |£250
           |Medi 2020
           |£250
           |Hydref 2020
           |£250
           |Tachwedd 2020
           |£250
           |Rhagfyr 2020
           |£250
           |Ionawr 2021
           |£250
           |Chwefror 2021
           |£250
           |Mawrth 2021
           |£250
           |Ebrill 2021
           |£250
           |Mai 2021
           |£250
           |Mehefin 2021
           |£250
           |Gorffennaf 2021
           |£250
           |Awst 2021
           |£38.16
           |Amcangyfrif o gyfanswm y llog
           |wedi’i gynnwys yn y taliadau misol
           |£5,038.16
           |Y cyfanswm i’w dalu
           |£5,038.16
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
    val expectedLines = Expected.WarningText().stripSpaces().split("\n")
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
