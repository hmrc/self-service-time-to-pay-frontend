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
import model.enumsforforms.{IsSoleSignatory, TypeOfBankAccount, TypesOfBankAccount}
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class AboutBankAccountPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {

  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/about-your-bank-account"

  override def assertInitialPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    pageTitle shouldBe expectedTitle(expectedHeadingContent(lang), lang)
    val expectedLines = Expected.MainText().stripSpaces().split("\n")
    assertContentMatchesExpectedLines(expectedLines)
  }

  def expectedHeadingContent(language: Language): String = language match {
    case Languages.English => "About your bank account"
    case Languages.Welsh   => "Ynglŷn â’ch cyfrif banc"
  }

  def selectTypeOfAccountRadioButton(businessOrPersonal: TypeOfBankAccount): Unit = {
    val businessRadioButton = xpath("//*[@id=\"typeOfAccount\"]")
    val personalRadioButton = xpath("//*[@id=\"typeOfAccount-2\"]")

    if (businessOrPersonal == TypesOfBankAccount.Business) click on businessRadioButton
    else click on personalRadioButton
  }

  def selectIsAccountHolderRadioButton(yesOrNo: IsSoleSignatory): Unit = {
    val yesRadioButton = xpath("//*[@id=\"isSoleSignatory\"]")
    val noRadioButton = xpath("//*[@id=\"isSoleSignatory-2\"]")

    if (yesOrNo == IsSoleSignatory.Yes) click on yesRadioButton
    else click on noRadioButton
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
      def apply()(implicit language: Language): String = language match {
        case English => mainTextEnglish
        case Welsh   => mainTextWelsh
      }

      private val mainTextEnglish =
        """About your bank account
          |What type of account details are you providing?
          |Business bank account
          |Personal bank account
          |
          |Are you the account holder?
          |You must be the payer and the only person required to authorise a Direct Debit from this account.
          |Yes
          |No
          |
          |Continue
        """.stripMargin
      private val mainTextWelsh =
        """Ynglŷn â’ch cyfrif banc
          |Pa fath o fanylion cyfrif yr ydych yn eu rhoi?
          |
          |Cyfrif banc busnes
          |Cyfrif banc personol
          |
          |Ai chi yw deiliad y cyfrif?
          |Mae’n rhaid mai chi yw’r talwr a’r unig berson sydd ei angen i awdurdodi Debyd Uniongyrchol o’r cyfrif hwn.
          |Iawn
          |Na
          |
          |Yn eich blaen
        """.stripMargin
    }
  }
}

