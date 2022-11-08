/*
 * Copyright 2022 HM Revenue & Customs
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

class NotSoleSignatoryPageSpec extends ItSpec {

  "language" in {

    notSoleSignatoryPage.open()
    notSoleSignatoryPage.assertPageIsDisplayed

    notSoleSignatoryPage.clickOnWelshLink()
    notSoleSignatoryPage.assertPageIsDisplayed(Welsh)

    notSoleSignatoryPage.clickOnEnglishLink()
    notSoleSignatoryPage.assertPageIsDisplayed(English)
  }

  "back button" in {
    notSoleSignatoryPage.open()
    notSoleSignatoryPage.backButtonHref shouldBe Some(s"${baseUrl.value}${ssttpdirectdebit.routes.DirectDebitController.aboutBankAccount()}")
  }
}
