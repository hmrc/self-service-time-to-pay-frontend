/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{verify => wmVerify, _}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.inject.guice.GuiceApplicationBuilder

class DirectDebitConnectorSpec extends ConnectorSpec with ServicesConfig with WithFakeApplication {

  implicit override lazy val app = new GuiceApplicationBuilder().
    disable[com.kenshoo.play.metrics.PlayModule].build()

  object DirectDebitConnectorTest extends DirectDebitConnector with ServicesConfig {
    lazy val directDebitURL: String = WiremockHelper.url
    lazy val serviceURL = "direct-debit"
    lazy val http = WSHttp
  }


  implicit val headerCarrier = HeaderCarrier()

  object testConnector extends DirectDebitConnector {
    val directDebitURL = ""
    val http: WSHttp = mock[WSHttp]
    val serviceURL = "direct-debit"
  }

  "Calling getBanksList" should {
    val validationURL = urlPathMatching("/direct-debit/.*/banks")
    val getRequest = get(validationURL)

    "return populated list of DirectDebitBank" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.OK).withBody(
          """{
            |  "processingDate": "2001-12-17T09:30:47Z",
            |  "directDebitInstruction": [
            |    {
            |      "sortCode": "123456",
            |      "accountNumber": "12345678",
            |      "referenceNumber": "111222333",
            |      "creationDate": "2001-01-01"
            |    },
            |    {
            |      "sortCode": "654321",
            |      "accountNumber": "87654321",
            |      "referenceNumber": "444555666",
            |      "creationDate": "2001-01-01"
            |    }
            |  ]
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.getBanks(SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.directDebitInstruction.size shouldBe 2
      response.directDebitInstruction.head.accountNumber.getOrElse("fail") shouldBe "12345678"
      response.directDebitInstruction.last.referenceNumber.getOrElse("fail") shouldBe "444555666"
    }

    "return empty list of DirectDebitBank when no accounts found" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.OK).withBody(
          """{
            |  "processingDate": "2001-12-17T09:30:47Z",
            |  "directDebitInstruction": []
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.getBanks(SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.directDebitInstruction.isEmpty shouldBe true
      response.processingDate shouldBe "2001-12-17T09:30:47Z"
    }


    "return empty list when BP not found" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.NOT_FOUND).withBody(
          """{
            |
            |"reason": "BP not found",
            |
            |"reasonCode": "002"
            |
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.getBanks(SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.directDebitInstruction.isEmpty shouldBe true
    }
  }

  "Calling validateBank" should {
    "return valid bank details" in {
      val jsonResponse = Json.fromJson[BankDetails](getBankResponseJSON).get

      when(testConnector.http.GET[Either[BankDetails, DirectDebitBank]](any())(any(), any())).thenReturn(Future(Left(jsonResponse)))

      val result = await(testConnector.validateOrRetrieveAccounts("123456", "123435678", SaUtr("test")))

      result.isLeft shouldBe true
      val account = result.left.get

      account.sortCode.get shouldBe "123456"
      account.accountNumber.get shouldBe "12345678"
      account.bankAddress shouldBe Some(Address(addressLine1 = "", postcode = ""))
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

      result.right.get.processingDate shouldBe "2001-12-17T09:30:47Z"
      result.right.get.directDebitInstruction.head.ddiReferenceNo shouldBe Some("ABCDabcd1234")
      result.right.get.paymentPlan.head.ppReferenceNo shouldBe "abcdefghij1234567890"
    }
  }

  "Calling validateOrRetrieveAccounts method" should {
    val validationURL = urlPathMatching("/direct-debit/.*/bank")
    val getRequest = get(validationURL)

    "pass a GET request with accountName to the SSTTP service and return a success response" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.OK).withBody(
          """{
            |  "sortCode": "123456",
            |  "accountNumber": "12345678",
            |  "bankName": "Bank"
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.validateOrRetrieveAccounts("123456", "12345678", SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.isLeft shouldBe true
    }

    "pass a GET request without accountName to the SSTTP service and return a success response" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.OK).withBody(
          """{
            |  "sortCode": "123456",
            |  "accountNumber": "12345678"
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.validateOrRetrieveAccounts("123456", "12345678", SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.isLeft shouldBe true
    }

    "pass a GET request without accountName to the SSTTP service and return a not found (emtpy) response" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.NOT_FOUND).withBody(
          """{
            |  "processingDate": "2001-12-17T09:30:47Z",
            |  "directDebitInstruction": []
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.validateOrRetrieveAccounts("123456", "12345678", SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.isRight shouldBe true
      response.right.get.directDebitInstruction.size shouldBe 0
    }

    "pass a GET request without accountName to the SSTTP service and return a not found (alternatives) response" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.NOT_FOUND).withBody(
          """{
            |  "processingDate": "2001-12-17T09:30:47Z",
            |  "directDebitInstruction": [
            |    {
            |      "sortCode": "123456",
            |      "accountNumber": "12345678",
            |      "referenceNumber": "111222333",
            |      "creationDate": "2001-01-01"
            |    },
            |    {
            |      "sortCode": "654321",
            |      "accountNumber": "87654321",
            |      "referenceNumber": "444555666",
            |      "creationDate": "2001-01-01"
            |    }
            |  ]
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.validateOrRetrieveAccounts("123456", "12345678", SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.isRight shouldBe true
      response.right.get.directDebitInstruction.size shouldBe 2
    }

    "pass a GET request and return an empty list for BP not found" in {
      stubFor(getRequest.willReturn(
        aResponse().withStatus(Status.NOT_FOUND).withBody(
          """{
            |
            |"reason": "BP not found",
            |
            |"reasonCode": "002"
            |
            |}""".stripMargin
        )
      ))

      val response = await(DirectDebitConnectorTest.validateOrRetrieveAccounts("123456", "12345678", SaUtr("SAUTR")))

      wmVerify(1, getRequestedFor(validationURL))

      response.isRight shouldBe true
      response.right.get.directDebitInstruction.isEmpty shouldBe true
    }
  }

}
