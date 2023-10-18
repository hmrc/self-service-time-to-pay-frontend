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
import testsupport.stubs.{AuthStub, GgStub, TaxpayerStub}
import uk.gov.hmrc.selfservicetimetopay.models.TotalDebtIsTooHigh

class StartPageSpec extends ItSpec {
  "language" in {
    startPage.open()
    startPage.assertInitialPageIsDisplayed

    startPage.clickOnWelshLink()
    startPage.assertInitialPageIsDisplayed(Welsh)

    startPage.clickOnEnglishLink()
    startPage.assertInitialPageIsDisplayed(English)
  }

  "back button" in {
    startPage.open()
    startPage.backButtonHref shouldBe None
  }

  "eligible" in {
    TaxpayerStub.getTaxpayer()
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.assertInitialPageIsDisplayed
  }

  "not eligible (debt too large)" in {
    TaxpayerStub.getTaxpayer(TotalDebtIsTooHigh)
    getBanksIsSuccessful()

    startPage.open()
    startPage.clickOnStartNowButton()
    debtTooLargePage.assertInitialPageIsDisplayed
  }
}
