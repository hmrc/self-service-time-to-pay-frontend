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
import testsupport.RichMatchers._

class ArrangementSummaryPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/summary"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces
    Expected.MainText().stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
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
        """Application successful
          |Direct Debit mandate reference:
          |123ABC123
          |Print your payment summary
          |What happens next?
          |HMRC will send you a letter within 5 days with your payment dates.
          |Your upfront payment will be taken in 3 to 5 days. Your next payment will be taken on 25th August 2019.
          |If you need to change your payment plan
          |Call the HMRC Helpline on 0300 322 7015.
          |What did you think of this service? (takes 30 seconds)
        """.stripMargin

      private val mainTextWelsh =
        """Cais yn llwyddiannus
          |Cyfeirnod mandad Debyd Uniongyrchol:
          |123ABC123
          |Argraffu crynodeb o’ch taliadau
          |Beth sy’n digwydd nesaf?
          |Byddwn yn anfon llythyr atoch cyn pen 5 diwrnod i gadarnhau’ch dyddiadau talu.
          |Your upfront payment will be taken in 3 to 5 days. Your next payment will be taken on 25th August 2019.
          |Os oes angen i chi newid eich cynllun talu
          |Ffoniwch Wasanaeth Cwsmeriaid Cymraeg CThEM ar 0300 200 1900.
          |Beth oedd eich barn am y gwasanaeth hwn? (mae’n cymryd 30 eiliad)
        """.stripMargin
    }

  }

}
