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

class InstalmentSummarySelectDatePage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/instalment-summary/select-date"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def assertPageIsDisplayed(checkState1: String, chechState2: String)(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText(checkState1, chechState2).stripSpaces()
  }

  def assertErrorPageIsDisplayed(): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def selectFirstOption =
    {
      val firstOption = xpath("/html/body/main/div[2]/article/form/div/div[1]/input")
      click on firstOption
    }

  def selectSecondOption =
    {
      val secondOption = xpath("/html/body/main/div[2]/article/form/div/div[2]/input")
      click on secondOption
    }

  def clickContinue = {
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
      def apply(checkState1: String = "unchecked", checkState2: String = "unchecked")(implicit language: Language): String = language match {
        case English => mainTextEnglish(checkState1, checkState2: String)
        case Welsh   => mainTextWelsh(checkState1, checkState2: String)
      }

      private def mainTextEnglish(checkState1: String, checkState2: String) =
        s"""
          |BETA This is a new service – your feedback will help us to improve it.
          |English | Cymraeg
          |Back
          |Choose the day you want your monthly payments collected
          |${checkState1}
          |28th or next working day
          |${checkState2}
          |A different day
          |Enter the day of the month
          |Continue
          |Get help with this page.
        """.stripMargin

      private def mainTextWelsh(checkState1: String, checkState2: String) =
        s"""
          |BETA Mae hwn yn wasanaeth newydd – bydd eich adborth yn ein helpu i'w wella.
          |English | Cymraeg
          |Yn ôl
          |Dewiswch y dydd yr hoffech i’ch taliadau misol gael eu casglu
          |${checkState1}
          |28ain diwrnod nesaf
          |${checkState2}
          |Diwrnod gwahanol
          |Ewch i mewn i ddiwrnod y mis
          |Yn eich blaen
          |Help gyda'r dudalen hon.
        """.stripMargin

    }
    object ErrorText {
      def apply(): String = errorText

      private def errorText =
        s"""BETA This is a new service – your feedback will help us to improve it.
           |English | Cymraeg
           |Back
           |Choose the day you want your monthly payments collected
           |There is a problem
           |Enter a number between 1 and 28
           |unchecked
           |28th or next working day
           |unchecked
           |A different day
           |Enter the day of the month
           |Continue
           |Get help with this page.
        """.stripMargin
    }
  }
}
