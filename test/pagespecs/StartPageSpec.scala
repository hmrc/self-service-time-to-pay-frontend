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
import uk.gov.hmrc.selfservicetimetopay.models.TotalDebtIsTooHigh

class StartPageSpec extends ItSpec {
  "language" in {

    startPage.open()
    startPage.assertPageIsDisplayed

    startPage.clickOnWelshLink()
    startPage.assertPageIsDisplayed(Welsh)

    startPage.clickOnEnglishLink()
    startPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    startPage.open()
    startPage.backButtonHref shouldBe None
  }

  "unauthorised - missing bearer token (user not logged in)" in {
    AuthStub.unathorisedMissingSession()
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    ggSignInPage.assertPageIsDisplayed
  }

  "eligible" in {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()
    IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful()

    fakeLoginPage.pretendLogin()
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.assertPageIsDisplayed
  }

  "not eligible (debt too large)" in {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer(TotalDebtIsTooHigh)
    IaStub.successfulIaCheck
    getBanksIsSuccessful()

    fakeLoginPage.pretendLogin()

    startPage.open()
    startPage.clickOnStartNowButton()
    debtTooLargePage.assertPageIsDisplayed
  }
}
