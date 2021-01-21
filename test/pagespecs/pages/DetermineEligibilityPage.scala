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
import testsupport.stubs.AddTaxesFeStub

/**
 * This page is an endpoint which might be called by ssttp-fe or add-taxes-fe.
 * This endpoint will send a redirect
 */
class DetermineEligibilityPage(implicit webDriver: WebDriver) extends BasePage(WireMockSupport.baseUrl) {

  override def path: String = "/pay-what-you-owe-in-instalments/arrangement/determine-eligibility"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = ()
}

