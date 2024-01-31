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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor5}
import pagespecs.pages.BasePage
import play.api.libs.json.{JsObject, Json}
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{aYearAgo, almostAYearAgo}
import uk.gov.hmrc.selfservicetimetopay.models._

class IneligiblePagesSpec extends ItSpec with TableDrivenPropertyChecks {

  override val overrideConfig: Map[String, Any] = Map("auditing.enabled" -> true)

  /**
   * spec to test all of the ineligible pages
   *
   * Ineligible reasons:
   * - No debits
   * - No sa enrolment (not enrolled)
   * - Not submitted return (Current year)
   * - Previous Tax Returns Not Submitted
   * - Total amount > £10k
   * - Old debt < £32
   * - debt less than £32
   */
  val listOfIneligibleReasons: TableFor5[String, Reason, String, BasePage, String] = Table(
    ("reason", "reasonObject", "pageAsString", "page", "expectedTotalDebtAuditString"),
    ("no debts", NoDebt, "general call us page", generalCallUsPage, "0.00"),
    ("current year return not submitted", ReturnNeedsSubmitting, "you need to file", needToFilePage, "33.00"),
    ("debt more than £10k", TotalDebtIsTooHigh, "debt too large", debtTooLargePage, "30000.01"),
    ("debt less than £32", DebtIsInsignificant, "debt too small", needToFilePage, "31.00"),
    ("debt older than 60 days", OldDebtIsTooHigh, "debt too old", callUsDebtTooOld, "33.00"),
    ("direct debit(s) created within the last 12 months", DirectDebitCreatedWithinTheLastYear, "already have a plan", alreadyHaveAPlanPage, "4900.00")
  )

  def beginJourney(ineligibleReason: Reason): Unit = {
    AuditStub.audit()
    TaxpayerStub.getTaxpayer(ineligibleReason)
    GgStub.signInPage(port)
    getBanksIsSuccessful(if (ineligibleReason == DirectDebitCreatedWithinTheLastYear) almostAYearAgo else aYearAgo)

    startPage.open()
    startPage.clickOnStartNowButton()
  }

  "Ineligible pages displayed correctly - ssttp eligibility" - {
    TableDrivenPropertyChecks.forAll(listOfIneligibleReasons) { (reason, reasonObject, pageAsString, page, expectedTotalDebtAuditString) =>
      s"$pageAsString should be displayed when user has ineligible reason: [$reason]" in {
        beginJourney(reasonObject)
        page.assertInitialPageIsDisplayed
        page.backButtonHref shouldBe None

        AuditStub.verifyEventAudited(
          "EligibilityCheck",
          Json.parse(
            s"""
              |{
              |  "eligibilityResult" : "ineligible",
              |  "eligibilityReasons": [ "${reasonObject.name}" ],
              |  "authProviderId": "fakeAuthConnectorProviderId",
              |  "utr": "6573196998",
              |  "totalDebt": "$expectedTotalDebtAuditString",
              |  "taxType" : "SA"
              |}
              |""".stripMargin
          ).as[JsObject]
        )
      }

      s"OPS-5822: User can go back from the /call-us page (via browser back button) and try again but still end up on the /call-us page [$reason]" in {
        beginJourney(reasonObject)
        page.assertInitialPageIsDisplayed

        webDriver.navigate().back()
        startPage.clickOnStartNowButton()

        // Bug OPS-5822 would have shown a technical difficulties page here rather than /call-us because of a
        // failure to deserialise the reason in the journey object in the db
        page.assertInitialPageIsDisplayed
      }
    }
  }
}
