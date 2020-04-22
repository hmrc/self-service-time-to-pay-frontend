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

package ssttpdirectdebit

import java.time.LocalDate

import play.api.inject.Injector
import play.api.test.FakeRequest
import testsupport.ItSpec
import testsupport.stubs.DirectDebitStub
import testsupport.testdata.TdAll.saUtr
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.selfservicetimetopay.models.{DirectDebitBank, DirectDebitInstruction}

class DirectDebitConnectorSpec extends ItSpec {
  private def injector: Injector = app.injector

  private def connector: DirectDebitConnector = injector.instanceOf[DirectDebitConnector]

  "getBanks should return a DirectDebitBank" in {
    DirectDebitStub.getBanksIsSuccessful

    connector.getBanks(saUtr)(FakeRequest()).futureValue shouldBe
      DirectDebitBank(
        "2019-04-05",
        List(DirectDebitInstruction(
          Some("12-34-56"),
          Some("12345678"),
          Some("123456789"),
          Some(LocalDate.of(2019, 4, 5)),
          Some(true), Some("123ABC123"),
          Some("123ABC123"),
          Some("Mr John Campbell"))))
  }

  "getBanks should not tolerate a general 404" in {
    DirectDebitStub.getBanksReturns404(saUtr)

    intercept[Exception] {
      connector.getBanks(saUtr)(FakeRequest()).futureValue
    }.getCause.isInstanceOf[NotFoundException] shouldBe true
  }

  "getBanks should tolerate a 404 BP Not Found error" in {
    DirectDebitStub.getBanksReturns404BPNotFound(saUtr)

    connector.getBanks(saUtr)(FakeRequest()).futureValue shouldBe DirectDebitBank.none
  }
}
