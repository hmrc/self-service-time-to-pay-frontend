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
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}
import testsupport.testdata.TdAll
import testsupport.testdata.TdAll.toLocalDate

import testsupport.testdata.TdAll.{address, communicationPreferences, debit1, debit1Amount, debit2, debit2Amount, dueDate, saUtr, taxYearEnd}
import timetopaytaxpayer.cor.model.{Debit, Return, SelfAssessmentDetails, Taxpayer}

import java.time.LocalDate

class CalculatorTaxLiabilitiesPageSpec extends ItSpec {

  private val debit1Amount = 2500
  private val debit2Amount = 3000

  val lateDebit1: Debit = Debit(originCode = "IN1", debit1Amount, dueDate = "2021-06-01", interest = None, taxYearEnd)
  val lateDebit2: Debit = Debit(originCode = "IN2", amount = debit2Amount, dueDate, interest = None, taxYearEnd)

  val taxpayerWithLateDebit: Taxpayer =
    Taxpayer(
      "Mr John Campbell",
      List(address),
      SelfAssessmentDetails(
        saUtr,
        communicationPreferences,
        List(debit1, debit2),
        List(
          Return(taxYearEnd, issuedDate = "2019-11-10", dueDate = "2019-08-15", receivedDate = "2019-03-09"),
          Return(taxYearEnd   = "2018-04-05", issuedDate = "2017-02-15", dueDate = "2018-01-31", receivedDate = "2018-03-09"))))

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()
    startPage.open()
    startPage.clickOnStartNowButton()
  }

  "language" in {
    beginJourney()
    taxLiabilitiesPage.assertPageIsDisplayed

    taxLiabilitiesPage.clickOnWelshLink()
    taxLiabilitiesPage.assertPageIsDisplayed(Welsh)

    taxLiabilitiesPage.clickOnEnglishLink()
    taxLiabilitiesPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    beginJourney()
    taxLiabilitiesPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttpeligibility.routes.SelfServiceTimeToPayController.start()}")
  }

  "continue to payment-today" in {
    beginJourney()
    taxLiabilitiesPage.assertPageIsDisplayed

    taxLiabilitiesPage.clickOnStartNowButton()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }
}
