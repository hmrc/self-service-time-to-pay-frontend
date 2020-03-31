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

package pagespecs.pages

import langswitch.Language
import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser
import testsupport.RichMatchers._

class AccessibilityStatementPage(baseUrl: BaseUrl)(implicit webDriver: WebDriver) extends BasePage(baseUrl) {
  import WebBrowser._

  override def path: String = "/pay-what-you-owe-in-instalments/accessibility-statement"

  override def assertPageIsDisplayed(implicit lang: Language): Unit = probing {
    readPath() shouldBe path
    readGlobalHeaderText().stripSpaces shouldBe Expected.GlobalHeaderText().stripSpaces
    val content = readMain().stripSpaces()
    Expected.MainText().stripSpaces().split("\n").foreach(expectedLine =>
      content should include(expectedLine)
    )
  }

  def clickOnAccessibilityStatementLink(): Unit = {
    click on linkText("Accessibility")
    val windowHandles = webDriver.getWindowHandles
    //Get rid of the current one
    windowHandles.remove(webDriver.getWindowHandle)
    val nextTab = windowHandles.iterator().next()

    switch to window(nextTab)
  }

  object Expected {

    object GlobalHeaderText {

      def apply()(implicit language: Language): String = {
        globalHeaderTextEnglish
      }
      private val globalHeaderTextEnglish = """Accessibility Statement"""
    }

    object MainText {
      def apply()(implicit language: Language): String = {
        mainTextEnglish
      }

      private val mainTextEnglish =
        """
          |Accessibility statement for set up a Self Assessment payment plan
          |This accessibility statement explains how accessible this service is, what to do if you have difficulty using it and how to report accessibility problems with the service.
          |This service is part of the wider GOV.UK website. There is a separate accessibility statement for the main GOV.UK website.
          |This statement only contains information about this service.
          |Using this service
          |This service allows you to set up a payment plan to pay overdue Self Assessment tax charges in instalments, if you are eligible.
          |This service is run by HM Revenue and Customs (HMRC). We want as many people as possible to be able to use this service. This means you should be able to:
          |•	change colours, contrast levels and fonts
          |•	zoom in up to 300% without the text spilling off the screen
          |•	get from the start of the service to the end using just a keyboard
          |•	get from the start of the service to the end using speech recognition software
          |•	listen to the service using a screen reader (including the most recent versions of JAWS, NVDA and VoiceOver)
          |We have also made the text in the service as simple as possible to understand.
          |AbilityNet has advice on making your device easier to use if you have a disability.
          |How accessible this service is
          |This service is partially compliant with the Web Content Accessibility Guidelines version 2.1 AA standard.
          |Some people may find parts of this service difficult to use:
          |•	some error messages are not descriptive enough for people with visual impairments - but are compatible with common assistive technology tools
          |•	some headings are not consistent
          |•	elements which are missing their labels - using a screen reader to read back the entire page will provide all the information needed
          |Reporting accessibility problems with this service
          |We are always looking to improve the accessibility of this service. If you find any problems that are not listed on this page or think we are not meeting accessibility requirements, report the accessibility problem.
          |What to do if you are not happy with how we respond to your complaint
          |The Equality and Human Rights Commission (EHRC) is responsible for enforcing the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018 (the ‘accessibility regulations’). If you are not happy with how we respond to your complaint, contact the Equality Advisory and Support Service (EASS), or the Equality Commission for Northern Ireland (ECNI) if you live in Northern Ireland.
          |Contacting us by phone or getting a visit from us in person
          |We provide a text relay service if you are deaf, hearing impaired or have a speech impediment.
          |We can provide a British Sign Language (BSL) interpreter, or you can arrange a visit from an HMRC advisor to help you complete the service.
          |Find out how to contact us.
          |Technical information about this service’s accessibility
          |HMRC is committed to making this service accessible, in accordance with the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018.
          |This service is partially compliant with the Web Content Accessibility Guidelines version 2.1 AA standard, due to the non-compliances listed below.
          |Non-accessible content
          |The content listed below is non-accessible for the following reasons.
          |Non-compliance with the accessibility regulations
          |This service uses radio buttons which are grouped so that the page can be navigated with only the keyboard
          |In some places, these groups are missing their labels which means screen reading software will not read out the description of the selected element. This does not meet WCAG success criterion 2.4.6 (Headings and Labels).
          |We plan to add the correct labels and field sets to all of our radio buttons by 30 September 2020.
          |We use tab titles to describe the content of our web pages. In some places, the tab titles do not match the heading of the page. This does not meet WCAG 2.1 success criterion 2.4.6 (Headings and Labels).
          |We plan to add consistent titles and headings for all the pages in the service by 30 September 2020.
          |Some error messages are not descriptive enough for people with visual impairments. This does not meet WCAG Success Criterion 3.3.3 (Error Suggestion)
          |The service is not fully compliant with WCAG 2.2.1 (timing adjustable) as there is not a warning for the service timeout.
          |We plan to meet this criterion by 30 September 2020.
          |How we tested this service
          |The service was last tested on 14 February 2019 and was checked for compliance with WCAG 2.1 AA.
          |The service was built using parts that were tested by the Digital Accessibility Centre. The full service was tested by HMRC and included disabled users.
          |This page was prepared on 26 February 2020. It was last updated on 27 February 2020.
        """.stripMargin
    }
  }
}
