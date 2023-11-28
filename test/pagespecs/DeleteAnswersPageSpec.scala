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

import langswitch.Languages.Welsh
import org.openqa.selenium.By.className
import org.scalatestplus.selenium.WebBrowser
import testsupport.ItSpec

class DeleteAnswersPageSpec extends ItSpec {

  "language" in {
    deleteAnswersPage.open()
    deleteAnswersPage.assertInitialPageIsDisplayed

    deleteAnswersPage.clickOnWelshLink()
    deleteAnswersPage.assertInitialPageIsDisplayed(Welsh)
  }

  "sign in button" in {
    deleteAnswersPage.open()
    deleteAnswersPage.assertHasSignInButton()

    deleteAnswersPage.clickOnWelshLink()
    deleteAnswersPage.assertHasSignInButton(Welsh)
  }

  "no back button" in {
    deleteAnswersPage.open()
    deleteAnswersPage.hasNoBackLink
  }

}