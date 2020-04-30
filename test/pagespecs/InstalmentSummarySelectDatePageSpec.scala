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

class InstalmentSummarySelectDatePageSpec extends ItSpec {

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
  }

  "language" in {
    beginJourney()
    instalmentSummarySelectDatePage.assertPageIsDisplayed(English)

    instalmentSummarySelectDatePage.clickOnWelshLink()
    instalmentSummarySelectDatePage.assertPageIsDisplayed(Welsh)

    instalmentSummarySelectDatePage.clickOnEnglishLink()
    instalmentSummarySelectDatePage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    instalmentSummarySelectDatePage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttpcalculator.routes.CalculatorController.getCalculateInstalments()}")
  }

  "enter an invalid day" in {
    beginJourney()
    instalmentSummarySelectDatePage.selectSecondOption()
    instalmentSummarySelectDatePage.enterDay("""123456""")
    instalmentSummarySelectDatePage.clickContinue()
    instalmentSummarySelectDatePage.assertErrorPageIsDisplayed()
  }

  "choose 28th or next working day and continue" in {
    beginJourney()
    CalculatorStub.generateSchedules(paymentDayOfMonth      = 27, firstPaymentDayOfMonth = 28)
    instalmentSummarySelectDatePage.assertPageIsDisplayed(English)
    instalmentSummarySelectDatePage.selectFirstOption()
    instalmentSummarySelectDatePage.clickContinue()
    instalmentSummaryPage.assertPageIsDisplayed
  }

  "choose a different day and continue" in {
    beginJourney()
    CalculatorStub.generateSchedules(paymentDayOfMonth      = 11, firstPaymentDayOfMonth = 12)
    instalmentSummarySelectDatePage.selectSecondOption()
    instalmentSummarySelectDatePage.enterDay("12")
    instalmentSummarySelectDatePage.clickContinue()
    instalmentSummaryPageForPaymentDayOfMonth11th.assertPageIsDisplayed
  }
}
