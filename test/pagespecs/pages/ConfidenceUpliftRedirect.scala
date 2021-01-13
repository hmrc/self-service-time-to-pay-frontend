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

package pagespecs.pages

import langswitch.Language
import org.openqa.selenium.WebDriver
import testsupport.RichMatchers._
import testsupport.stubs.IdentityVerificationStub

class ConfidenceUpliftRedirect(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import org.scalatestplus.selenium.WebBrowser._

  override def path: String = IdentityVerificationStub.mdtpUpliftPagePath

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path

    ()
  }

}
