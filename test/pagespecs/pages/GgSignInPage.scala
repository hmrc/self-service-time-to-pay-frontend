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
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._
import testsupport.WireMockSupport
import testsupport.stubs.GgStub

class GgSignInPage(implicit webDriver: WebDriver) extends BasePage(WireMockSupport.baseUrl) {
  import WebBrowser._

  override def path: String = GgStub.signInPath

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing{
    readPath() shouldBe path
    tagName("body").element.text shouldBe "Sign in using Government Gateway"
    ()
  }
}

