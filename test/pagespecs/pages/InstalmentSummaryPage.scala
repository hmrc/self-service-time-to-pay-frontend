/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class InstalmentSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    //readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def clickInstalmentsChange =
    {
      val changeLink = xpath("//*[@id=\"id_payment\"]/dl/div[1]/dd[2]/a")
      click on changeLink
    }

  def clickCollectionDayChange =
    {
      val changeLink = xpath("//*[@id=\"content\"]/article/table/tbody/tr/td[3]/a")
      click on changeLink
    }

  object Expected {

    object MainText {
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """
          |BETA This is a new service – your feedback will help us to improve it.
          |English | Cymraeg
          |Back
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
          |Get help with this page.
        """.stripMargin

      private val mainTextWelsh =
        """
          |BETA Mae hwn yn wasanaeth newydd – bydd eich adborth yn ein helpu i'w wella.
          |English | Cymraeg
          |Yn ôl
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
          |Help gyda'r dudalen hon.
        """.stripMargin
    }

  }
  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }
}
