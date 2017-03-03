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

package uk.gov.hmrc.selfservicetimetopay.controllers.calculator

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.connectors.{CalculatorConnector, SessionCacheConnector}
import uk.gov.hmrc.selfservicetimetopay.controllers._
import uk.gov.hmrc.selfservicetimetopay.resources._

import scala.concurrent.Future

class AmountsDueControllerSpec extends PlayMessagesSpec with MockitoSugar {

  val mockSessionCache: SessionCacheConnector = mock[SessionCacheConnector]
  val mockCalculatorConnector: CalculatorConnector = mock[CalculatorConnector]

  val amountsDueForm = Seq(
    "amount" -> BigDecimal.valueOf(3000),
    "dueByYear" -> BigDecimal.valueOf(2017),
    "dueByMonth" -> BigDecimal.valueOf(1),
    "dueByDay" -> BigDecimal.valueOf(31)
  )

  "AmountsDueControllerSpec" should {
    val controller = new CalculatorController(messagesApi, mockCalculatorConnector) {
      override lazy val sessionCache: SessionCacheConnector = mockSessionCache
    }

    "redirect successfully to the amounts you owe page" in {
      when(mockSessionCache.get(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(ttpSubmission)))
      when(mockSessionCache.put(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mock[CacheMap]))

      val response = controller.start.apply(FakeRequest().withCookies(sessionProvider.createTtpCookie()))

      status(response) mustBe SEE_OTHER

      redirectLocation(response).get mustBe routes.CalculatorController.getAmountsDue().url
    }

    "successfully display the amounts you owe page" in {
      val request = FakeRequest().withCookies(sessionProvider.createTtpCookie())
      val response = controller.getAmountsDue().apply(request)

      status(response) mustBe OK
      contentAsString(response) must include(getMessages(request)("ssttp.calculator.form.amounts_due.title"))
    }

    "successfully add a valid amount due entry" in {
      //TODO implement the rest of the spec
    }

    "submit remove an amount due entry from the list" in {
    }

    "submit all amounts due page" in {
    }
  }
}
