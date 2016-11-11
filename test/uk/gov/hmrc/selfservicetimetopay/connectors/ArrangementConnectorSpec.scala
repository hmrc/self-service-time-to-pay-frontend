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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArrangementConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  implicit val headerCarrier = HeaderCarrier()

  object testConnector extends ArrangementConnector {
    val arrangementURL = ""
    val http = mock[WSHttp]
    val serviceURL = "ttparrangements"
  }

  "ArrangementConnector" should {
    "use the correct arrangements URL" in {
      ArrangementConnector.arrangementURL shouldBe "http://localhost:8889"
    }
    "use the correct service URL" in {
      ArrangementConnector.serviceURL shouldBe "ttparrangements"
    }
    "use the correct HTTP" in {
      ArrangementConnector.http shouldBe WSHttp
    }
  }

  "Calling submitArrangements" should {
    "return true" in {
      val response = HttpResponse(CREATED)
      when(testConnector.http.POST[TTPArrangement, HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future(response))

      val result = await(testConnector.submitArrangements(submitArrangementResponse))

      result shouldBe true
    }
  }

  "return false" in {
    val response = HttpResponse(UNAUTHORIZED)
    when(testConnector.http.POST[TTPArrangement, HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future(response))

    val result = await(testConnector.submitArrangements(submitArrangementResponse))

    result shouldBe false
  }
}
