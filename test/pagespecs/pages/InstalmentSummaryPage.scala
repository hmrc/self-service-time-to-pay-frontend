/*
 * Copyright 2021 HM Revenue & Customs
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

abstract class InstalmentSummaryPage(baseUrl: BaseUrl, paymentDayOfMonthEnglish: String, paymentDayOfMonthWelsh: String)
                                    (implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Check your payment plan"
    case Languages.Welsh   => "Gwiriwch fanylion eich amserlen talu"
  }

  def clickInstalmentsChange(): Unit = probing {
    val changeLink = xpath("""//*[@id="id_payment"]/dl/div[3]/dd[2]/a""")
    click on changeLink
  }

  def clickCollectionDayChange(): Unit = {
    val changeLink = xpath("""//*[@id="id_payment"]/dl/div[2]/dd[2]/a""")
    click on changeLink
  }

  def clickContinue(): Unit = {
    val button = xpath("//*[@id=\"content\"]/article/form/div/button")
    click on button
  }

  def clickUpfrontPaymentChange(): Unit = {
    val button = xpath("""//*[@id="id_payment"]/dl/div[1]/dd[2]/a""")
    click on button
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
        case English => mainTextEnglish(paymentDayOfMonthEnglish)
        case Welsh   => mainTextWelsh(paymentDayOfMonthWelsh)
      }

      private def mainTextEnglish(paymentDayOfMonth: String): String = {
        val x = if (paymentDayOfMonth.equals("11th")) "14.18" else "21.60"
        val y = if (paymentDayOfMonth.equals("11th")) "2,464.18" else "2,471.60"
        val z = if (paymentDayOfMonth.equals("11th")) "4,914.18" else "4,921.60"

        s"""Check your payment plan
           |Upfront payment taken within 7 working days
           |£0.00
           |Change Monthly payments
           |Payments collected on
           |$paymentDayOfMonth or next working day
           |Change
           |Monthly payments
           |December 2019
           |£2,450.00
           |January 2020
           |£$y
           |including interest of £$x added to the final payment
           |Change Monthly payments
           |Total to pay
           |£$z
           |Continue
        """.stripMargin
      }

      private def mainTextWelsh(paymentDayOfMonth: String): String = {
        val x = if (paymentDayOfMonth.equals("11eg")) "14.18" else "21.60"
        val y = if (paymentDayOfMonth.equals("11eg")) "2,464.18" else "2,471.60"
        val z = if (paymentDayOfMonth.equals("11eg")) "4,914.18" else "4,921.60"

        s"""Gwiriwch fanylion eich amserlen talu
           |Taliad ymlaen llaw
           |£0.00
           |Newid Rhandaliadau misol
           |Dyddiad casglu rhandaliadau misol
           |$paymentDayOfMonth neu’r diwrnod gwaith nesaf
           |Newid
           |Rhandaliadau misol
           |Rhagfyr 2019
           |£2,450.00
           |Ionawr 2020
           |£$y
           |Wedi’u casglu dros £$x o fisoedd
           |Newid Rhandaliadau misol
           |Cyfanswm yr ad-daliad
           |£$z
           |Yn eich blaen
        """.stripMargin
      }
    }
  }
}

class InstalmentSummaryPageForPaymentDayOfMonth27th(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends InstalmentSummaryPage(baseUrl, paymentDayOfMonthEnglish = "28th",  paymentDayOfMonthWelsh = "28ain")(webDriver)

class InstalmentSummaryPageForPaymentDayOfMonth11th(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends InstalmentSummaryPage(baseUrl, paymentDayOfMonthEnglish = "11th", paymentDayOfMonthWelsh = "11eg")(webDriver)
