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

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.models.BankAccount
import scala.concurrent.ExecutionContext.Implicits.global

class SelfServiceOrchestrationConnectorSpec extends ConnectorSpec with WithFakeApplication with ServicesConfig {

  implicit val hc = HeaderCarrier()

  val orchConnector = new SelfServiceOrchestrationConnector {
    override lazy val serviceURL = baseUrl("self-service-time-to-pay")
    override val http: HttpGet = WSHttp
  }
  val validationURL = urlPathEqualTo("/validateAccount")

  "SelfServiceOrchestrationConnector" should {
    "pass a GET request with accountName to the SSTTP service and return a success response" in {

      val getRequest = get(validationURL)

      stubFor(getRequest.willReturn(
        aResponse().withStatus(200).withBody(
          """[{
            |    "sortCode": "123456",
            |    "accountNumber": "12345678",
            |    "accountName": "Joe Bloggs"
            |}, {
            |    "sortCode": "654321",
            |    "accountNumber": "87654321",
            |    "accountName": "Joe Bloggs"
            |}]""".stripMargin
        )
      ))

      val response = await(orchConnector.validateAccount("123456", "12345678", Some("Joe Bloggs")))

      verify(1, getRequestedFor(validationURL))

      response shouldBe a [Seq[_]]
      response.size shouldBe 2
    }

    "pass a GET request without accountName to the SSTTP service and return a success response" in {

      val getRequest = get(validationURL)

      stubFor(getRequest.willReturn(
        aResponse().withStatus(200).withBody(
          """[{
            |    "sortCode": "123456",
            |    "accountNumber": "12345678",
            |    "accountName": "Joe Bloggs"
            |}]""".stripMargin
        )
      ))

      val response = await(orchConnector.validateAccount("123456", "12345678", None))

      verify(1, getRequestedFor(validationURL))

      response shouldBe a [Seq[_]]
      response.size shouldBe 1
    }

    "pass a GET request without accountName to the SSTTP service and return a not found (emtpy) response" in {

      val getRequest = get(validationURL)

      stubFor(getRequest.willReturn(
        aResponse().withStatus(404).withBody(
          """[]""".stripMargin
        )
      ))

      val response = await(orchConnector.validateAccount("123456", "12345678", None))

      verify(1, getRequestedFor(validationURL))

      response shouldBe a [Seq[_]]
      response.size shouldBe 0
    }

    "pass a GET request without accountName to the SSTTP service and return a not found (alternatives) response" in {

      val getRequest = get(validationURL)

      stubFor(getRequest.willReturn(
        aResponse().withStatus(404).withBody(
          """[{
            |    "sortCode": "123456",
            |    "accountNumber": "12345678",
            |    "accountName": "Joe Bloggs"
            |}, {
            |    "sortCode": "654321",
            |    "accountNumber": "87654321",
            |    "accountName": "Joe Bloggs"
            |}]""".stripMargin
        )
      ))

      val response = await(orchConnector.validateAccount("123456", "12345678", None))

      verify(1, getRequestedFor(validationURL))

      response shouldBe a [Seq[_]]
      response.size shouldBe 2
    }
  }
}
