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

package testsupport.testdata

import play.api.libs.json.JsObject
import testsupport.JsonSyntax._
import testsupport.testdata.TdAll._

object ArrangementTd {
  private val calculatorPaymentScheduleInstalmentJson =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 10000,
      "interest": 2
    }""".asJson

  private val calculatorPaymentScheduleInstalmentJson2 =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 120,
      "interest": 4
    }""".asJson

  private val calculatorPaymentScheduleInstalmentJson3 =
    //language=Json
    """{
      "paymentDate": "2019-08-25",
      "amount": 250,
      "interest": 3
    }""".asJson

  private val calculatorPaymentScheduleJson: JsObject =
    s"""{
          "startDate" : "2019-04-23",
          "endDate" : "2019-08-21",
          "initialPayment" : 0,
           "amountToPay" : 4000,
           "instalmentBalance" : 200,
           "totalInterestCharged" : 200,
           "totalPayable" : 200,
           "instalments" : [
              $calculatorPaymentScheduleInstalmentJson,
              $calculatorPaymentScheduleInstalmentJson2,
              $calculatorPaymentScheduleInstalmentJson3
            ]
        }""".asJson

  val tTPArrangementJson: JsObject =
    s"""{
          "paymentPlanReference": "12345678",
          "directDebitReference": "87654321",
          "taxpayer": {
            "customerName": "Mr John Campbell",
            "addresses": [
              {
               "addressLine1" : "Big building",
               "addressLine2" : "Barington Road",
               "postcode" : "BN12 4XL"
              }
            ],
            "selfAssessment": {
              "utr": "$utr",
              "communicationPreferences": {
                 "welshLanguageIndicator": false,
                 "audioIndicator": false,
                 "largePrintIndicator": false,
                 "brailleIndicator": false
              },
              "debits": [
                $debit1Json,
                $debit2Json
              ],
              "returns": [
                {
                  "taxYearEnd": "2020-04-05",
                  "issuedDate": "2019-11-10",
                  "dueDate": "2019-08-15",
                  "receivedDate": "2018-03-09"
                },
                {
                  "taxYearEnd": "2018-04-05",
                  "issuedDate": "2017-02-15",
                  "dueDate": "2018-01-31",
                  "receivedDate": "2018-03-09"
                }
              ]
            }
          },
          "schedule": $calculatorPaymentScheduleJson
        }""".asJson
}
