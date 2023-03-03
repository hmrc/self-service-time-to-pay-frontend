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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.DirectDebitTd
import testsupport.testdata.TdAll.saUtr
import testsupport.{AccountName, ItSpec, _}

class DirectDebitPageSpec extends ItSpec {

  /*
  * TODO
  *  This test works only when you run it from intellij.
  *  For some reasons it fails when you run it from sbt.
  *
  * Reporter completed abruptly with an exception after receiving event: SuiteCompleted(Ordinal(0, 18),DirectDebitPageSpec,pagespecs.DirectDebitPageSpec,Some(pagespecs.DirectDebitPageSpec),Some(24245),Some(MotionToSuppress),Some(TopOfClass(pagespecs.DirectDebitPageSpec)),None,None,pool-1-thread-1,1608054753002).
  * java.net.SocketException: Broken pipe (Write failed)
  * ....
  *
  * */
  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertInitialPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertInitialPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    selectDatePage.assertInitialPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed()
    howMuchCanYouPayEachMonthPage.selectAnOption()
    howMuchCanYouPayEachMonthPage.clickContinue()

    checkYourPaymentPlanPage.assertInitialPageIsDisplayed()
    checkYourPaymentPlanPage.clickContinue()

    directDebitPage.assertInitialPageIsDisplayed()
  }

  "language" ignore {
    beginJourney()

    directDebitPage.clickOnWelshLink()
    directDebitPage.assertInitialPageIsDisplayed(Welsh)

    directDebitPage.clickOnEnglishLink()
    directDebitPage.assertInitialPageIsDisplayed(English)
  }

  "back button" ignore {
    beginJourney()
    directDebitPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttparrangement.routes.ArrangementController.getCheckPaymentPlan()}")
  }

  "enter invalid Account Name" ignore {
    beginJourney()
    directDebitPage.fillOutForm("123ede23efr4efr4ew32ef3r4", DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(AccountName())
  }

  "enter invalid Sort Code " ignore {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, "fqe23fwef322few23r", DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(SortCode())
  }

  "enter invalid Account Number" ignore {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, "24wrgedfbgt423twergdfb")
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(AccountNumber())
  }

  "enter invalid bank account " ignore {
    beginJourney()
    directDebitPage.fillOutForm("Mr John Campbell", "12-34-56", "12345678")
    BarsStub.validateBankFail(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(InvalidBankDetails())
  }

  "enter invalid bank account - SortCodeOnDenyList " ignore {
    beginJourney()
    directDebitPage.fillOutForm("Mr John Campbell", "12-34-56", "12345678")
    BarsStub.validateBankFailSortCodeOnDenyList(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    directDebitPage.clickContinue()
    directDebitPage.assertErrorPageIsDisplayed(InvalidBankDetails())
  }

  val sortCode = "12-34-56"
  val accountNumber = "12345678"
  val accountName = "Mr John Campbell"

  "enter valid bank account " ignore {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksIsSuccessful()
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertInitialPageIsDisplayed()
  }

  "enter valid bank account given business partner not found succeeds" ignore {
    beginJourney()
    directDebitPage.fillOutForm(DirectDebitTd.accountName, DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    BarsStub.validateBank(DirectDebitTd.sortCode, DirectDebitTd.accountNumber)
    DirectDebitStub.getBanksBPNotFound(saUtr)
    directDebitPage.clickContinue()
    directDebitConfirmationPage.assertInitialPageIsDisplayed()
  }
}
