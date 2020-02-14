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
import testsupport.RichMatchers._

class NeedToFilePage (baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/you-need-to-file"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces
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
        """Back
          |File your return to use this service
          |To be eligible to set up a payment plan online you need to have filed your Self Assessment tax return. Once you have done this, you can return to the service.
          |
          |Go to your tax account to file your Self Assessment tax return.
        """
          .stripMargin

      private val mainTextWelsh =
        """
          |Ffoniwch ni
          |Cyflwynwch eich Ffurflen Dreth i ddefnyddio’r gwasanaeth hwn
          |I fod yn gymwys i drefnu cynllun talu ar-lein, mae’n rhaid eich bod wedi cyflwyno’ch Ffurflen Dreth Hunanasesiad. Pan fyddwch wedi gwneud hyn, gallwch ddychwelyd i’r gwasanaeth.
          |
          |Ewch i’ch cyfrif treth i gyflwyno’ch Ffurflen Dreth Hunanasesiad.
        """.stripMargin
    }

  }

}
