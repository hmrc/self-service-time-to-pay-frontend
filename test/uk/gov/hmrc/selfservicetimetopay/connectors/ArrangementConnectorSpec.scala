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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status._
import ssttparrangement.{ArrangementConnector, SubmissionError, SubmissionSuccess}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ArrangementConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val httpClient: HttpClient = mock[HttpClient]
  val testConnector = new ArrangementConnector(
    servicesConfig = mock[ServicesConfig],
    httpClient     = httpClient
  )

  "Calling submitArrangements" should {
    "return Right(SubmissionSuccess())" in {
      val response = HttpResponse(CREATED)
      when(httpClient.POST[TTPArrangement, HttpResponse](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future(response))

      val result = await(testConnector.submitArrangements(submitArrangementResponse))

      result shouldBe Right(SubmissionSuccess())
    }

    "return Left(SubmissionError(401, \"Unauthorized\"))" in {
      when(httpClient.POST[TTPArrangement, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(Upstream4xxResponse("Unauthorized", UNAUTHORIZED, UNAUTHORIZED, Map())))

      val result = await(testConnector.submitArrangements(submitArrangementResponse))

      result shouldBe Left(SubmissionError(UNAUTHORIZED, "Unauthorized"))
    }
  }
}
