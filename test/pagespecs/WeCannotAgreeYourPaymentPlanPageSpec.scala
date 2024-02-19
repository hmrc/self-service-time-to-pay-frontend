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
import testsupport.testdata.TdAll

class WeCannotAgreeYourPaymentPlanPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    TaxpayerStub.getTaxpayer()
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    DateCalculatorStub.stubAddWorkingDays(TdAll.localDateTime.toLocalDate.plusDays(10))
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

    startAffordabilityPage.assertInitialPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertInitialPageIsDisplayed()
  }

  "displays 'how much can you pay each month' page when income is higher than spending" in {
    beginJourney()

    addIncomeSpendingPage.assertInitialPageIsDisplayed

    addIncomeSpendingPage.enterIncome("2000")
    addIncomeSpendingPage.enterSpending("1000")

    howMuchYouCouldAffordPage.assertInitialPageIsDisplayed()

    howMuchYouCouldAffordPage.clickContinue()

    howMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed()
  }

  "displays kick-out page when income is equal to spending" - {
    "with working language toggle" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.enterIncome("2000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed()
      weCannotAgreeYourPaymentPlanPage.clickOnWelshLink()
      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed(Welsh)
    }
  }

  "displays kick-out page when income is lower than spending" - {
    "with working language toggle" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.enterIncome("10")
      addIncomeSpendingPage.enterSpending("2")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed()
      weCannotAgreeYourPaymentPlanPage.clickOnWelshLink()
      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed(Welsh)
    }
  }

  "displays kick-out page when the payment plan is longer than 24 months" - {
    "with working language toggle" in {
      beginJourney()

      addIncomeSpendingPage.assertInitialPageIsDisplayed

      addIncomeSpendingPage.clickOnWelshLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(Welsh)
      addIncomeSpendingPage.assertAddIncomeLinkIsDisplayed(Welsh)

      addIncomeSpendingPage.clickOnEnglishLink()
      addIncomeSpendingPage.assertInitialPageIsDisplayed(English)

      addIncomeSpendingPage.enterIncome("1000")
      addIncomeSpendingPage.enterSpending("2000")

      howMuchYouCouldAffordPage.clickContinue()

      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed
      weCannotAgreeYourPaymentPlanPage.clickOnWelshLink()
      weCannotAgreeYourPaymentPlanPage.assertInitialPageIsDisplayed(Welsh)
    }
  }

}
