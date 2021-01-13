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
import testsupport.stubs.{AddTaxesStub, AuthStub, IdentityVerificationStub}
import testsupport.testdata.{Scenario, TdAll}
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L100, L200, L50}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment}

class MdtpUpliftRedirectSpec extends ItSpec {

  def begin(
      utr:             Option[SaUtr]           = None,
      confidenceLevel: Option[ConfidenceLevel] = Some(ConfidenceLevel.L100),
      allEnrolments:   Option[Set[Enrolment]]  = None
  ): Unit = {
    startPage.open()
    startPage.assertPageIsDisplayed()
    AuthStub.authorise(utr, confidenceLevel, allEnrolments)

    ()
  }

  def begin(s: Scenario): Unit = {
    begin(s.maybeSaUtr, s.confidenceLevel, s.allEnrolments)
  }

  val upliftScenarios = List(
    Scenario(TdAll.saUtr, L100, TdAll.saEnrolment, "confidence level < 200"),
    Scenario(TdAll.saUtr, L50, TdAll.saEnrolment, "confidence level < 200")
  )

  "take the user to mdtp uplift" in {
    upliftScenarios.foreach { s =>
      begin(s.maybeSaUtr, s.confidenceLevel, s.allEnrolments)

      IdentityVerificationStub.mdtpUpliftStubbedPage()

      startPage.clickOnStartNowButton()
      mdtpUplift.assertPageIsDisplayed()
    }
  }

  private implicit def toOption[T](t: T): Option[T] = Some(t)
  private implicit def toSet[T](t: T): Set[T] = Set(t)
  private implicit def toOptionSet[T](t: T): Option[Set[T]] = Some(Set(t))
}

