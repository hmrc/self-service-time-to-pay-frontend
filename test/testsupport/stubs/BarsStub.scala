/*
 * Copyright 2023 HM Revenue & Customs
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
      post(urlPathEqualTo(s"/validate/bank-details"))
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

  def validateBankDDNotSupported(sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/validate/bank-details"))
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
            .withBody(validResponseDDNotSupported)
        )
    )

  def validateBankFail(sortCode: String, accountNumber: String): StubMapping =
    stubFor(
      post(urlPathEqualTo(s"/validate/bank-details"))
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
      post(urlPathEqualTo(s"/validate/bank-details"))
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
        "accountNumberIsWellFormatted": "Yes",
        "nonStandardAccountDetailsRequiredForBacs": "No",
        "sortCodeIsPresentOnEISCD":"Yes",
        "sortCodeSupportsDirectDebit": "Yes",
        "sortCodeSupportsDirectCredit": "Yes",
        "iban": "GB59 HBUK 1234 5678",
        "sortCodeBankName": "Lloyds"
      }
      """.stripMargin

  val validResponseDDNotSupported: String =
    // language=JSON
    """
      {
        "accountNumberIsWellFormatted": "Yes",
        "nonStandardAccountDetailsRequiredForBacs": "No",
        "sortCodeIsPresentOnEISCD":"Yes",
        "sortCodeSupportsDirectDebit": "No",
        "sortCodeSupportsDirectCredit": "Yes",
        "iban": "GB59 HBUK 1234 5678",
        "sortCodeBankName": "Lloyds"
      }
      """.stripMargin

  val invalidResponse: String =
    // language=JSON
    """
      {
        "accountNumberIsWellFormatted": "No",
        "nonStandardAccountDetailsRequiredForBacs": "No",
        "sortCodeIsPresentOnEISCD":"Yes",
        "sortCodeSupportsDirectDebit": "Yes",
        "sortCodeSupportsDirectCredit": "Yes",
        "iban": "GB59 HBUK 1234 5678",
        "sortCodeBankName": "Lloyds"
      }
      """.stripMargin

}

