/*
 * Copyright 2020 HM Revenue & Customs
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

import langswitch.Language
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

abstract class InstalmentSummaryPage(baseUrl: BaseUrl, paymentDayOfMonth: String)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary"

  val headingEnglish: String = "Check your payment plan"
  val headingWelsh: String = "Gwiriwch fanylion eich amserlen talu"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces()
    Expected.MainText(paymentDayOfMonth).stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
    title() shouldBe expectedTitle()
  }

  def clickInstalmentsChange(): Unit = {
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
      def apply(paymentDayOfMonth: String)(implicit language: Language): String = language match {
        case English => mainTextEnglish(paymentDayOfMonth)
        case Welsh   => mainTextWelsh(paymentDayOfMonth)
      }

      private def mainTextEnglish(paymentDayOfMonth: String) =
        s"""Check your payment plan
          |Upfront payment taken within 5 days
          |£0.00
          |Change Monthly payments
          |Payments collected on
          |$paymentDayOfMonth or next working day
          |Change
          |Monthly payments
          |December 2019
          |£1,633.00
          |January 2020
          |£1,633.00
          |February 2020
          |1,834.00
          |including interest of £200.00 added to the final payment
          |Change Monthly payments
          |Total to pay
          |£5,100.00
          |Continue
        """.stripMargin

      private def mainTextWelsh(paymentDayOfMonth: String) =
        s"""Gwiriwch fanylion eich amserlen talu
          |Taliad ymlaen llaw
          |£0.00
          |Newid Rhandaliadau misol
          |Dyddiad casglu rhandaliadau misol
          |$paymentDayOfMonth neu’r diwrnod gwaith nesaf
          |Newid
          |Rhandaliadau misol
          |December 2019
          |£1,633.00
          |January 2020
          |£1,633.00
          |February 2020
          |1,834.00
          |Wedi’u casglu dros £200.00 o fisoedd
          |Newid Rhandaliadau misol
          |Cyfanswm yr ad-daliad
          |£5,100.00
          |Yn eich blaen
        """.stripMargin
    }
  }
}

class InstalmentSummaryPageForPaymentDayOfMonth27th(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends InstalmentSummaryPage(baseUrl, "27th")(webDriver)

class InstalmentSummaryPageForPaymentDayOfMonth11th(baseUrl: BaseUrl)(implicit webDriver: WebDriver)
  extends InstalmentSummaryPage(baseUrl, "11th")(webDriver)
