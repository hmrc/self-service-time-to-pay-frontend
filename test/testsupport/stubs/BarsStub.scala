/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import play.api.http.Status

object BarsStub extends Matchers with Status {

  def validateBank(sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/v2/validateBankDetails"))
        .withRequestBody(equalToJson(
          s"""
            {
            "account" : {
              "sortCode" : "${sortCode.replace("-", "")}",
              "accountNumber" : "$accountNumber"
              }
            }
            """))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(validResponse)
        )
    )

  def validateBankFail(sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/v2/validateBankDetails"))
        .withRequestBody(equalToJson(
          s"""
            {
            "account" : {
              "sortCode" : "${sortCode.replace("-", "")}",
              "accountNumber" : "$accountNumber"
              }
            }
            """))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(invalidResponse)
        )
    )

  def validateBankFailSortCodeOnDenyList(sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/v2/validateBankDetails"))
        .withRequestBody(equalToJson(
          s"""
            {
            "account" : {
              "sortCode" : "${sortCode.replace("-", "")}",
              "accountNumber" : "$accountNumber"
              }
            }
            """))
        .willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody(errorResponse)
        )
    )

  val errorResponse: String =
    // language=JSON
    """
      {
        "code": "SORT_CODE_ON_DENY_LIST",
        "desc": "083210: sort code is on deny list"
      }"""

  val validResponse: String =
    // language=JSON
    """
      {
        "accountNumberWithSortCodeIsValid": "yes",
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"yes",
        "supportsBACS":"yes",
        "ddiVoucherFlag":"no",
        "directDebitsDisallowed": "no",
        "directDebitInstructionsDisallowed": "no",
        "iban": "GB59 HBUK 1234 5678",
        "sortCodeBankName": "Lloyds"
      }
      """.stripMargin

  val invalidResponse: String =
    // language=JSON
    """
      {
        "accountNumberWithSortCodeIsValid": "yes",
        "nonStandardAccountDetailsRequiredForBacs": "no",
        "sortCodeIsPresentOnEISCD":"yes",
        "supportsBACS":"yes",
        "ddiVoucherFlag":"no",
        "directDebitsDisallowed": "yes",
        "directDebitInstructionsDisallowed": "yes",
        "iban": "GB59 HBUK 1234 5678",
        "sortCodeBankName": "Lloyds"
      }
      """.stripMargin

}

