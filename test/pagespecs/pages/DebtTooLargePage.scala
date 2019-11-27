/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatest.Assertion
import org.scalatest.selenium.WebBrowser.tagName
import testsupport.WireMockSupport
import testsupport.RichMatchers._

class DebtTooLargePage(implicit webDriver: WebDriver) extends BasePage(WireMockSupport.baseUrl) {

  private val englishMainText =
    """BETA This is a new service – your feedback will help us to improve it.
      |English | Cymraeg
      |Back
      |Please call us
      |To be eligible to set up a payment plan online the tax you owe must be £10,000 or less.
      |For further support you can contact the Business Support Service and speak to an adviser on 0300 200 3835.
      |Before you call, make sure you have:
      |information on any savings or investments you have
      |your bank details
      |We're likely to ask:
      |what you've done to try to pay the bill
      |if you can pay some of the bill now
      |Our opening times are:
      |Monday to Friday: 8am to 8pm
      |<
      |Saturday and Sunday: 8am to 4pm
      |Get help with this page.""".stripMargin

  override def path: String = "/pay-what-you-owe-in-instalments/eligibility/debt-large/call-us"

  override def assertPageIsDisplayed(implicit lang: Language): Assertion = probing{
    readPath() shouldBe path
    readMain().stripSpaces shouldBe englishMainText
  }

  implicit class StringOps(s: String) {
    /**
     * Transforms string so it's easier it to compare.
     */
    def stripSpaces(): String = s
      .replaceAll("[^\\S\\r\\n]+", " ") //replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") //replace many consecutive new lines with one new line
      .split("\n").map(_.trim) //trim each line
      .filterNot(_ == "") //remove any empty lines
      .mkString("\n")

  }
}
