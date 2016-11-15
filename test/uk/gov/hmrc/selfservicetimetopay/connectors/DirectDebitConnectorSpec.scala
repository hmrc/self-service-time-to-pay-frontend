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

import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DirectDebitConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication {

  val mockHttp: WSHttp = mock[WSHttp]
  implicit val headerCarrier = HeaderCarrier()

  object testConnector extends DirectDebitConnector {
    val directDebitURL = ""
    val http = mock[WSHttp]
    val serviceURL = "direct-debits"
  }

  "DirectDebitConnector" should {
    "use the correct direct-debit URL" in {
      DirectDebitConnector.directDebitURL shouldBe "http://localhost:9854"
    }
    "use the correct service URL" in {
      DirectDebitConnector.serviceURL shouldBe "direct-debit"
    }
    "use the correct HTTP" in {
      DirectDebitConnector.http shouldBe WSHttp
    }
  }

  "Calling getBanksList" should {
    "return DirectDebitBank" in {

      val jsonResponse = Json.fromJson[DirectDebitBank](getBanksResponseJSON).get

      when(testConnector.http.GET[DirectDebitBank](any())(any(), any())).thenReturn(Future(jsonResponse))

      val saUtr = new SaUtr("test")
      val result = await(testConnector.getBanksList(saUtr))

      result shouldBe a[DirectDebitBank]
      result.processingDate shouldBe "2001-12-17T09:30:47Z"
      result.directDebitInstruction.head.sortCode shouldBe Some("123456")
      result.directDebitInstruction.head.ddiRefNo shouldBe None
    }
  }

  "Calling validateBank" should {
    "return valid bank details" in {
      val jsonResponse = Json.fromJson[BankDetails](getBankResponseJSON).get

      when(testConnector.http.GET[BankDetails](any())(any(), any())).thenReturn(Future(jsonResponse))

      val result = await(testConnector.getBank("123456", "123435678"))

      result shouldBe a[BankDetails]
      result.sortCode shouldBe "123456"
      result.accountNumber shouldBe "12345678"
      result.bankAddress shouldBe Some(Address("", "", "", "", "", ""))
    }
  }

  "Calling createPaymentPlan" should {
    "return DirectDebitInstructionPaymentPlan" in {
      val jsonResponse = Json.fromJson[DirectDebitInstructionPaymentPlan](createPaymentPlanResponseJSON).get

      when(testConnector.http.POST[PaymentPlanRequest, DirectDebitInstructionPaymentPlan](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val saUtr = SaUtr("test")
      val paymentPlanRequest = Json.fromJson[PaymentPlanRequest](createPaymentRequestJSON).get
      val result = await(testConnector.createPaymentPlan(paymentPlanRequest, saUtr))

      result shouldBe a[DirectDebitInstructionPaymentPlan]
      result.processingDate shouldBe "2001-12-17T09:30:47Z"
      result.directDebitInstruction.head.ddiRefNo shouldBe Some("ABCDabcd1234")
      result.paymentPlan.head.ppReferenceNo shouldBe "abcdefghij1234567890"
    }
  }
}
