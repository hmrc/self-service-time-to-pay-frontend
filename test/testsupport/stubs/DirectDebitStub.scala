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
import play.api.http.Status
import play.api.libs.json.Json.prettyPrint
import testsupport.testdata.DirectDebitTd.{bpNotFound, directDebitBankJson}
import testsupport.testdata.TdAll.aYearAgo
import testsupport.testdata.{DirectDebitTd, TdAll}
import timetopaytaxpayer.cor.model.SaUtr

object DirectDebitStub extends Matchers with Status {

  def validateBank(port: Int, sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/direct-debit/validate-bank-account"))
        .withRequestBody(equalToJson("{\"sortCode\":\"" + sortCode.replace("-", "") + "\", \"accountNumber\":\"" + accountNumber + "\"}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("true")
        )
    )

  def validateBankFail(port: Int, sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/direct-debit/validate-bank-account"))
        .withRequestBody(equalToJson("{\"sortCode\":\"" + sortCode.replace("-", "") + "\", \"accountNumber\":\"" + accountNumber + "\"}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("false")
        )
    )

  def getBanksIsSuccessful(creationDate: String = aYearAgo): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${TdAll.utr}/banks"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(prettyPrint(directDebitBankJson(creationDate)))
        )
    )

  def getBanksBPNotFound(utr: SaUtr): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${utr.value}/banks"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(prettyPrint(bpNotFound))
        )
    )

  def getBanksNotFound(utr: SaUtr): StubMapping =
    stubFor(
      get(urlPathEqualTo(s"/direct-debit/${utr.value}/banks"))
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND)
            .withBody("""{ reason: "some reason", "reasonCode": "some code" }""")
        )
    )

  def postPaymentPlan: StubMapping =
    stubFor(
      post(s"/direct-debit/${TdAll.utr}/instructions/payment-plan")
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(prettyPrint(DirectDebitTd.directDebitInstructionPaymentPlanJson))
        )
    )
}

