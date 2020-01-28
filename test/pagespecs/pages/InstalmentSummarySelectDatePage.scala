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
import org.scalatest.selenium.WebBrowser
import testsupport.RichMatchers._

class InstalmentSummarySelectDatePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary/select-date"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def assertErrorPageIsDisplayed(): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def selectFirstOption() =
    {
      val firstOption = xpath("/html/body/main/div[2]/article/form/div/div[1]/input")
      click on firstOption
    }

  def selectSecondOption() =
    {
      val secondOption = xpath("/html/body/main/div[2]/article/form/div/div[2]/input")
      click on secondOption
    }

  def clickContinue() = {
    val button = xpath("//*[@id=\"next\"]")
    click on button
  }

  def enterDay(day: String) =
    {
      val dayField = xpath("/html/body/main/div[2]/article/form/div/div[3]/input")
      click on dayField
      enter(day)
    }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = language match {
        case English => globalHeaderTextEnglish
        case Welsh   => globalHeaderTextWelsh
      }

      private val globalHeaderTextEnglish =
        """GOV.UK
          |Set up a payment plan
        """.stripMargin

      private val globalHeaderTextWelsh =
        """GOV.UK
          |Trefnu cynllun talu
        """.stripMargin
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Back
           |Choose the day you want your monthly payments collected
           |unchecked 28th or next working day
           |unchecked A different day
           |Enter the day of the month
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Yn ôl
           |Dewiswch y dydd yr hoffech i’ch taliadau misol gael eu casglu
           |unchecked 28ain diwrnod nesaf
           |unchecked Diwrnod gwahanol
           |Ewch i mewn i ddiwrnod y mis
           |Yn eich blaen
        """.stripMargin

    }
    object ErrorText {
      def apply(): String = errorText

      private def errorText =
        s"""Back
           |Choose the day you want your monthly payments collected
           |There is a problem
           |Enter a number between 1 and 28
           |unchecked 28th or next working day
           |unchecked A different day
           |Enter the day of the month
           |Continue
        """.stripMargin
    }
  }
}
