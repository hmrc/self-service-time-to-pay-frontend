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

package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.CalculatorDataGenerator

class MonthlyPaymentAmountPageSpec extends ItSpec {

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
  }

  "language" in {
    beginJourney()
    monthlyPaymentAmountPage.assertPageIsDisplayed

    monthlyPaymentAmountPage.clickOnWelshLink()
    monthlyPaymentAmountPage.assertPageIsDisplayed(Welsh)

    monthlyPaymentAmountPage.clickOnEnglishLink()
    monthlyPaymentAmountPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    monthlyPaymentAmountPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttpcalculator.routes.CalculatorController.getPayTodayQuestion()}")
  }

  "invalid entry" in {
    val value = "1"
    beginJourney()
    monthlyPaymentAmountPage.enterAmount(value)
    CalculatorDataGenerator.generateSchedules()
    monthlyPaymentAmountPage.clickContinue()
    monthlyPaymentAmountPage.assertErrorPageIsDisplayed(value)
  }

  "valid entry and continue" in {
    beginJourney()
    monthlyPaymentAmountPage.enterAmount("2000")
    CalculatorDataGenerator.generateSchedules()
    monthlyPaymentAmountPage.clickContinue()
    selectDatePage.assertPageIsDisplayed()
  }
}
