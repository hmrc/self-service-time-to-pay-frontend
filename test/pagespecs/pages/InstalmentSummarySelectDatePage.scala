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
import org.openqa.selenium.WebDriver
import org.scalatest.Assertion
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class InstalmentSummarySelectDatePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary/select-date"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Which day do you want to pay each month?"
    case Languages.Welsh   => "Dewiswch y dydd yr hoffech i’ch taliadau misol gael eu casglu"
  }

  def assertErrorPageIsDisplayed(): Unit = probing {
    readPath() shouldBe path
    val expectedLines = Expected.ErrorText().stripSpaces.split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def selectFirstOption28thDay(): Unit = {
    val firstOption = xpath("/html/body/main/div[2]/article/form/div/div[1]/input")
    click on firstOption
  }

  def selectSecondOption(): Unit = {
    val secondOption = xpath("/html/body/main/div[2]/article/form/div/div[2]/input")
    click on secondOption
  }

  def clickContinue(): Unit = {
    val button = xpath("//*[@id=\"next\"]")
    click on button
  }

  def enterDay(day: String): Unit = {
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

      private val globalHeaderTextEnglish = """Set up a Self Assessment payment plan"""

      private val globalHeaderTextWelsh = """Trefnu cynllun talu"""
    }

    object MainText {
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        s"""Which day do you want to pay each month?
           |28th or next working day
           |A different day
           |Enter a day between 1 and 28
           |Continue
        """.stripMargin

      private val mainTextWelsh =
        s"""Dewiswch y dydd yr hoffech i’ch taliadau misol gael eu casglu
           |28ain diwrnod nesaf
           |Diwrnod gwahanol
           |Ewch i mewn i ddiwrnod y mis
           |Yn eich blaen
        """.stripMargin

    }

    object ErrorText {
      def apply(): String = errorText

      private def errorText =
        s"""Which day do you want to pay each month?
           |There is a problem
           |Enter a number between 1 and 28
           |28th or next working day
           |A different day
           |Enter a day between 1 and 28
           |Continue
        """.stripMargin
    }

  }

}
