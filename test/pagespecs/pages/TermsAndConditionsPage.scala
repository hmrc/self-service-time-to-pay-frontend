/*
 * Copyright 2022 HM Revenue & Customs
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
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
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
          |This plan does not cover your future tax bills. You will still need to pay your future bills in full and on time.
          |
          |You need to contact HMRC if:
          |
          |anything changes that you think affects your payment plan
          |you need to change your payment plan
          |You can write to us about your Direct Debit:
          |
          |Debt Management
          |Self Assessment
          |HM Revenue and Customs
          |BX9 1AS
          |United Kingdom
          |Declaration
          |
          |I agree to the terms and conditions of this payment plan. I confirm that this is the earliest I am able to settle this debt.
          |
          |Agree and continue
        """.stripMargin
      private val mainTextWelsh =
        """Telerau ac amodau
          |Gallwn ganslo’r cytundeb hwn os:
          |
          |ydych yn talu’n hwyr neu’n methu taliad
          |ydych yn talu bil treth arall yn hwyr
          |nad ydych yn cyflwyno’ch Ffurflenni Treth yn y dyfodol mewn pryd
          |Os byddwn yn canslo’r cytundeb hwn, bydd yn rhaid i chi dalu’r cyfanswm sydd arnoch ar unwaith.
          |Gallwn ddefnyddio unrhyw ad-daliadau y gallech eu cael i dalu’ch taliadau treth.
          |
          |Nid yw’r cynllun hwn yn cwmpasu’ch biliau treth yn y dyfodol. Bydd dal angen i chi dalu eich biliau yn y dyfodol yn llawn ac mewn pryd.
          |
          |Bydd angen i chi gysylltu â CThEF os yw unrhyw un o’r canlynol yn berthnasol:
          |
          |mae unrhyw beth yn newid ac rydych o’r farn ei fod yn effeithio ar eich cynllun talu
          |mae angen i chi newid eich cynllun talu
          |Gallwch ysgrifennu atom ynglŷn â’ch Debyd Uniongyrchol:
          |
          |Rheolaeth Dyledion
          |Hunanasesiad
          |HGwasanaeth Cwsmeriaid Cymraeg CThEF
          |HMRC
          |BX9 1ST
          |Datganiad
          |
          |Cytunaf â thelerau ac amodau’r cynllun talu hwn. Cadarnhaf mai dyma’r cynharaf y gallaf setlo’r ddyled hon.
          |
          |Cytuno ac yn eich blaen
        """.stripMargin
    }
  }
}
