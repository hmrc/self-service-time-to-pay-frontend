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
import TdAll._
import CalculatorTd._
import testsupport.JsonSyntax._
import uk.gov.hmrc.selfservicetimetopay.models._

object ArrangementTd {

  val arrangementDayOfMonth = new ArrangementDayOfMonth(dayOfMonth = 4)

  val arrangementDayOfMonthJson =
    //language=Json
    """{
     "dayOfMonth": 4
     }""".asJson

  val arrangementDirectDebit = new ArrangementDirectDebit(
    accountName   = "Mr John Campbell",
    sortCode      = "12-34-56",
    accountNumber = "12345678"
  )

  val arrangementDirectDebitJson =
    //language=Json
    """{
    "accountName": "Mr John Campbell",
     "sortCode": "12-34-56",
     "accountNumber": "12345678"
    }""".asJson

  val tTPArrangement = new TTPArrangement(paymentPlanReference = "12345678",
                                          directDebitReference = "87654321",
                                          taxpayerDetails      = taxpayerDetails,
                                          schedule             = calculatorPaymentSchedule)

  val tTPArrangementJson =
    s"""{
    "paymentPlanReference": "12345678",
     "directDebitReference": "87654321",
     "taxpayer": ${taxpayerDetailsJson},
     "schedule": ${calculatorPaymentScheduleJson}
    }""".asJson

}
