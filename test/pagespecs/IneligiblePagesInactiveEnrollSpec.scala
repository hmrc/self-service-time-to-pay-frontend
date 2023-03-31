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

import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor4}
import pagespecs.pages.BasePage
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub.getBanksIsSuccessful
import testsupport.stubs._
import testsupport.testdata.TdAll.{aYearAgo, almostAYearAgo}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.concurrent.{ExecutionContext, Future}
import testsupport.testdata.TdAll.unactivatedSaEnrolment

class IneligiblePagesInactiveEnrollSpec extends ItSpec with TableDrivenPropertyChecks {

  override val fakeAuthConnector: AuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set(Enrolment("IR-SA", Seq(), "Inactive", None))), Some("6573196998")), Some(Credentials("IR-SA", "")))
      )
      retrievalResult.map(_.asInstanceOf[A])
    }
  }

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

  "authorisation based eligibility" - {

    "show you_need_to_request_access_to_self_assessment page when the user has no activated sa enrolments" in {
      AuthStub.authorise(allEnrolments = Some(Set(unactivatedSaEnrolment)))
      TaxpayerStub.getTaxpayer()
      DirectDebitStub.getBanksIsSuccessful()
      GgStub.signInPage(port)
      startPage.open()
      startPage.clickOnStartNowButton()
      youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed
    }

  }
}
