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

import testsupport.Language.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._

class InstalmentSummarySelectDatePageSpec extends ItSpec {

  def beginJourney(): Unit = {
    TaxpayerStub.getTaxpayer()
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

  }

  "language" in {
    beginJourney()
    selectDatePage.assertInitialPageIsDisplayed(English)

    selectDatePage.clickOnWelshLink()
    selectDatePage.assertInitialPageIsDisplayed(Welsh)

    selectDatePage.clickOnEnglishLink()
    selectDatePage.assertInitialPageIsDisplayed(English)
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
    // TODO [OPS-8650]: Update / remove with new journey
    //    beginJourney()
    //    CalculatorDataGenerator.generateSchedules(paymentDayOfMonth      = 27, firstPaymentDayOfMonth = 28)
    //    selectDatePage.assertPageIsDisplayed(English)
    //    selectDatePage.selectFirstOption28thDay()
    //    selectDatePage.clickContinue()
    //    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
  }

  "choose a different day and continue" in {
    // TODO [OPS-8650]: Update / remove with new journey
    //    beginJourney()
    //    CalculatorDataGenerator.generateSchedules(paymentDayOfMonth      = 11, firstPaymentDayOfMonth = 12)
    //    selectDatePage.selectSecondOption()
    //    selectDatePage.enterDay("11")
    //    selectDatePage.clickContinue()
    //    calculatorInstalmentsPage11thDay.assertPageIsDisplayed()
  }
}
