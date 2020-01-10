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

package uk.gov.hmrc.selfservicetimetopay.testonly

import play.api.libs.json.{JsValue, Json}

object TestUserDebits {

  val sample1: JsValue = Json.parse(
    s"""
       {
         "debits" : [ {
           "taxYearEnd" : "2019-04-05",
           "charge" : {
             "originCode" : "IN1",
             "creationDate" : "2019-01-05"
           },
           "relevantDueDate" : "2019-02-25",
           "totalOutstanding" : 2500
         },{
           "taxYearEnd" : "2019-04-05",
           "charge" : {
             "originCode" : "IN2",
             "creationDate" : "2019-01-05"
           },
           "relevantDueDate" : "2019-02-25",
           "totalOutstanding" : 2500
         } ]
       }
    """)

}
