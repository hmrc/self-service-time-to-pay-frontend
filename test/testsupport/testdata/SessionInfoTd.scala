/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime}

import play.libs.Json
import uk.gov.hmrc.selfservicetimetopay.models._
import testsupport.testdata.CalculatorTd._
import testsupport.testdata.DirectDebitTd._
import testsupport.testdata.TdAll._
import testsupport.JsonSyntax._
import token.{TTPSessionId, Token, TokenData}

object SessionInfoTd {

  val notLoggedInJourneyInfo = NotLoggedInJourneyInfo(
    amountDue                 = Some(200),
    calculatorPaymentSchedule = Some(calculatorPaymentSchedule)
  )

  val notLoggedInJourneyInfoJson =
    s"""
      {
      "amountDue": 200,
      "calculatorPaymentSchedule": ${calculatorPaymentScheduleJson}
      }
    """.asJson

  val eligibilityStatus = EligibilityStatus(
    eligible = true,
    reasons  = List(NoDebt)
  )

  val eligibilityStatusJson =
    s"""
      {
      "eligible":true,
      "reasons":["NoDebt"]
      }
    """.asJson

  val tTPSubmission = TTPSubmission(
    schedule               = Some(calculatorPaymentSchedule),
    bankDetails            = Some(bankDetails),
    existingDDBanks        = Some(directDebitBank),
    taxpayer               = Some(taxpayer),
    calculatorData         = calculatorInput,
    durationMonths         = Some(2),
    eligibilityStatus      = Some(eligibilityStatus),
    debitDate              = Some("2019-04-05"),
    notLoggedInJourneyInfo = Some(notLoggedInJourneyInfo),
    ddRef                  = Some("1234567A"))

  val tTPSubmissionJson =
    s"""{
        "schedule" : ${calculatorPaymentScheduleJson},
        "bankDetails": ${bankDetailsJson},
        "existingDDBanks": ${directDebitBankJson},
        "taxpayer": ${taxpayerJson},
        "calculatorData" : ${calculatorInputJson},
        "durationMonths" : 2,
        "eligibilityStatus": ${eligibilityStatusJson},
        "debitDate" :  "2019-04-05",
        "notLoggedInJourneyInfo" : ${notLoggedInJourneyInfoJson},
        "ddRef" : "1234567A"
      }""".asJson

  val eligibilityRequest = new EligibilityRequest(
    dateOfEligibilityCheck = "2019-04-05",
    taxpayer               = taxpayer)

  val eligibilityRequestJson =
    s"""
      {
       "dateOfEligibilityCheck":"2019-04-05",
       "taxpayer":${taxpayerJson}
      }
    """.asJson

  val tTPSessionId = new TTPSessionId(v = "12345A")

  val tTPSessionIdJson =
    //language=Json
    """
      {
      "v":"12345A"
      }
    """.asJson

  val tokenTd = new Token(v = "token")

  val tokenTdJson =
    //language=Json
    """
      {
       "v": "token"
      }
    """.asJson

  val ldt = LocalDateTime.of(2019, 8, 2, 3, 2, 1, 2)

  val tokenData = new TokenData(token                = tokenTd, expirationDate = ldt, associatedTTPSession = tTPSessionId)

  val tokenDataJson =
    s"""
      {
      "token":${tokenTdJson},
      "expirationDate": "${ldt}",
      "associatedTTPSession": ${tTPSessionIdJson}
      }
    """.asJson

}
