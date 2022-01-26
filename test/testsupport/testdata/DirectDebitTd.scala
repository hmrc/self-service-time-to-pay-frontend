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

package testsupport.testdata

import play.api.libs.json.JsObject
import testsupport.JsonSyntax._
import testsupport.testdata.TdAll.aYearAgo

object DirectDebitTd {
  val sortCode = "12-34-56"
  val accountNumber = "12345678"
  val accountName = "Mr John Campbell"

  private def directDebitInstructionJson(creationDate: String) =
    s"""{
          "sortCode":"12-34-56",
          "accountNumber":"12345678",
          "referenceNumber":"123456789",
          "creationDate": "$creationDate",
          "paperAuddisFlag": true,
          "ddiRefNumber": "123ABC123",
          "ddiReferenceNo": "123ABC123",
          "accountName":"Mr John Campbell"
        }""".asJson

  def directDebitBankJson(createdDate: String = "2020-04-05"): JsObject =
    s"""
      {
        "processingDate": "2019-04-05",
        "directDebitInstruction":[${directDebitInstructionJson(createdDate)}]
      }
    """.asJson

  val bpNotFound: JsObject =
    //language=Json
    s"""
      {
        "processingDate": "",
        "directDebitInstruction":[]
      }
    """.asJson

  val directDebitInstructionPaymentPlanJson: JsObject =
    s"""{
          "processingDate": "Data to process",
          "acknowledgementId": "123456543",
          "directDebitInstruction": [${directDebitInstructionJson(aYearAgo)}],
          "paymentPlan":[
            {
              "ppReferenceNo": "1234567"
            }
          ]
        }""".asJson

  val bankDetailsJson: JsObject =
    s"""{"sortCode": "$sortCode",
       "accountNumber": "$accountNumber",
       "bankName": "RBS",
       "bankAddress": {
         "addressLine1" : "Big building",
         "addressLine2" : "Barington Road",
         "postcode" : "BN12 4XL"
        },
       "accountName": "$accountName",
       "ddiRefNumber": "123456789"
       }""".asJson
}
