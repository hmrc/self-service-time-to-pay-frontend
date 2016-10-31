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

package uk.gov.hmrc.ssttp.connectors

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.ssttp.config.WSHttp
import uk.gov.hmrc.ssttp.models.{DirectDebitBank, DirectDebitInstructionPaymentPlan}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source


class DirectDebitConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  val mockHttp: WSHttp = mock[WSHttp]
  implicit val headerCarrier = HeaderCarrier()

  object testConnector extends DirectDebitConnector {
    val directDebitURL = ""
    val http = mock[HttpGet]
    val serviceURL = "direct-debits"
  }

  "DirectDebitConnector" should {
    "use the correct direct-debit URL" in {
      DirectDebitConnector.directDebitURL shouldBe "http://localhost:9876"
    }
    "use the correct service URL" in {
      DirectDebitConnector.serviceURL shouldBe "direct-debits"
    }
    "use the correct HTTP" in {
      DirectDebitConnector.http shouldBe WSHttp
    }
  }

  "Calling getBanksList" should {
    val jsonFile = Source.fromFile(s"test/uk/gov/hmrc/ssttp/resources/GetBanksResponse.json").mkString
    val responseJSON = Json.parse(jsonFile)

    "Return DirectDebitBank" in {

      val response = HttpResponse(Status.OK, Some(responseJSON))

      when(testConnector.http.GET[HttpResponse](any())(any(), any())).thenReturn(Future(response))

      val hc = new HeaderCarrier()
      val result = testConnector.getBanksList()(hc)

      ScalaFutures.whenReady(result) { r =>
        r shouldBe a[DirectDebitBank]
        r match {
          case directDebitBanks: DirectDebitBank =>
            directDebitBanks.processingDate shouldBe "2001-12-17T09:30:47Z"
            directDebitBanks.directDebitInstruction.head.sortCode.get shouldBe "123456"
        }
      }
    }
  }

  "Calling getInstructionPaymentPlan" should {
    val jsonFile = Source.fromFile(s"test/uk/gov/hmrc/ssttp/resources/GetDirectDebitInstructionPaymentPlanResponse.json").mkString
    val responseJSON = Json.parse(jsonFile)

    "Return DirectDebitInstructionPaymentPlan" in {
      val response = HttpResponse(Status.OK, Some(responseJSON))

      when(testConnector.http.GET[HttpResponse](any())(any(), any())).thenReturn(Future(response))

      val hc = new HeaderCarrier()
      val result = testConnector.getInstructionPaymentPlan()(hc)

      ScalaFutures.whenReady(result) { r =>
        r shouldBe a[DirectDebitInstructionPaymentPlan]
        r match {
          case directDebitInstructionPaymentPlan: DirectDebitInstructionPaymentPlan =>
            directDebitInstructionPaymentPlan.processingDate shouldBe "2001-12-17T09:30:47Z"
            directDebitInstructionPaymentPlan.directDebitInstruction.head.ddiReferenceNo.get shouldBe "ABCDabcd1234"
            directDebitInstructionPaymentPlan.paymentPlan.head.ppReferenceNo shouldBe "abcdefghij1234567890"
        }
      }
    }
  }
}
