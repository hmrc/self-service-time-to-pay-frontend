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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor4}
import pagespecs.pages.BasePage
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{aYearAgo, almostAYearAgo, unactivatedSaEnrolment}
import uk.gov.hmrc.selfservicetimetopay.models._

class IneligiblePagesSpec extends ItSpec with TableDrivenPropertyChecks {
  /**
   * spec to test all of the ineligible pages
   *
   * Ineligible reasons:
   * - No debits
   * - Not on IA
   * - No sa enrolment (not enrolled)
   * - Not submitted return (Current year)
   * - Previous Tax Returns Not Submitted
   * - Total amount > £10k
   * - Old debt < £32
   * - debt less than £32
   */
  val listOfIneligibleReasons: TableFor4[String, Reason, String, BasePage] = Table(
    ("reason", "reasonObject", "pageAsString", "page"),
    ("no debts", NoDebt, "general call us page", generalCallUsPage),
    //    ("not on IA", IsNotOnIa, "not on ia page", notOnIaPage),
    ("current year return not submitted", ReturnNeedsSubmitting, "you need to file", needToFilePage),
    ("debt more than £10k", TotalDebtIsTooHigh, "debt too large", debtTooLargePage),
    ("debt less than £32", DebtIsInsignificant, "debt too small", needToFilePage),
    ("old debt is more than £32", OldDebtIsTooHigh, "old debt too large", generalCallUsPage),
    ("direct debit(s) created within the last 12 months", DirectDebitCreatedWithinTheLastYear, "general call us page", generalCallUsPage)
  )

  def beginJourney(ineligibleReason: Reason): Unit = {
    AuthStub.authorise()
    TaxpayerStub.getTaxpayer(ineligibleReason)
    if (ineligibleReason == IsNotOnIa) IaStub.failedIaCheck
    else IaStub.successfulIaCheck
    GgStub.signInPage(port)
    getBanksIsSuccessful(if (ineligibleReason == DirectDebitCreatedWithinTheLastYear) almostAYearAgo else aYearAgo)
    startPage.open()
    startPage.clickOnStartNowButton()
    ()
  }

  "Ineligible pages displayed correctly - ssttp eligibility" - {
    TableDrivenPropertyChecks.forAll(listOfIneligibleReasons) { (reason, reasonObject, pageAsString, page) =>
      s"$pageAsString should be displayed when user has ineligible reason: [$reason]" in {
        beginJourney(reasonObject)
        page.assertPageIsDisplayed
        page.backButtonHref shouldBe None
      }

      s"OPS-5822: User can go back from the /call-us page (via browser back button) and try again but still end up on the /call-us page [$reason]" in {
        beginJourney(reasonObject)
        page.assertPageIsDisplayed

        webDriver.navigate().back()
        startPage.clickOnStartNowButton()

        // Bug OPS-5822 would have shown a technical difficulties page here rather than /call-us because of a
        // failure to deserialise the reason in the journey object in the db
        page.assertPageIsDisplayed
      }
    }
  }

  "authorisation based eligibility" - {

    "show you_need_to_request_access_to_self_assessment page no sa enrolments" in {
      AuthStub.authorise(allEnrolments = Some(Set()))
      TaxpayerStub.getTaxpayer()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      youNeedToRequestAccessToSelfAssessment.assertPageIsDisplayed
    }

    "show you_need_to_request_access_to_self_assessment page when the user has no activated sa enrolments" in {
      AuthStub.authorise(allEnrolments = Some(Set(unactivatedSaEnrolment)))
      TaxpayerStub.getTaxpayer()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      youNeedToRequestAccessToSelfAssessment.assertPageIsDisplayed
    }
  }
}
