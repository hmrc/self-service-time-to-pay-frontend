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

import langswitch.Languages
import testsupport.ItSpec
import testsupport.stubs._
import testsupport.testdata.TdAll
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.Enrolment

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances}

class YouNeedToRequestAccessToSelfAssessmentPageSpec extends ItSpec {

  override val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

      val retrievalResult = Future.successful(
        new ~(new ~(Enrolments(Set.empty[Enrolment]), None), None)
      )
      (retrievalResult.map(_.asInstanceOf[A]))
    }
  }

  def begin(
      utr:           Option[SaUtr]          = Some(TdAll.saUtr),
      allEnrolments: Option[Set[Enrolment]] = Some(Set(TdAll.saEnrolment))
  ): Unit = {
    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    AuthStub.authorise(utr, allEnrolments)

    ()
  }

  private case class Scenario(
      allEnrolments: Option[Set[Enrolment]],
      maybeSaUtr:    Option[SaUtr],
      caseName:      String                 = ""
  )

  def begin(): Unit = {
    val s = requestSaScenarios.head
    begin(s.maybeSaUtr, s.allEnrolments)
  }

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

  "language" in {
    TaxpayerStub.getTaxpayer()
    DirectDebitStub.getBanksIsSuccessful()

    begin()
    startNowAndAssertRequestToSA()
    youNeedToRequestAccessToSelfAssessment.clickOnWelshLink()
    youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed(Languages.Welsh)
    youNeedToRequestAccessToSelfAssessment.clickOnEnglishLink()
    youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed(Languages.English)
  }

  "back button" in {
    begin()
    startNowAndAssertRequestToSA()
    youNeedToRequestAccessToSelfAssessment.backButtonHref.value shouldBe s"${baseUrl.value}${startPage.path}"
  }

  "take the user to request page" in {
    requestSaScenarios.foreach { s =>
      TaxpayerStub.getTaxpayer()
      DirectDebitStub.getBanksIsSuccessful()

      begin(s.maybeSaUtr, s.allEnrolments)

      startNowAndAssertRequestToSA()
    }
  }

  "click on the call to action and navigate call us page if auth sends no credentials/providerId" in {
    TaxpayerStub.getTaxpayer()
    DirectDebitStub.getBanksIsSuccessful()

    startPage.open()
    startPage.assertInitialPageIsDisplayed()
    AuthStub.authorise(allEnrolments = None, credentials = None)
    startPage.clickOnStartNowButton()
    youNeedToRequestAccessToSelfAssessment.assertInitialPageIsDisplayed()
    youNeedToRequestAccessToSelfAssessment.clickTheButton()
    notEnrolledPage.assertInitialPageIsDisplayed()
  }

  private implicit def toOption[T](t: T): Option[T] = Some(t)

  private implicit def toSet[T](t: T): Set[T] = Set(t)

  private implicit def toOptionSet[T](t: T): Option[Set[T]] = Some(Set(t))

}
