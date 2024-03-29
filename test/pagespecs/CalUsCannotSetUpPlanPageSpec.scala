/*
 * Copyright 2024 HM Revenue & Customs
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
import testsupport.stubs.{GgStub, TaxpayerStub}

class CalUsCannotSetUpPlanPageSpec extends ItSpec {
  def beginJourney(): Unit = {
    TaxpayerStub.taxpayerNotFound()
    GgStub.signInPage(port)

    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    startPage.clickOnStartNowButton()
  }

  "page should be shown" in {
    beginJourney()

    callUsCannotSetUpPlan.assertInitialPageIsDisplayed

    callUsCannotSetUpPlan.clickOnWelshLink()
    callUsCannotSetUpPlan.assertInitialPageIsDisplayed(Welsh)

    callUsCannotSetUpPlan.clickOnEnglishLink()
    callUsCannotSetUpPlan.assertInitialPageIsDisplayed(English)
  }

}
