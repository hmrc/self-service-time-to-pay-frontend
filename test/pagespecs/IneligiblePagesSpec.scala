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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2, TableFor3, TableFor4}
import pagespecs.pages.BasePage
import play.api.libs.json.JsObject
import testsupport.ItSpec
import testsupport.stubs._
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.ConfidenceLevel
import testsupport.testdata.EligibilityTaxpayerVariationsTd

class IneligiblePagesSpec extends ItSpec with TableDrivenPropertyChecks {

  /**
   * spec to test all of the ineligible pages
   *
   * Ineligible reasons:
   * - No debits
   * - Not on IA
   * - No sa enrolment (not enrolled)
   * - Confidence level < 200
   * - Not submitted return (Current year)
   * - Previous Tax Returns Not Submitted
   * - Total amount > £10k
   * - Old debt < £32
   * - debt less than £32
   */

  val listOfIneligibleReasons: TableFor4[String, JsObject, String, BasePage] = Table(
    ("reason", "jsObject", "pageAsString", "page"),
    ("no debts", TdAll.noDebits, "general call us page", generalCallUsPage)//,
   // ("not on IA", TdAll.notOnIa, "not on ia page", notOnIaPage),
   // ("current year return not submitted", TdAll.returnNotSubmitted, "you need to file", needToFilePage),
   // ("debt more than £10k", TdAll.totalDebtIsTooHigh, "debt too large", debtTooLargePage),
   // ("debt less than £32", TdAll.debtTooSmall, "debt too small", needToFilePage)
  )

  def beginJourney(ineligibleReason: JsObject): Unit = {
    AuthStub.authorise()
    //TODO need to make it get a taxpayer with that ineligble reason and this should be the fix
    //need to make it use the right taxpayer each time...
    //will require some kind of mechanism
    //TaxpayerStub.getTaxpayer()
    TaxpayerStub.getTaxpayer(utr = String, EligibilityTaxpayerVariationsTd.zeroDebtTaxpayer)
    //EligibilityStub.ineligible(reasonJson = ineligibleReason)
    GgStub.signInPage(port)
    startPage.open()
    startPage.clickOnStartNowButton()
  }

  "Ineligible pages displayed correctly - ssttp eligibility" - {
    TableDrivenPropertyChecks.forAll(listOfIneligibleReasons) { (reason, json, pageAsString, page) =>
      s"$pageAsString should be displayed when user has ineligible reason: [$reason]" in {
        beginJourney(json)
        page.assertPageIsDisplayed
      }
    }
  }

  "authorisation based eligibility" - {
    "show not-enrolled page for confidence level < 200" in {
      AuthStub.authorise(confidenceLevel = Some(ConfidenceLevel.L100))
      TaxpayerStub.getTaxpayer()
      EligibilityStub.eligible()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      notEnrolledPage.assertPageIsDisplayed
    }
    "show not-enrolled page no sa enrolments" in {
      AuthStub.authorise(allEnrolments = Some(Set()))
      TaxpayerStub.getTaxpayer()
      EligibilityStub.eligible()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      notEnrolledPage.assertPageIsDisplayed
    }
  }

}
