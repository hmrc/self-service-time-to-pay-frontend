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

import java.time.LocalDate

import testsupport.JsonSyntax._
import testsupport.testdata.TdAll._
import uk.gov.hmrc.selfservicetimetopay.models._

object DirectDebitTd {
  val sortCode = "12-09-98"
  val accountNumber = "124365"
  val accountName = "Mr John Campbell"

  val directDebitInstruction = new DirectDebitInstruction(
    sortCode        = Some(sortCode),
    accountNumber   = Some(accountNumber),
    referenceNumber = Some("123456789"),
    creationDate    = Some("2019-04-05"),
    paperAuddisFlag = Some(true),
    ddiRefNumber    = None,
    ddiReferenceNo  = None,
    accountName     = Some(accountName))

  val directDebitInstructionJson =
    //language=Json
    """{
        "sortCode":"12-09-98",
   "accountNumber":"124365",
   "referenceNumber":"123456789",
   "creationDate": "2019-04-05",
   "paperAuddisFlag": true,
   "accountName":"Mr John Campbell"
    }""".asJson

  val directDebitBank = new DirectDebitBank(
    processingDate         = "2019-04-05",
    directDebitInstruction = List(directDebitInstruction))

  val directDebitBankJson =
    s"""
      {
      "processingDate": "2019-04-05",
      "directDebitInstruction":[${directDebitInstructionJson}]
      }
    """.asJson

  val directDebitPaymentPlan = new DirectDebitPaymentPlan(ppReferenceNo = "1234567")

  val directDebitPaymentPlanJson =
    //language=Json
    """{
    "ppReferenceNo": "1234567"
    }""".asJson

  val directDebitInstructionPaymentPlan = new DirectDebitInstructionPaymentPlan(
    processingDate         = "Data to process",
    acknowledgementId      = "123456543",
    directDebitInstruction = List(directDebitInstruction),
    paymentPlan            = List(directDebitPaymentPlan)
  )

  val directDebitInstructionPaymentPlanJson =
    s"""{
      "processingDate": "Data to process",
      "acknowledgementId": "123456543",
      "directDebitInstruction": [${directDebitInstructionJson}],
      "paymentPlan":[${directDebitPaymentPlanJson}]
      }""".asJson

  val knownFact = new KnownFact(service = "live services", value = "Hi")

  val knownFactJson =
    //language=Json
    """{
     "service": "live services",
      "value": "Hi"
     }""".asJson

  val paymentPlan = new PaymentPlan(ppType                    = "DirectDebit",
                                    paymentReference          = "1234ed",
                                    hodService                = "agents",
                                    paymentCurrency           = "GB",
                                    initialPaymentAmount      = Some("20000"),
                                    initialPaymentStartDate   = Some("2019-06-12"),
                                    scheduledPaymentAmount    = "200",
                                    scheduledPaymentStartDate = "2019-07-12",
                                    scheduledPaymentEndDate   = "2019-08-12",
                                    scheduledPaymentFrequency = "MONTHLY",
                                    balancingPaymentAmount    = "20",
                                    balancingPaymentDate      = "2019-07-18",
                                    totalLiability            = "600")

  val paymentPlanJson =
    //language=Json
    """
    {
     "ppType": "DirectDebit",
     "paymentReference": "1234ed",
     "hodService": "agents",
     "paymentCurrency": "GB",
     "initialPaymentAmount": "20000",
     "initialPaymentStartDate": "2019-06-12",
     "scheduledPaymentAmount": "200",
     "scheduledPaymentStartDate": "2019-07-12",
     "scheduledPaymentEndDate": "2019-08-12",
     "scheduledPaymentFrequency": "MONTHLY",
     "balancingPaymentAmount": "20",
     "balancingPaymentDate": "2019-07-18",
     "totalLiability": "600"
    }
  """.asJson

  val paymentPlanRequest = new PaymentPlanRequest(
    requestingService      = "agents",
    submissionDateTime     = "2019-06-12",
    knownFact              = List(knownFact),
    directDebitInstruction = directDebitInstruction,
    paymentPlan            = paymentPlan,
    printFlag              = true)

  val paymentPlanRequestJson =
    s"""
      {
      "requestingService": "agents",
      "submissionDateTime": "2019-06-12",
      "knownFact": [${knownFactJson}],
      "directDebitInstruction": ${directDebitInstructionJson},
       "paymentPlan": ${paymentPlanJson},
       "printFlag": true
      }
    """.asJson

  val bankDetails = new BankDetails(
    sortCode      = sortCode,
    accountNumber = accountNumber,
    bankName      = "RBS",
    bankAddress   = address,
    accountName   = accountName,
    ddiRefNumber  = "123456789"
  )

  val bankDetailsJson =
    s"""{"sortCode": "${sortCode}",
       "accountNumber": "${accountNumber}",
       "bankName": "RBS",
       "bankAddress": ${addressJson},
       "accountName": "${accountName}",
       "ddiRefNumber": "123456789"
       }""".asJson

}
