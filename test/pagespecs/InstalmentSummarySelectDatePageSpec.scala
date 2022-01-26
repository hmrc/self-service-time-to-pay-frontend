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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.CalculatorDataGenerator

class InstalmentSummarySelectDatePageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.assertPageIsDisplayed()
    startPage.clickOnStartNowButton()

    taxLiabilitiesPage.assertPageIsDisplayed()
    taxLiabilitiesPage.clickOnStartNowButton()

    paymentTodayQuestionPage.assertPageIsDisplayed()
    paymentTodayQuestionPage.selectRadioButton(false)
    paymentTodayQuestionPage.clickContinue()

    monthlyPaymentAmountPage.assertPageIsDisplayed()
    monthlyPaymentAmountPage.enterAmount("2000")
    monthlyPaymentAmountPage.clickContinue()

  }

  "language" in {
    beginJourney()
    selectDatePage.assertPageIsDisplayed(English)

    selectDatePage.clickOnWelshLink()
    selectDatePage.assertPageIsDisplayed(Welsh)

    selectDatePage.clickOnEnglishLink()
    selectDatePage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    selectDatePage.backButtonHref shouldBe Some(s"${baseUrl.value}${monthlyPaymentAmountPage.path}")
  }

  "enter an invalid day" in {
    beginJourney()
    selectDatePage.selectSecondOption()
    selectDatePage.enterDay("""123456""")
    selectDatePage.clickContinue()
    selectDatePage.assertErrorPageInvalidNumberIsDisplayed()
    selectDatePage.assertSecondOptionIsChecked()
  }

  "enter no day" in {
    beginJourney()
    selectDatePage.selectSecondOption()
    selectDatePage.enterDay("")
    selectDatePage.clickContinue()
    selectDatePage.assertErrorPageNoDayIsDisplayed()
    selectDatePage.assertSecondOptionIsChecked()
  }

  "choose 28th or next working day and continue" in {
    beginJourney()
    CalculatorDataGenerator.generateSchedules(paymentDayOfMonth      = 27, firstPaymentDayOfMonth = 28)
    selectDatePage.assertPageIsDisplayed(English)
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
  }

  "choose a different day and continue" in {
    beginJourney()
    CalculatorDataGenerator.generateSchedules(paymentDayOfMonth      = 11, firstPaymentDayOfMonth = 12)
    selectDatePage.selectSecondOption()
    selectDatePage.enterDay("11")
    selectDatePage.clickContinue()
    calculatorInstalmentsPage11thDay.assertPageIsDisplayed()
  }
}
