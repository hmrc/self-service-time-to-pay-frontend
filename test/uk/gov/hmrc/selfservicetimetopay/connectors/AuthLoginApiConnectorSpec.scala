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

import model.CalculatorPaymentSchedule
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import ssttpcalculator.CalculatorConnector
import testsupport.{ItSpec, UnitSpec}
import testsupport.testdata.TdAll
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.selfservicetimetopay.models.CalculatorInput
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthLoginApiConnectorSpec extends ItSpec with MockitoSugar {

  implicit val request = TdAll.request

  private val httpClient: HttpClient = mock[HttpClient]
  val testConnector = new CalculatorConnector(
    servicesConfig = mock[ServicesConfig],
    httpClient     = httpClient
  )

  "Calling submitLiabilities test only endpoint should" - {
    "return a payment schedule" in {
      val jsonResponse = Json.fromJson[Seq[CalculatorPaymentSchedule]](submitLiabilitiesResponseJSON).get

      when(httpClient.POST[CalculatorInput, Seq[CalculatorPaymentSchedule]](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future(jsonResponse))

      val result = testConnector.calculatePaymentSchedule(submitDebitsRequest).futureValue

      result.size shouldBe 11
      result.head.initialPayment shouldBe BigDecimal("50")
      result.head.amountToPay shouldBe BigDecimal("5000")
      result.last.instalments.size shouldBe 12
    }

    "return no payment schedule" in {
      when(httpClient.POST[CalculatorInput, Seq[CalculatorPaymentSchedule]](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future(Seq()))

      val result = testConnector.calculatePaymentSchedule(submitDebitsRequest).futureValue

      result shouldBe Seq()
    }
  }
}
