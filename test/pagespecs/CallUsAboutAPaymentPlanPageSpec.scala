
package pagespecs

import langswitch.Languages.{English, Welsh}
import testsupport.ItSpec
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful

class CallUsAboutAPaymentPlanPageSpec extends ItSpec {
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

    startAffordabilityPage.assertInitialPageIsDisplayed()
    startAffordabilityPage.clickContinue()

    addIncomeSpendingPage.assertInitialPageIsDisplayed()
    addIncomeSpendingPage.clickOnAddChangeIncome()

    yourMonthlyIncomePage.clickContinue()

  }
  "language" in {
    beginJourney()

    callUsAboutAPaymentPlanPage.assertInitialPageIsDisplayed

    callUsAboutAPaymentPlanPage.clickOnWelshLink()
    callUsAboutAPaymentPlanPage.assertInitialPageIsDisplayed(Welsh)

    callUsAboutAPaymentPlanPage.clickOnEnglishLink()
    callUsAboutAPaymentPlanPage.assertInitialPageIsDisplayed(English)
  }
  "back button" in {
    beginJourney()
    yourMonthlyIncomePage.backButtonHref shouldBe Some(s"${baseUrl.value}${yourMonthlyIncomePage.path}")
  }


}
