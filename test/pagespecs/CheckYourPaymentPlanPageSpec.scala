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
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.CalculatorDataGenerator

class CheckYourPaymentPlanPageSpec extends ItSpec {

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

    selectDatePage.assertPageIsDisplayed()
    selectDatePage.selectFirstOption28thDay()
    selectDatePage.clickContinue()

    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
    calculatorInstalmentsPage28thDay.selectAnOption()
    calculatorInstalmentsPage28thDay.clickContinue()

    checkYourPaymentPlanPage.assertPageIsDisplayed()
  }

  "language" in {
    beginJourney()

    checkYourPaymentPlanPage.clickOnWelshLink()
    checkYourPaymentPlanPage.assertPageIsDisplayed(Welsh)

    checkYourPaymentPlanPage.clickOnEnglishLink()

    checkYourPaymentPlanPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    checkYourPaymentPlanPage.backButtonHref shouldBe Some(s"${baseUrl.value}${calculatorInstalmentsPage28thDay.path}")
  }

  "change monthly instalments" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeMonthlyAmountLink()
    calculatorInstalmentsPage28thDay.assertPageIsDisplayed()
  }

  "change collection day" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeCollectionDayLink()
    selectDatePage.assertPageIsDisplayed
  }

  "change upfront payment" in {
    beginJourney()
    checkYourPaymentPlanPage.clickChangeUpfrontPaymentLink()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    checkYourPaymentPlanPage.clickContinue()
    aboutBankAccountPage.assertPageIsDisplayed
  }
}
