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
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class InstalmentSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def clickInstalmentsChange() = {
    val changeLink = xpath("//*[@id=\"id_payment\"]/dl/div[1]/dd[2]/a")
    click on changeLink
  }

  def clickCollectionDayChange() = {
    val changeLink = xpath("//*[@id=\"content\"]/article/table/tbody/tr/td[3]/a")
    click on changeLink
  }

  def clickContinue() = {
    val button = xpath("//*[@id=\"content\"]/article/form/div/button")
    click on button
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish = """Set up a payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Back
          |Check your payment schedule details
          |Monthly instalments
          |Collected over 3 months
          |£300.00
          |Change Monthly instalments
          |Total interest
          |Added to final payment
          |£200.00
          |Total repayment
          |Including interest
          |£200.00
          |Monthly instalment collection date
          |25 or next working day Change
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Yn ôl
          |Gwiriwch fanylion eich amserlen talu
          |Rhandaliadau misol
          |Wedi’u casglu dros 3 o fisoedd
          |£300.00
          |Newid Rhandaliadau misol
          |Cyfanswm y llog
          |Wedi’i ychwanegu at y taliad terfynol
          |£200.00
          |Cyfanswm yr ad-daliad
          |Gan gynnwys llog
          |£200.00
          |Dyddiad casglu rhandaliadau misol
          |25 neu’r diwrnod gwaith nesaf Newid
          |Yn eich blaen
        """.stripMargin
    }

  }

}
