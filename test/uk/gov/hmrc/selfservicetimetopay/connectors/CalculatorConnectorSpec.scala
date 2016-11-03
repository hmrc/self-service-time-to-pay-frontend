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
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.CalculatorPaymentSchedule
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CalculatorConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  implicit val headerCarrier = HeaderCarrier()

  object testConnector extends CalculatorConnector {
    val calculatorURL = ""
    val http = mock[WSHttp]
    val serviceURL = "paymentSchedule"
  }

  "CalculatorConnector" should {
    "use the correct calculator URL" in {
      CalculatorConnector.calculatorURL shouldBe "http://localhost:8888"
    }
    "use the correct service URL" in {
      CalculatorConnector.serviceURL shouldBe "paymentschedule"
    }
    "use the correct HTTP" in {
      CalculatorConnector.http shouldBe WSHttp
    }
  }

  "Calling submitLiabilities" should {
    "return CalculatorPaymentSchedule" in {
      val response = HttpResponse(Status.OK, Some(submitLiabilitiesResponse))
      when(testConnector.http.POST[JsValue, HttpResponse](any(), any(), any())(any(), any(), any())).thenReturn(Future(response))

      val result = testConnector.submitLiabilities(submitLiabilitiesRequest)

      ScalaFutures.whenReady(result) { r =>
        r shouldBe a[Some[List[CalculatorPaymentSchedule]]]
        r match {
          case paymentSchedule: Some[List[CalculatorPaymentSchedule]] =>
            paymentSchedule.get.size shouldBe 11
            paymentSchedule.get.head.initialPayment shouldBe BigDecimal("50")
            paymentSchedule.get.head.amountToPay shouldBe BigDecimal("5000")
            paymentSchedule.get.last.instalments.size shouldBe 12
        }
      }
    }
  }
}
