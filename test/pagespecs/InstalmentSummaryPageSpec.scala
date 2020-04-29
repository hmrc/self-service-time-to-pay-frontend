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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs._

class InstalmentSummaryPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.clickOnStartNowButton()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()
    monthlyPaymentAmountPage.enterAmount("2450")
    CalculatorStub.generateSchedules()
    monthlyPaymentAmountPage.clickContinue()
    calculatorInstalmentsPage.selectAnOption()
    calculatorInstalmentsPage.clickContinue()
    CalculatorStub.generateSchedules(paymentDayOfMonth      = 27, firstPaymentDayOfMonth = 28)
    instalmentSummarySelectDatePage.selectFirstOption()
    instalmentSummarySelectDatePage.clickContinue()
  }

  "language" in {
    beginJourney()

    instalmentSummaryPage.assertPageIsDisplayed

    instalmentSummaryPage.clickOnWelshLink()
    instalmentSummaryPage.assertPageIsDisplayed(Welsh)

    instalmentSummaryPage.clickOnEnglishLink()

    instalmentSummaryPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    instalmentSummaryPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttparrangement.routes.ArrangementController.getChangeSchedulePaymentDay()}")
  }

  "change monthly instalments" in {
    beginJourney()
    instalmentSummaryPage.clickInstalmentsChange()
    calculatorInstalmentsPage.assertPageIsDisplayed
  }

  "change collection day" in {
    beginJourney()
    instalmentSummaryPage.clickCollectionDayChange()
    instalmentSummarySelectDatePage.assertPageIsDisplayed
  }

  "change upfront payment" in {
    beginJourney()
    instalmentSummaryPage.clickUpfrontPaymentChange()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    instalmentSummaryPage.clickContinue()
    directDebitPage.assertPageIsDisplayed
  }

}
