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
import pagespecs.pages.{CheckYourPaymentPlanPage, HowMuchCanYouPayEachMonthPage}
import ssttpcalculator.CalculatorType.PaymentOptimised
import ssttpcalculator.model.PaymentPlanOption
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll
import testsupport.testdata.TdAll.defaultRemainingIncomeAfterSpending

import java.time.LocalDate

class CheckYourPaymentPlanPageSpec extends CheckYourPaymentPlanPageBaseSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> PaymentOptimised.value
  )

  val pageUnderTest: CheckYourPaymentPlanPage = checkYourPaymentPlanPage
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage = howMuchCanYouPayEachMonthPage
}

trait CheckYourPaymentPlanPageBaseSpec extends ItSpec {

  val pageUnderTest: CheckYourPaymentPlanPage
  val inUseHowMuchCanYouPayEachMonthPage: HowMuchCanYouPayEachMonthPage

  def beginJourney(remainingIncomeAfterSpending: BigDecimal = defaultRemainingIncomeAfterSpending): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    DateCalculatorStub.stubAddWorkingDays(TdAll.localDateTime.toLocalDate.plusDays(10))

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

    startAffordabilityPage.assertInitialPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertInitialPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.assertInitialPageIsDisplayed
    yourMonthlyIncomePage.enterMonthlyIncome(remainingIncomeAfterSpending.toString)
    yourMonthlyIncomePage.clickContinue()

    addIncomeSpendingPage.assertPathHeaderTitleCorrect(English)
    addIncomeSpendingPage.clickOnAddChangeSpending()

    yourMonthlySpendingPage.assertInitialPageIsDisplayed
    yourMonthlySpendingPage.clickContinue()

    howMuchYouCouldAffordPage.clickContinue()
    inUseHowMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed
    inUseHowMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Basic)
    inUseHowMuchCanYouPayEachMonthPage.clickContinue()
  }

  "language" in {
    beginJourney()

    pageUnderTest.clickOnWelshLink()
    pageUnderTest.assertInitialPageIsDisplayed(Welsh)

    pageUnderTest.clickOnEnglishLink()

    pageUnderTest.assertInitialPageIsDisplayed(English)
  }

  "change monthly instalments" in {
    beginJourney()
    pageUnderTest.clickChangeMonthlyAmountLink()
    inUseHowMuchCanYouPayEachMonthPage.assertInitialPageIsDisplayed()
  }

  "change collection day" in {
    beginJourney()
    pageUnderTest.clickChangeCollectionDayLink()
    selectDatePage.assertInitialPageIsDisplayed
  }

  "change upfront payment amount" in {
    beginJourney()
    pageUnderTest.clickChangeUpfrontPaymentAnswerLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "change upfront answer" in {
    beginJourney()
    pageUnderTest.clickChangeUpfrontPaymentAmountLink()
    paymentTodayQuestionPage.assertInitialPageIsDisplayed
  }

  "continue to the next page" in {
    beginJourney()
    pageUnderTest.clickContinue()
    aboutBankAccountPage.assertInitialPageIsDisplayed
  }

  "shows warning" in {
    beginJourney()
    pageUnderTest.assertInitialPageIsDisplayed
    pageUnderTest.clickChangeMonthlyAmountLink()
    inUseHowMuchCanYouPayEachMonthPage.selectASpecificOption(PaymentPlanOption.Higher)
    inUseHowMuchCanYouPayEachMonthPage.clickContinue()
    pageUnderTest.assertWarningIsDisplayed(English)
    pageUnderTest.clickOnWelshLink()
    pageUnderTest.assertWarningIsDisplayed(Welsh)
  }
}
