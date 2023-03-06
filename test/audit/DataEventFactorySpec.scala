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
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import testsupport.ItSpec
import testsupport.testdata.{DirectDebitTd, TdAll, TdRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.models.BankDetails

import java.time.ZoneId.systemDefault
import java.time.{Clock, LocalDateTime}

class DataEventFactorySpec extends ItSpec {
  private val td = TdAll
  private val tdRequest = TdRequest
  private val directDebitTd = DirectDebitTd
  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = tdRequest.request

  private val dataEventFactory: DataEventFactory = fakeApplication().injector.instanceOf[DataEventFactory]

  private def fixedClock: Clock = {
    val currentDateTime = LocalDateTime.parse("2020-05-02T00:00:00.880").toInstant(UTC)
    Clock.fixed(currentDateTime, systemDefault)
  }

  val journey: Journey = Journey.newJourney(fixedClock)
    .copy(
      maybeTaxpayer = Some(td.taxpayer),
    )

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

    "manualAffordabilityCheck" - {
      val _500amount = 500
      val _600amount = 600

      "negative disposable income case" in {
        val journeyNegativeRemainingIncome = journey.copy(
          maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, _500amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _600amount)))
        )

        val computedDataEvent = dataEventFactory.manualAffordabilityCheck(journeyNegativeRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType = "ManualAffordabilityCheck",
          eventId = "event-id",
          tags = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail = Json.parse(
            s"""
              {
                "totalDebt": "4900",
                "spending": "600",
                "income": "500",
                "halfDisposableIncome": "-50",
                "status": "Negative Disposable Income",
                "utr": "6573196998"
              }
              """)
        )

        computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
      }
      "zero disposable income case" in {
        val journeyZeroRemainingIncome = journey.copy(
          maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, _500amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _500amount)))
        )

        val computedDataEvent = dataEventFactory.manualAffordabilityCheck(journeyZeroRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType = "ManualAffordabilityCheck",
          eventId = "event-id",
          tags = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail = Json.parse(
            s"""
              {
                "totalDebt": "4900",
                "spending": "500",
                "income": "500",
                "halfDisposableIncome": "0",
                "status": "Zero Disposable Income",
                "utr": "6573196998"
              }
              """)
        )

        computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
      }
      "no plan no longer than 24 months" in {
        val journeyNoPlanWithin24Months = journey.copy(
          maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, _600amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _500amount)))
        )

        val computedDataEvent = dataEventFactory.manualAffordabilityCheck(journeyNoPlanWithin24Months)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType = "ManualAffordabilityCheck",
          eventId = "event-id",
          tags = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail = Json.parse(
            s"""
              {
                "totalDebt": "4900",
                "spending": "500",
                "income": "600",
                "halfDisposableIncome": "50",
                "status": "Total Tax Bill Income Greater than 24 Months",
                "utr": "6573196998"
              }
              """)
        )

        computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
      }
    }

    "manualAffordabilityPlanSetUp" - {
      "50% case" in {
        val journey50PerCent = journey.copy(
          maybeBankDetails = Some(BankDetails(
            sortCode = directDebitTd.sortCode,
            accountNumber = directDebitTd.accountNumber,
            accountName = directDebitTd.accountName,
            maybeDDIRefNumber = Some(directDebitTd.dDIRefNumber)))
        )

        val computedDataEvent = dataEventFactory.manualAffordabilityPlanSetUp(journey50PerCent)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType = "ManualAffordabilityPlanSetUp",
          eventId = "event-id",
          tags = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail = Json.parse(
            s"""
            {
              "bankDetails": {
                "accountNumber": "12345678",
                "name": "Mr John Campbell",
                "sortCode": "12-34-56",
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
        computedDataEvent.copy(eventId = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId = "event-id", generatedAt = td.instant)
      }
    }
  }
}
