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
import testsupport.stubs.{AuthStub, GgStub, IaStub, TaxpayerStub}

class CalculatorTaxLiabilitiesPageSpec extends ItSpec {

  def beginJourney(): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
  }

  "language" in {
    beginJourney()
    taxLiabilitiesPage.assertPageIsDisplayed()

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
    taxLiabilitiesPage.assertPageIsDisplayed()

    taxLiabilitiesPage.clickOnStartNowButton()
    paymentTodayQuestionPage.assertPageIsDisplayed
  }
}
