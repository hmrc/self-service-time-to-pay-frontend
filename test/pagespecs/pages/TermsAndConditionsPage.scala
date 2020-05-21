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

import langswitch.{Language, Languages}
import langswitch.Languages.{English, Welsh}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class TermsAndConditionsPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/terms-and-conditions"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val content = readMain().stripSpaces()
    Expected.MainText().stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Terms and conditions"
    case Languages.Welsh   => "Telerau ac amodau"
  }

  def clickContinue(): Unit = {
    val button = id("continue_button")
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
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Terms and conditions
          |We can cancel this agreement if you:
          |
          |pay late or miss a payment
          |pay another tax bill late
          |do not submit your future tax returns on time
          |If we cancel this agreement, you will need to pay the total amount you owe straight away.
          |
          |We can use any refunds you might get to pay off your tax charges.
          |
          |If your circumstances change and you can pay more or you can pay in full, you need to let us know.
          |
          |By continuing, you confirm this is the earliest you can settle the charges and you agree to the terms and conditions.
          |
          |Confirm and continue
        """.stripMargin
      private val mainTextWelsh =
        """Telerau ac amodau
          |Gallwn ganslo’r cytundeb hwn os:
          |
          |ydych yn talu’n hwyr neu’n methu taliad
          |ydych yn talu bil treth arall yn hwyr
          |nad ydych yn cyflwyno’ch Ffurflenni Treth yn y dyfodol mewn pryd
          |Os byddwn yn canslo’r cytundeb hwn, bydd yn rhaid i chi dalu’r cyfanswm sydd arnoch ar unwaith.
          |
          |Gallwn ddefnyddio unrhyw ad-daliadau y gallech eu cael i dalu’r ddyled hon yn ôl.
          |
          |Os bydd eich amgylchiadau’n newid, a gallwch dalu mwy neu gallwch dalu’n llawn, mae’n rhaid i chi roi gwybod i ni.
          |
          |Cytunaf â thelerau ac amodau’r cynllun talu hwn. Cadarnhaf mai dyma’r cynharaf y gallaf setlo’r ddyled hon.
          |
          |Cadarnhau ac yn eich blaen
        """.stripMargin
    }
  }
}
