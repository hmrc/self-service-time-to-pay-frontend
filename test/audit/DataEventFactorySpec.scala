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

package audit

import java.time.ZoneOffset.UTC
import journey.Journey
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import testsupport.ItSpec
import testsupport.testdata.{TdAll, TdRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.ZoneId.systemDefault
import java.time.{Clock, LocalDateTime}

class DataEventFactorySpec extends ItSpec {
  private val td = TdAll
  private val tdRequest = TdRequest
  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = tdRequest.request

  private val dataEventFactory: DataEventFactory = fakeApplication().injector.instanceOf[DataEventFactory]

  private def fixedClock: Clock = {
    val currentDateTime = LocalDateTime.parse("2020-05-02T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  val journey: Journey = Journey.newJourney(fixedClock)

  private def splunkEventTags(transName: String) = Map(
    "clientIP" -> tdRequest.trueClientIp,
    "path" -> tdRequest.requestPath,
    "X-Session-ID" -> tdRequest.rawSessionId,
    "Akamai-Reputation" -> tdRequest.akamaiReputationValue,
    "X-Request-ID" -> tdRequest.requestId,
    "deviceID" -> tdRequest.deviceId,
    "clientPort" -> tdRequest.trueClientPort,
    "transactionName" -> transName
  )

  "Splunk audit events" - {

    s"manualAffordabilityCheck" in {
      val computedDataEvent = dataEventFactory.manualAffordabilityCheck(journey)

      val expectedDataEvent = ExtendedDataEvent(
        auditSource = "pay-what-you-owe",
        auditType   = "ManualAffordabilityCheck",
        eventId     = "event-id",
        tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
        detail      = Json.parse(
          s"""
            {
              "totalDebt": "5000",
              "spending": "9001.56",
              "income": "5000",
              "halfDisposalIncome": "-4001.56",
              "status": "Negative Disposable Income",
              "utr": "012324729"
            }
            """)
      )

      computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
    }

    s"manualAffordabilityPlanSetUp" in {
      val computedDataEvent = dataEventFactory.manualAffordabilityPlanSetUp(journey)

      val expectedDataEvent = ExtendedDataEvent(
        auditSource = "pay-what-you-owe",
        auditType   = "ManualAffordabilityPlanSetUp",
        eventId     = "event-id",
        tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
        detail      = Json.parse(
          s"""
          {
            "bankDetails": {
              "accountNumber": "86563611",
              "name": "Illumination",
              "sortCode": "207102",
            },
            "halfDisposalIncome: "41000.00",
            "selectionType: "fiftyPercent",
            "schedule": {
              totalPayable": "9001.56",
                   "installmentDate": "28",
                   "installments": [
                      {
                        "amount": "1500",
                        "installmentNumber": "1",
                        "paymentDate": "2023-08-28",
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": 2",
                        "paymentDate": "2023-07-28",
                      },
                      {
                        "amount": "1500",
                         "installmentNumber": "3",
                         "paymentDate": "2023-06-28",
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": "4",
                         "paymentDate": "2023-05-28"
                      },
                      {
                        "amount": "1500",
                        "installmentNumber": "5",
                         "paymentDate": "2023-04-28"
                      },
                      {
                        "amount": "1500",
                         "installmentNumber": "6",
                         "paymentDate": "2023-03-28"
                      }
                   ],
                   "initialPaymentAmount": "0",
                   "totalNoPayments": "6",
                   "totalInterestCharged": "1.56",
                   "totalPayable": "9001.56",
                   "totalPaymentWithoutInterest": "9000"
                },
                "status": "Success",
                "paymentReference": "paymentReference",
                "utr": "012324729"
             }
          }
          """)
      )
      computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
    }
  }
}
