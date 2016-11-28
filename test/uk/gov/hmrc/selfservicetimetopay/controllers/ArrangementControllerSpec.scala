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

package uk.gov.hmrc.selfservicetimetopay.controllers



import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.FrontendGlobal._
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class ArrangementControllerSpec extends UnitSpec
  with MockitoSugar with WithFakeApplication with ScalaFutures {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val authConnector = mock[AuthConnector]
  val ddConnector = mock[DirectDebitConnector]
  val arrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector = mock[TaxPayerConnector]
  val sessionCache = mock[SessionCache]

  val controller = new ArrangementController(
    ddConnector,
    arrangementConnector,
    sessionCache
  )


  "Self Service Time To Pay Arrangement Controller" should {
    "return success and display the application complete page" in {

      implicit val hc = new HeaderCarrier

      when(sessionCache.fetchAndGetEntry[TTPSubmission](Matchers.eq(sessionCacheKey))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(ttpSubmission)))

      when(ddConnector.createPaymentPlan(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(directDebitInstructionPaymentPlan))

      when(arrangementConnector.submitArrangements(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Right(SubmissionSuccess())))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit"))

      redirectLocation(response).get shouldBe controllers.routes.ArrangementController.applicationComplete().url
    }

    "redirect to start if no data in session cache" in {
      when(sessionCache.fetchAndGetEntry[TTPSubmission](Matchers.eq(sessionCacheKey))(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      val response = controller.submit().apply(FakeRequest("POST", "/arrangement/submit"))

      redirectLocation(response).get shouldBe controllers.routes.SelfServiceTimeToPayController.present().url

    }

  }
}
