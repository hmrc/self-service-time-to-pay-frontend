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

import java.time.{Clock, LocalDateTime}

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs._

class MonthlyPaymentAmountPageSpec extends ItSpec {

  def beginJourney() =
    {
      AuthStub.authorise()
      TaxpayerStub.getTaxpayer()
      EligibilityStub.eligible()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      taxLiabilitiesPage.clickOnStartNowButton()
      paymentTodayQuestionPage.selectRadioButton(false)
      paymentTodayQuestionPage.clickContinue()
    }

  "language" in
    {
      beginJourney()
      monthlyPaymentAmountPage.assertPageIsDisplayed

      monthlyPaymentAmountPage.clickOnWelshLink()
      monthlyPaymentAmountPage.assertPageIsDisplayed(Welsh)

      monthlyPaymentAmountPage.clickOnEnglishLink()
      monthlyPaymentAmountPage.assertPageIsDisplayed(English)
    }

  "invalid entry" in
    {
      val value = "1"
      beginJourney()
      monthlyPaymentAmountPage.enterAmout(value)
      CalculatorStub.generateSchedule
      monthlyPaymentAmountPage.clickContinue()
      monthlyPaymentAmountPage.assertErrorPageIsDisplayed(value)

    }

  "valid entry and continue" in
    {
      beginJourney()
      monthlyPaymentAmountPage.enterAmout("2450")
      CalculatorStub.generateSchedule
      monthlyPaymentAmountPage.clickContinue()
      calculatorInstalmentsPage.assertPageIsDisplayed
    }
}