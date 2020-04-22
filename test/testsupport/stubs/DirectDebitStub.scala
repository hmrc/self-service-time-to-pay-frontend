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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.Matchers
import play.api.libs.json.Json.prettyPrint
import testsupport.testdata.{DirectDebitTd, TdAll}
import timetopaytaxpayer.cor.model.SaUtr

object DirectDebitStub extends Matchers {

  def getBank(port: Int, sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/direct-debit/bank")).withRequestBody(equalToJson("{\"sortCode\":\"" + sortCode.replace("-", "") + "\", \"accountNumber\":\"" + accountNumber + "\"}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("true")
        )
    )

  def getBankFail(port: Int, sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/direct-debit/bank")).withRequestBody(equalToJson("{\"sortCode\":\"" + sortCode.replace("-", "") + "\", \"accountNumber\":\"" + accountNumber + "\"}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("false")
        )
    )

  def getBanksIsSuccessful: StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${TdAll.utr}/banks"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(prettyPrint(DirectDebitTd.directDebitBankJson))
        )
    )

  def getBanksReturns404BPNotFound(utr: SaUtr): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${utr.value}/banks"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{ reason: "BP not found", "reasonCode": "002" }""")
        )
    )

  def getBanksReturns404(utr: SaUtr): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${utr.value}/banks"))
        .willReturn(
          aResponse()
            .withStatus(404)
            .withBody("""{ reason: "some reason", "reasonCode": "some code" }""")
        )
    )

  def postPaymentPlan: StubMapping =
    stubFor(
      post(s"/direct-debit/${TdAll.utr}/instructions/payment-plan")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(prettyPrint(DirectDebitTd.directDebitInstructionPaymentPlanJson))
        )
    )
}

