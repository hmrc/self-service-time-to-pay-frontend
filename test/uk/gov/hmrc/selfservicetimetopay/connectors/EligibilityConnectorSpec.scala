/*
 * Copyright 2016 HM Revenue & Customs
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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, SelfAssessment}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  object testConnector extends EligibilityConnector {
    val eligibilityURL: String = ""
    val http = mock[WSHttp]
    val serviceURL = "eligibility"
  }

  "EligibilityConnector" should {
    "use the correct eligibility URL" in {
      EligibilityConnector.eligibilityURL shouldBe "http://localhost:9856"
    }
    "use the correct service URL" in {
      EligibilityConnector.serviceURL shouldBe "eligibility"
    }
    "use the correct HTTP" in {
      EligibilityConnector.http shouldBe WSHttp
    }
  }

  "Calling checkEligibility" should {
    "return an eligible response" in {
      val jsonResponse = Json.fromJson[EligibilityStatus](checkEligibilityTrueResponse).get

      when(testConnector.http.POST[SelfAssessment, EligibilityStatus](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.checkEligibility(checkEligibilityTrueRequest))

      result.eligible shouldBe true
    }
    "return an illegible response" in {
      val jsonResponse = Json.fromJson[EligibilityStatus](checkEligibilityFalseResponse).get

      when(testConnector.http.POST[SelfAssessment, EligibilityStatus](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.checkEligibility(checkEligibilityFalseRequest))

      result.eligible shouldBe false
    }
  }

}
