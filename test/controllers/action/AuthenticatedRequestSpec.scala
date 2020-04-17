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

package controllers.action

import org.scalatest.WordSpec
import testsupport.RichMatchers
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}

// test for diagnostics for OPS-4481 - remove if that ticket is done
class AuthenticatedRequestSpec extends WordSpec with RichMatchers {
  "hasActivatedSaEnrolment" should {
    "return true" when {
      "the SA enrolment exists and is activated" in {
        val request =
          new AuthenticatedRequest(
            TdAll.request, Enrolments(Set(Enrolment("IR-SA", Seq.empty, "Activated", None))), L200, None)

        request.hasActivatedSaEnrolment shouldBe true
      }
    }

    "return false" when {
      "the SA enrolment exists and is not activated" in {
        val request =
          new AuthenticatedRequest(
            TdAll.request, Enrolments(Set(Enrolment("IR-SA", Seq.empty, "Not Activated", None))), L200, None)

        request.hasActivatedSaEnrolment shouldBe false
      }

      "the SA enrolment does not exist" in {
        val request = new AuthenticatedRequest(TdAll.request, Enrolments(Set.empty), L200, None)

        request.hasActivatedSaEnrolment shouldBe false
      }
    }
  }
}
