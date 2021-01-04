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

package controllers.action

import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeRequest
import testsupport.testdata.TdAll.{saEnrolment, unactivatedSaEnrolment}
import testsupport.testdata.TdRequest
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.auth.core.Enrolments

class AuthenticatedRequestSpec extends WordSpec with Matchers {
  import TdRequest._

  private val request = FakeRequest()
    .withSessionId()
    .withLangEnglish()
    .withAuthToken()
    .withAkamaiReputationHeader()
    .withRequestId()
    .withSessionId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  "hasActiveSaEnrolment" should {
    "return true" when {
      "the SA enrolment exists and is activated" in {
        new AuthenticatedRequest(request, Enrolments(Set(saEnrolment)), L200, None).hasActiveSaEnrolment shouldBe true
      }
    }

    "return false" when {
      "the SA enrolment exists and is not activated" in {
        new AuthenticatedRequest(
          request, Enrolments(Set(unactivatedSaEnrolment)), L200, None).hasActiveSaEnrolment shouldBe false
      }

      "the SA enrolment does not exist" in {
        new AuthenticatedRequest(request, Enrolments(Set.empty), L200, None).hasActiveSaEnrolment shouldBe false
      }
    }
  }
}
