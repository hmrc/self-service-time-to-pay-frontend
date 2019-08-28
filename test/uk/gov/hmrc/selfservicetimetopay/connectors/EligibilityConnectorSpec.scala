/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.connectors

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import ssttpeligibility.EligibilityConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, ReturnNeedsSubmitting, SelfAssessment, TotalDebtIsTooHigh}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityConnectorSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val request: Request[_] = HeaderCarrier()

  private val httpClient: HttpClient = mock[HttpClient]
  val testConnector = new EligibilityConnector(
    httpClient     = httpClient,
    servicesConfig = mock[ServicesConfig]
  )

  "Calling checkEligibility" should {
    "return an eligible response" in {
      val jsonResponse = Json.fromJson[EligibilityStatus](checkEligibilityTrueResponse).get

      when(httpClient.POST[SelfAssessment, EligibilityStatus](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.checkEligibility(checkEligibilityTrueRequest, "1234567890"))

      result.eligible shouldBe true
    }

    "return an illegible response for turns needs failing" in {
      val jsonResponse = Json.fromJson[EligibilityStatus](checkEligibilityFalseResponseNotSubmitted).get

      when(httpClient.POST[SelfAssessment, EligibilityStatus](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.checkEligibility(checkEligibilityFalseRequest, "1234567890"))

      result.eligible shouldBe false
      result.reasons.contains(ReturnNeedsSubmitting) shouldBe true
    }
    "return an illegible response" in {
      val jsonResponse = Json.fromJson[EligibilityStatus](checkEligibilityFalseResponse).get

      when(httpClient.POST[SelfAssessment, EligibilityStatus](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.checkEligibility(checkEligibilityFalseRequest, "1234567890"))

      result.eligible shouldBe false
      result.reasons.contains(TotalDebtIsTooHigh) shouldBe true
    }
  }
}
