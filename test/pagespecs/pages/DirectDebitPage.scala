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
import testsupport._
import testsupport.RichMatchers._

class DirectDebitPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import org.scalatestplus.selenium.WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/direct-debit"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Enter account details to set up a Direct Debit"
    case Languages.Welsh   => "Nodwch fanylion y cyfrif i drefnu Debyd Uniongyrchol"
  }

  def assertErrorPageIsDisplayed(implicit field: ErrorCase): Unit = probing {
    readPath() shouldBe path
    val expectedLines = Expected.ErrorText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def fillOutForm(accountNameInput: String, sortCodeInput: String, accountNumberInput: String): Unit = probing {
    click on xpath("//*[@id=\"accountName\"]")
    enter(accountNameInput)
    click on xpath("//*[@id=\"sortCode\"]")
    enter(sortCodeInput)
    click on xpath("//*[@id=\"accountNumber\"]")
    enter(accountNumberInput)
  }

  def clickContinue(): Unit = {
    clickOnContinue()
    //    val button = xpath("""//*[@id="content"]/article/form/div[2]/button""")
    //    click on button
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
      def apply(checkState1: String = "unchecked", checkState2: String = "unchecked")(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """Enter account details to set up a Direct Debit
          |Enter your banking details
          |Name on the account
          |Sort code
          |Account number
          |To continue you must be:
          |
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders, if there are any
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Nodwch fanylion y cyfrif i drefnu Debyd Uniongyrchol
          |Enter your banking details
          |Enw’r cyfrif
          |Cod didoli
          |Rhif y cyfrif
          |I fynd yn eich blaen, mae’n rhaid bod y canlynol yn wir:
          |rydych wedi’ch enwi’n ddeiliad y cyfrif ar gyfer y cyfrif hwn
          |rydych yn gallu sefydlu Debydau Uniongyrchol heb ganiatâd deiliaid eraill y cyfrif
          |Yn eich blaen
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
        """There is a problem
          |Check your account name is correct
          |Enter account details to set up a Direct Debit
          |Enter your banking details
          |Name on the account
          |Check your account name is correct
          |123ede23efr4efr4ew32ef3r4
          |Sort code 12-34-56
          |Account number 12345678
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders, if there are any
          |Continue
      """.stripMargin

      private val sortCodeErrorText =
        """There is a problem
          |Sort code must be a 6 digit number
          |Enter account details to set up a Direct Debit
          |Enter your banking details
          |Name on the account Mr John Campbell
          |Sort code
          |Sort code must be a 6 digit number
          |fqe23fwef322few23r
          |Account number 12345678
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders, if there are any
          |Continue
        """.stripMargin

      private val accountNumberErrorText =
        """There is a problem
          |Account number must be an 8 digit number
          |Enter account details to set up a Direct Debit
          |Enter your banking details
          |Name on the account Mr John Campbell
          |Sort code 12-34-56
          |Account number
          |Account number must be an 8 digit number
          |24wrgedf
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders, if there are any
          |Continue
        """.stripMargin

      private val invalidBankDetailsErrorText =
        """This isn't a valid bank account
          |Re-enter your bank details
          |Enter account details to set up a Direct Debit
          |Enter your banking details
          |Name on the account Mr John Campbell
          |Sort code 123456
          |Account number 12345678
          |To continue you must be:
          |a named account holder for this account
          |able to set up Direct Debits without permission from the other account holders, if there are any
          |Continue
        """.stripMargin
    }

  }

}
