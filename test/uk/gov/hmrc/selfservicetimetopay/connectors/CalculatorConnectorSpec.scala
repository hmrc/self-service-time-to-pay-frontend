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
import uk.gov.hmrc.selfservicetimetopay.config.{WSHttp, CalculatorConnector => realConnector}
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorInput, CalculatorPaymentSchedule}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CalculatorConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  object testConnector extends CalculatorConnector {
    val calculatorURL = ""
    val http: WSHttp = mock[WSHttp]
    val serviceURL = "paymentSchedule"
  }

  "CalculatorConnector" should {
    "use the correct calculator URL" in {
      realConnector.calculatorURL shouldBe "http://localhost:8888"
    }

    "use the correct service URL" in {
      realConnector.serviceURL shouldBe "paymentschedule"
    }

    "use the correct HTTP" in {
      realConnector.http shouldBe WSHttp
    }
  }

  "Calling submitLiabilities" should {
    "return a payment schedule" in {
      val jsonResponse = Json.fromJson[Option[Seq[CalculatorPaymentSchedule]]](submitLiabilitiesResponseJSON).get

      when(testConnector.http.POST[CalculatorInput, Option[Seq[CalculatorPaymentSchedule]]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = await(testConnector.submitLiabilities(submitDebitsRequest))

      result shouldBe defined
      result.get.size shouldBe 11
      result.get.head.initialPayment shouldBe BigDecimal("50")
      result.get.head.amountToPay shouldBe BigDecimal("5000")
      result.get.last.instalments.size shouldBe 12
    }

    "return no payment schedule" in {
      when(testConnector.http.POST[CalculatorInput, Option[Seq[CalculatorPaymentSchedule]]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future(None))

      val result = await(testConnector.submitLiabilities(submitDebitsRequest))

      result shouldBe None
    }
  }
}
