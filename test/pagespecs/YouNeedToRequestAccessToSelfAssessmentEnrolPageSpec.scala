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

import testsupport.{FakeAuthConnector, ItSpec}
import testsupport.stubs._
import testsupport.testdata.TdAll
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class YouNeedToRequestAccessToSelfAssessmentEnrolPageSpec extends ItSpec {

  override val fakeAuthConnector: FakeAuthConnector = new FakeAuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "")), "Activated", None))), None), Some(Credentials("authId-999", "")))
      )
      retrievalResult.map(_.asInstanceOf[A])
    }
  }

  def begin(): Unit = {
    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    ()
  }

  private case class Scenario(
      allEnrolments: Option[Set[Enrolment]],
      maybeSaUtr:    Option[SaUtr],
      caseName:      String                 = ""
  )

  def startNowAndAssertRequestToSA(): Unit = {
    startPage.clickOnStartNowButton()
    youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed()
  }

  private val requestSaScenarios = List(
    Scenario(TdAll.saEnrolment, None, "no UTR found"),
    Scenario(None, TdAll.saUtr, "no SA enrolment"),
    Scenario(None, None, "no SA enrolment nor UTR"),
    Scenario(TdAll.unactivatedSaEnrolment, TdAll.saUtr, "no active SA enrolment")
  )

  "click on the call to action and navigate to PTA" in {
    requestSaScenarios.foreach { _ =>
      val s = requestSaScenarios.head
      TaxpayerStub.getTaxpayer()
      DirectDebitStub.getBanksIsSuccessful()

      begin()
      startNowAndAssertRequestToSA()

      AddTaxesFeStub.enrolForSaStub(s.maybeSaUtr)
      AddTaxesFeStub.enrolForSaStubbedPage()

      youNeedToRequestAccessToSelfAssessment.clickTheButton()
      enrolForSaPage.assertInitialPageIsDisplayed()
    }
  }

  private implicit def toOption[T](t: T): Option[T] = Some(t)

  private implicit def toOptionSet[T](t: T): Option[Set[T]] = Some(Set(t))

}
