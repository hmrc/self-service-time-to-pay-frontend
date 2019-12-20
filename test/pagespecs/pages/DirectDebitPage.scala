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
import testsupport._
import testsupport.RichMatchers._

class DirectDebitPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/direct-debit"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing {
    readPath() shouldBe path
    readGlobalHeader().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    readMain().stripSpaces shouldBe Expected.MainText().stripSpaces()
  }

  def assertErrorPageIsDisplayed(implicit field: ErrorCase): Assertion = probing {
    readPath() shouldBe path
    readMain().stripSpaces shouldBe Expected.ErrorText().stripSpaces
  }

  def fillOutForm(accountNameInput: String, sortCodeInput: String, accountNumberInput: String) =
    {
      click on xpath("//*[@id=\"accountName\"]")
      enter(accountNameInput)
      click on xpath("//*[@id=\"sortCode\"]")
      enter(sortCodeInput)
      click on xpath("//*[@id=\"accountNumber\"]")
      enter(accountNumberInput)
    }

  def clickContinue =
    {
      val button = xpath("//*[@id=\"content\"]/article/form/div[2]/button")
      click on button
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
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Back
          |Enter account details to set up a Direct Debit
          |Account name
          |Sort code
          |Account number
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders
          |Continue
          |Get help with this page.
        """.stripMargin

      private val mainTextWelsh =
        """Yn ôl
          |Nodwch fanylion y cyfrif i drefnu Debyd Uniongyrchol
          |Enw’r cyfrif
          |Cod didoli
          |Rhif y cyfrif
          |I fynd yn eich blaen, mae’n rhaid bod y canlynol yn wir:
          |rydych wedi’ch enwi’n ddeiliad y cyfrif ar gyfer y cyfrif hwn
          |rydych yn gallu sefydlu Debydau Uniongyrchol heb ganiatâd deiliaid eraill y cyfrif
          |Yn eich blaen
          |Help gyda'r dudalen hon.
        """.stripMargin
    }

    object ErrorText {
      def apply()(implicit field: ErrorCase): String = field match {
        case AccountName()        => accountNameErrorText
        case SortCode()           => sortCodeErrorText
        case AccountNumber()      => accountNumberErrorText
        case InvalidBankDetails() => invalidBankDetailsErrorText
      }

      private val accountNameErrorText =
        """Back
          |Something you’ve entered isn’t valid
          |Check your account name is correct
          |Enter account details to set up a Direct Debit
          |Account name
          |Check your account name is correct
          |Sort code
          |Account number
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders
          |Continue
          |Get help with this page.
      """.stripMargin

      private val sortCodeErrorText =
        """Back
          |Something you’ve entered isn’t valid
          |Sort code must be a 6 digit number
          |Enter account details to set up a Direct Debit
          |Account name
          |Sort code
          |Sort code must be a 6 digit number
          |Account number
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders
          |Continue
          |Get help with this page.
        """.stripMargin

      private val accountNumberErrorText =
        """Back
          |Something you’ve entered isn’t valid
          |Account number must be an 8 digit number
          |Enter account details to set up a Direct Debit
          |Account name
          |Sort code
          |Account number
          |Account number must be an 8 digit number
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders
          |Continue
          |Get help with this page.
        """.stripMargin

      private val invalidBankDetailsErrorText =
        """Back
          |This isn't a valid bank account
          |Re-enter your bank details
          |Enter account details to set up a Direct Debit
          |Account name
          |Sort code
          |Account number
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders
          |Continue
          |Get help with this page.
        """.stripMargin
    }
  }
}
