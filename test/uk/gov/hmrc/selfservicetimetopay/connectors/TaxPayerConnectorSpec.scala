/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDateTime, ZoneId}

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.{DefaultRunModeAppNameConfig, WSHttp}
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global

class TaxPayerConnectorSpec extends UnitSpec with MockitoSugar with ServicesConfig with WithFakeApplication with DefaultRunModeAppNameConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val clock: Clock = Clock.fixed(LocalDateTime.parse("2006-01-22T16:28:55.185", DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.of("Europe/London")).toInstant, ZoneId.of("UTC"))

  object testConnector extends TaxPayerConnector {
    val taxPayerURL = "time-to-pay-taxpayer"
    val http: WSHttp = mock[WSHttp]
    val serviceURL = "taxpayer"
  }

  "calling getTaxPayer" should {

    "return a valid taxpayer" in {
      val taxPayerResponse = Json.fromJson[Taxpayer](taxPayerJson).get
      val httpResponse = HttpResponse(201, Some(taxPayerJson))
      when(testConnector.http.GET[HttpResponse]("time-to-pay-taxpayer/taxpayer/testUTR")).thenReturn(httpResponse)

      val result = await(testConnector.getTaxPayer("testUTR"))
      assert(result.contains(taxPayerResponse))
    }

  }

  "getTaxpayer fixes returns" in {

    val taxPayer = Json.fromJson[Taxpayer](taxPayerJson).get
    val taxPayerWithFixedReturns = Json.fromJson[Taxpayer](taxPayerWithFixedReturnsJson).get

    taxPayer.fixReturns shouldBe taxPayerWithFixedReturns
  }
}
