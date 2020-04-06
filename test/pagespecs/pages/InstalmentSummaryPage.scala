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

class InstalmentSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces()
    Expected.MainText().stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
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
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Check your payment plan
          |Upfront payment taken within 5 days
          |£0.00
          |Change Monthly payments
          |Payments collected on
          |25th or next working day
          |Change
          |Monthly payments
          |August 2019
          |August 2019
          |August 2019
          |including interest of £200.00 added to the final payment
          |£300.00
          |£120.00
          |£250.00
          |Change Monthly payments
          |Total to pay
          |£200.00
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Gwiriwch fanylion eich amserlen talu
          |Taliad ymlaen llaw
          |£0.00
          |Newid Rhandaliadau misol
          |Dyddiad casglu rhandaliadau misol
          |25th neu’r diwrnod gwaith nesaf
          |Newid
          |Rhandaliadau misol
          |August 2019
          |August 2019
          |August 2019
          |Wedi’u casglu dros £200.00 o fisoedd
          |£300.00
          |£120.00
          |£250.00
          |Newid Rhandaliadau misol
          |Cyfanswm yr ad-daliad
          |£200.00
          |Yn eich blaen
        """.stripMargin
    }

  }

}
