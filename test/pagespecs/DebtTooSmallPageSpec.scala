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

import testsupport.ItSpec
import testsupport.Language.{English, Welsh}

class DebtTooSmallPageSpec extends ItSpec {

  "language" in {

    debtTooSmallPage.open()
    debtTooSmallPage.assertInitialPageIsDisplayed

    debtTooSmallPage.clickOnWelshLink()
    debtTooSmallPage.assertInitialPageIsDisplayed(Welsh)

    debtTooSmallPage.clickOnEnglishLink()
    debtTooSmallPage.assertInitialPageIsDisplayed(English)
  }

  "back button" in {
    debtTooSmallPage.open()
    debtTooSmallPage.backButtonHref shouldBe None
  }
}
