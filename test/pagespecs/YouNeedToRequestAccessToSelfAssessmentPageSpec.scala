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

package pagespecs

import langswitch.Languages
import testsupport.ItSpec
import testsupport.stubs._
import testsupport.testdata.{Scenario, TdAll}
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L100, L200, L50}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment}

class YouNeedToRequestAccessToSelfAssessmentPageSpec extends ItSpec {

  def begin(
      utr:             Option[SaUtr]           = Some(TdAll.saUtr),
      confidenceLevel: Option[ConfidenceLevel] = Some(ConfidenceLevel.L100),
      allEnrolments:   Option[Set[Enrolment]]  = Some(Set(TdAll.saEnrolment))
  ): Unit = {
    startPage.open()
    startPage.assertPageIsDisplayed()
    AuthStub.authorise(utr, confidenceLevel, allEnrolments)

    ()
  }

  def begin(): Unit = {
    val s = requestSaScenarios.head
    begin(s.maybeSaUtr, s.confidenceLevel, s.allEnrolments)
  }

  def startNowAndAssertRequestToSA(): Unit = {
    startPage.clickOnStartNowButton()
    youNeedToRequestAccessToSelfAssessment.assertPageIsDisplayed()
  }

  val requestSaScenarios = List(
    Scenario(None, L100, TdAll.saEnrolment, "confidence level < 200 and not UTR found"),
    Scenario(TdAll.saUtr, L200, None, "no SA enrolment"),
    Scenario(None, L200, None, "no SA enrolment nor UTR"),
    Scenario(TdAll.saUtr, L200, TdAll.unactivatedSaEnrolment, "no active SA enrolment")
  )

  "language" in {
    begin()
    startNowAndAssertRequestToSA()
    youNeedToRequestAccessToSelfAssessment.clickOnWelshLink()
    youNeedToRequestAccessToSelfAssessment.assertPageIsDisplayed(Languages.Welsh)
    youNeedToRequestAccessToSelfAssessment.clickOnEnglishLink()
    youNeedToRequestAccessToSelfAssessment.assertPageIsDisplayed(Languages.English)
  }

  "back button" in {
    begin()
    startNowAndAssertRequestToSA()
    youNeedToRequestAccessToSelfAssessment.backButtonHref.value shouldBe s"${baseUrl.value}${startPage.path}"
  }

  "take the user to request page" in {
    requestSaScenarios.foreach { s =>
      begin(s.maybeSaUtr, s.confidenceLevel, s.allEnrolments)

      startNowAndAssertRequestToSA()
    }
  }

  "click on the call to action" in {
    requestSaScenarios.foreach { s =>
      begin(s.maybeSaUtr, s.confidenceLevel, s.allEnrolments)
      startNowAndAssertRequestToSA()

      AddTaxesStub.enrolForSaStub(s.maybeSaUtr)
      IdentityVerificationStub.identityVerificationStubbedPage()

      youNeedToRequestAccessToSelfAssessment.clickTheButton()
      identityVerificationPage.assertPageIsDisplayed()
    }
  }

  private implicit def toOption[T](t: T): Option[T] = Some(t)
  private implicit def toSet[T](t: T): Set[T] = Set(t)
  private implicit def toOptionSet[T](t: T): Option[Set[T]] = Some(Set(t))
}
