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
import play.api.libs.json.{JsObject, Json}
import testsupport.{FakeAuthConnector, ItSpec}
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

  override val overrideConfig: Map[String, Any] = Map("auditing.enabled" -> true)

  override val fakeAuthConnector: FakeAuthConnector = new FakeAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set(Enrolment("IR-SA", Seq(), "Inactive", None))), Some("6573196998")), Some(Credentials("123", "GovernmentGateway")))
      )
      retrievalResult.map(_.asInstanceOf[A])
    }
  }

  "authorisation based eligibility" - {

    "show you_need_to_request_access_to_self_assessment page when the user has no activated sa enrolments" in {
      TaxpayerStub.getTaxpayer()
      DirectDebitStub.getBanksIsSuccessful()
      GgStub.signInPage(port)
      AuditStub.audit()

      startPage.open()
      startPage.clickOnStartNowButton()
      youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed

      AuditStub.verifyEventAudited(
        "EligibilityCheck",
        Json.parse(
          """
            |{
            |  "eligibilityResult" : "ineligible",
            |  "enrollmentReasons": "inactiveEnrolment",
            |  "utr": "6573196998",
            |  "authProviderId": "123",
            |  "taxType" : "SA"
            |}
            |""".stripMargin
        ).as[JsObject]
      )
    }

  }
}
