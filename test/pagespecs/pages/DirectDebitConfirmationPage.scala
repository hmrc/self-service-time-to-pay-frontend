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

class DirectDebitConfirmationPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/direct-debit-confirmation"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Check your Direct Debit details"
    case Languages.Welsh   => "Gwiriwch fanylion eich Debyd Uniongyrchol"
  }

  def clickChangeButton(): Unit = {
    val button = id("change-name")
    click on button
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
      def apply(increase: Int = 0)(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Check your Direct Debit details
          |Name on the account
          |Mr John Campbell
          |Change
          |Sort code
          |12 - 34 - 56
          |Account number
          |12345678
          |You are covered by the Direct Debit Guarantee
          |The Direct Debit Guarantee
          |This Guarantee is offered by all banks and building societies that accept instructions to pay Direct Debits.
          |If there are any changes to the amount, date or frequency of your Direct Debit HMRC NDDS will notify you 10 working days in advance of your account being debited or as otherwise agreed. If you request HMRC NDDS to collect a payment, confirmation of the amount and date will be given to you at the time of the request.
          |If an error is made in the payment of your Direct Debit by HMRC NDDS or your bank or building society you are entitled to a full and immediate refund of the amount paid from your bank or building society. If you receive a refund you are not entitled to, you must pay it back when HMRC NDDS asks you to.
          |You can cancel a Direct Debit at any time by simply contacting your bank or building society. Written confirmation may be required. Please also notify us.
          |Continue
        """.stripMargin
      private val mainTextWelsh =
        """Gwiriwch fanylion eich Debyd Uniongyrchol
          |Yr enw sydd ar y cyfrif
          |Mr John Campbell
          |Newid
          |Cod didoli
          |12 - 34 - 56
          |Rhif y cyfrif
          |12345678
          |Rydych wedi’ch gwarchod gan y Warant Debyd Uniongyrchol
          |Y Warant Debyd Uniongyrchol
          |Cynigir y Warant hon gan bob banc a chymdeithas adeiladu sy’n derbyn cyfarwyddiadau i dalu Debydau Uniongyrchol.
          |Os oes unrhyw newidiadau i swm, dyddiad neu amlder eich Debyd Uniongyrchol, bydd NDDS CThEF yn rhoi gwybod i chi 10 diwrnod gwaith cyn i’ch cyfrif gael ei ddebydu, neu fel y cytunwyd fel arall. Os byddwch yn gwneud cais i NDDS CThEF gasglu taliad, rhoddir cadarnhad o’r swm a’r dyddiad i chi ar adeg y cais.
          |Os gwneir camgymeriad gan NDDS CThEF neu eich banc neu’ch cymdeithas adeiladu wrth dalu Debyd Uniongyrchol, mae gennych hawl i ad-daliad llawn a di-oed o’r swm a dalwyd o’ch banc neu’ch cymdeithas adeiladu. Os byddwch yn cael ad-daliad nad oes hawl gennych iddo, bydd yn rhaid i chi ei dalu’n ôl pan fydd NDDS CThEF yn gofyn i chi wneud hynny.
          |Gallwch ganslo Debyd Uniongyrchol ar unrhyw adeg drwy gysylltu â’ch banc neu’ch cymdeithas adeiladu. Efallai y bydd angen cadarnhad ysgrifenedig. Rhowch wybod i ni hefyd.
          |Yn eich blaen
        """.stripMargin
    }

  }

}
