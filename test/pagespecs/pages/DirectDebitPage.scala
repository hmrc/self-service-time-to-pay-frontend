/*
 * Copyright 2023 HM Revenue & Customs
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
import testsupport._
import testsupport.RichMatchers._

class DirectDebitPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import org.scalatestplus.selenium.WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/direct-debit"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "Set up Direct Debit"
    case Languages.Welsh   => "Trefnu Debyd Uniongyrchol"
  }

  def assertErrorPageIsDisplayed(field: ErrorCase, language: Language = English): Unit = probing {
    readPath() shouldBe path
    val expectedLines = Expected.ErrorText(field, language).stripSpaces().split("\n")
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
        """Set up Direct Debit
          |Name on the account
          |Sort code
          |Must be 6 digits long
          |Account number
          |Must be between 6 and 8 digits long
          |Continue
        """.stripMargin

      private val mainTextWelsh =
        """Trefnu Debyd Uniongyrchol
          |Yr enw sydd ar y cyfrif
          |Cod didoli
          |Mae’n rhaid i hyn fod yn 6 digid o hyd
          |Rhif y cyfrif
          |Mae’n rhaid iddo fod rhwng 6 ac 8 digid o hyd
          |Yn eich blaen
        """.stripMargin
    }

    object ErrorText {
      def apply(field: ErrorCase, language: Language = English): String = field match {
        case AccountName()             => accountNameErrorText
        case SortCode()                => sortCodeErrorText
        case AccountNumber()           => accountNumberErrorText
        case InvalidBankDetails()      => invalidBankDetailsErrorText
        case SortCodeOnDenyList()      => sortCodeOnDenyListErrorText
        case DirectDebitNotSupported() => directDebitNotSupportedErrorText(language)
      }

      private val accountNameErrorText =
        """There is a problem
          |Check your account name is correct
          |Set up Direct Debit
          |Name on the account
          |Check your account name is correct
          |Sort code
          |Must be 6 digits long
          |Account number
          |Must be between 6 and 8 digits long
          |Continue
      """.stripMargin

      private val sortCodeErrorText =
        """There is a problem
          |Sort code must be a 6 digit number
          |Set up Direct Debit
          |Name on the account
          |Sort code
          |Must be 6 digits long
          |Account number
          |Must be between 6 and 8 digits long
          |Continue
        """.stripMargin

      private val accountNumberErrorText =
        """There is a problem
          |Account number must be between 6 and 8 digits
          |Set up Direct Debit
          |Name on the account
          |Sort code
          |Account number
          |Must be between 6 and 8 digits long
          |Continue
        """.stripMargin

      private val invalidBankDetailsErrorText =
        """There is a problem
          |Enter a valid combination of bank account number and sort code
          |Set up Direct Debit
          |Name on the account
          |Enter a valid combination of bank account number and sort code
          |Sort code
          |Must be 6 digits long
          |Account number
          |Must be between 6 and 8 digits long
          |Continue
        """.stripMargin

      private def directDebitNotSupportedErrorText(language: Language = English) = language match {
        case English => directDebitNotSupportedErrorEnglishText
        case Welsh   => directDebitNotSupportedErrorWelshText
      }

      private val directDebitNotSupportedErrorEnglishText =
        """There is a problem
          |You have entered a sort code which does not accept this type of payment. Check you have entered a valid sort code or enter details for a different account
          |Set up Direct Debit
          |Name on the account
          |Sort code
          |You have entered a sort code which does not accept this type of payment. Check you have entered a valid sort code or enter details for a different account
          |Account number
          |Continue
        """.stripMargin

      private val directDebitNotSupportedErrorWelshText =
        """Mae problem wedi codi
          |Rydych wedi nodi cod didoli nad yw’n derbyn y math hwn o daliad. Gwiriwch eich bod wedi nodi cod didoli dilys, neu nodwch fanylion ar gyfer cyfrif gwahanol
          |Trefnu Debyd Uniongyrchol
          |Yr enw sydd ar y cyfrif
          |Cod didoli
          |Rydych wedi nodi cod didoli nad yw’n derbyn y math hwn o daliad. Gwiriwch eich bod wedi nodi cod didoli dilys, neu nodwch fanylion ar gyfer cyfrif gwahanol
          |Rhif y cyfrif
          |Yn eich blaen
        """.stripMargin

      private val sortCodeOnDenyListErrorText =
        """Sorry, we’re experiencing technical difficulties
          |Please try again in a few minutes.
        """.stripMargin
    }

  }

}
