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

import langswitch.Languages
import testsupport.ItSpec
import testsupport.stubs.{AuthStub, GgStub, TaxPayerForEligibilityStub, TaxpayerStub}
import uk.gov.hmrc.selfservicetimetopay.models.TotalDebtIsTooHigh

class StartPageSpec extends ItSpec {

  "language" in {

    startPage.open()
    startPage.assertPageIsDisplayed()

    startPage.clickOnWelshLink()
    startPage.assertPageIsDisplayed(Languages.Welsh)

    startPage.clickOnEnglishLink()
    startPage.assertPageIsDisplayed(Languages.English)

  }

  "unuthorised - missing bearer token (user not logged in)" in {
    AuthStub.unathorisedMissingSession()
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    ggSignInPage.assertPageIsDisplayed()
  }

  "eligible" in {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer()

    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
    taxLiabilitiesPage.assertPageIsDisplayed()
  }

  "not eligible (debt too large)" in {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer(TaxPayerForEligibilityStub.ineligibilityReasonToIneligibleTaxpayerMockMapping(TotalDebtIsTooHigh))
    startPage.open()
    startPage.clickOnStartNowButton()
    debtTooLargePage.assertPageIsDisplayed()
  }
}
