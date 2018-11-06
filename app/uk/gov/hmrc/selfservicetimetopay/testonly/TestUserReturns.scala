/*
 * Copyright 2018 HM Revenue & Customs
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

object TestUserReturns {

  object Descriptions {

    //tax year ends in yyyy-04-05
    val taxYearEnd = "Tax Year Indicator. Indicates the year ending 5/4/nnnn (MANDATORY)"

    val issueDate = "Date of issue of the return (OPTIONAL)"

    //tax year ends in yyyy-04-05, and due date is in 31st'Jan next year
    val dueDate = "Date of deferral is returned when present else the return due date is supplied (OPTIONAL)"

    val receivedDate = "Date SA Return was received (OPTIONAL)"
  }

  /**
    * Assuming that today is 2017-11-15
    * User has submitted all tax relevant tax returns
    */
  val sample1: JsValue = Json.parse(
    s"""
      {
        "returns": [
          {
            "taxYearEnd":   "2017-04-05",
            "dueDate":      "2019-01-31"
          },
          {
            "taxYearEnd":   "2016-04-05",
            "dueDate":      "2017-01-31",
            "receivedDate": "2016-09-09"
          },
          {
            "taxYearEnd":   "2015-04-05",
            "dueDate":      "2016-01-31",
            "receivedDate": "2017-09-09"
          },
          {
            "taxYearEnd":   "2015-04-05",
            "dueDate":      "2016-01-31",
            "issueDate":    "2016-02-15",
            "receivedDate": "2016-03-09"
          }
        ]
      }
    """)

  /**
    * Assuming that today is 2017-11-15
    * User has submitted all but last tax returns
    * There is not filed last tax return.
    */
  val sample2: JsValue = Json.parse(
    s"""
      {
        "returns": [
          {
            "taxYearEnd":   "2016-04-05",
            "dueDate":      "2017-01-31",
            "receivedDate": "2016-09-09"
          },
          {
            "taxYearEnd":   "2015-04-05",
            "dueDate":      "2016-01-31",
            "receivedDate": "2017-09-09"
          },
          {
            "taxYearEnd":   "2015-04-05",
            "dueDate":      "2016-01-31",
            "receivedDate": "2015-09-09"
          }
        ]
      }
    """)

}
