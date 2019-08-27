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

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import sstttaxpayer.TaxPayerConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global

class TaxPayerConnectorSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpClient: HttpClient = mock[HttpClient]
  val testConnector = new TaxPayerConnector(
    servicesConfig = mock[ServicesConfig],
    http           = httpClient
  )

  "calling getTaxPayer" should {

    "return a valid taxpayer" in {
      val taxPayerResponse = Json.fromJson[Taxpayer](taxPayerJson).get
      val httpResponse = HttpResponse(201, Some(taxPayerJson))
      when(httpClient.GET[HttpResponse]("time-to-pay-taxpayer/taxpayer/testUTR")).thenReturn(httpResponse)

      val result = await(testConnector.getTaxPayer("testUTR"))
      assert(result.contains(taxPayerResponse))
    }

  }

}
