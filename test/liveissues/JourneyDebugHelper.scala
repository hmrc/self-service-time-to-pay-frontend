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

package liveissues

import java.time.LocalDate

import play.api.libs.json.{JsLookupResult, JsValue, Json, OFormat, Reads}
import timetopaytaxpayer.cor.model.{Debit, Interest}

/**
 * It's a tool for extracting DES responses from journey json.
 *
 * It takes a journey json (paste it from kibana logs)
 * and extracts debits and returns which can be
 * used to set up a test journey in inspector page
 * where you can define responses in DES stubs.
 *
 * /pay-what-you-owe-in-instalments/test-only/inspector
 *
 *
 * Usage:
 * 1. get the journey json from logs
 * 2. Paste it into `journeyJson` val
 * 3. Run the app
 * 4. Paste the output into
 * https://www.development.tax.service.gov.uk/pay-what-you-owe-in-instalments/test-only/create-user-and-log-in
 * 5. Happy debugging!
 */
object JourneyDebugHelper extends App {

  //enter a journey json
  // (get it from logs)
  val journeyJson = {
    // language=JSON
    """{
  "_id" : "5f7b957e1a000032a0c41ab2",
  "status" : "InProgress",
  "createdOn" : "2020-10-05T21:51:58.693",
  "maybeTaxpayer" : {
    "customerName" : "xx xxxxx xxxxx",
    "addresses" : [ {
      "addressLine1" : "4 xxxxxxx xxxxxxx",
      "addressLine2" : "xxxxx xxxx",
      "addressLine3" : "xxxxxxxxxx",
      "postcode" : "x16 7xx"
    } ],
    "selfAssessment" : {
      "utr" : "7970***",
      "communicationPreferences" : {
        "welshLanguageIndicator" : false,
        "audioIndicator" : false,
        "largePrintIndicator" : false,
        "brailleIndicator" : false
      },
      "debits" : [ {
        "originCode" : "BCD",
        "amount" : 1448.47,
        "dueDate" : "2021-01-31",
        "taxYearEnd" : "2020-04-05"
      }, {
        "originCode" : "IN1",
        "amount" : 724.23,
        "dueDate" : "2021-01-31",
        "taxYearEnd" : "2021-04-05"
      }, {
        "originCode" : "IN2",
        "amount" : 724.24,
        "dueDate" : "2021-07-31",
        "taxYearEnd" : "2021-04-05"
      } ],
      "returns" : [ {
        "taxYearEnd" : "2016-04-05",
        "issuedDate" : "2016-04-21",
        "dueDate" : "2017-01-31",
        "receivedDate" : "2017-02-16"
      }, {
        "taxYearEnd" : "2017-04-05",
        "issuedDate" : "2017-04-06",
        "dueDate" : "2018-01-31",
        "receivedDate" : "2018-01-31"
      }, {
        "taxYearEnd" : "2018-04-05",
        "issuedDate" : "2018-04-06",
        "dueDate" : "2019-01-31",
        "receivedDate" : "2019-01-30"
      }, {
        "taxYearEnd" : "2019-04-05",
        "receivedDate" : "2020-02-16"
      }, {
        "taxYearEnd" : "2020-04-05",
        "receivedDate" : "2020-06-13"
      } ]
    }
  },
  "durationMonths" : 2,
  "maybeEligibilityStatus" : {
    "reasons" : [ ]
  }
}""".stripMargin
  }

  //implementation

  val json = Json.parse(journeyJson)
  val debits: List[Debit] = (json \ "maybeTaxpayer" \ "selfAssessment" \ "debits").get.as[List[timetopaytaxpayer.cor.model.Debit]]
  val returns: JsValue = (json \ "maybeTaxpayer" \ "selfAssessment" \ "returns").get

  case class DesDebit(
      taxYearEnd:       LocalDate,
      charge:           DesCharge,
      relevantDueDate:  LocalDate,
      totalOutstanding: BigDecimal,
      interest:         Option[Interest]
  )

  case class DesCharge(
      originCode:   String,
      creationDate: LocalDate
  )

  implicit val f2: OFormat[DesCharge] = Json.format[DesCharge]
  implicit val f1: OFormat[DesDebit] = Json.format[DesDebit]

  def toDesDebit(d: timetopaytaxpayer.cor.model.Debit): DesDebit = DesDebit(
    taxYearEnd       = d.taxYearEnd,
    charge           = DesCharge(
      originCode   = d.originCode,
      creationDate = LocalDate.parse("1900-01-01") //its ignored
    ),
    relevantDueDate  = d.dueDate,
    totalOutstanding = d.amount,
    interest         = d.interest
  )

  val desReturns = Json.obj("returns" -> returns)
  println(Json.prettyPrint(desReturns))

  println()

  val desDebits = Json.obj("debits" -> debits.map(toDesDebit))
  println(Json.prettyPrint(desDebits))

}
