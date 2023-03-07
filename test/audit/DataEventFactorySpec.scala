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
import ssttpcalculator.CalculatorService
import testsupport.ItSpec
import testsupport.testdata.{DirectDebitTd, TdAll, TdRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDayOfMonth, BankDetails, PlanSelection, SelectedPlan}

import java.time.ZoneId.systemDefault
import java.time.{Clock, LocalDateTime}

class DataEventFactorySpec extends ItSpec {
  private val td = TdAll
  private val tdRequest = TdRequest
  private val directDebitTd = DirectDebitTd
  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = tdRequest.request

  private val dataEventFactory: DataEventFactory = fakeApplication().injector.instanceOf[DataEventFactory]
  private val calculatorService: CalculatorService = fakeApplication().injector.instanceOf[CalculatorService]

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
      val _500Amount = 500
      val _600Amount = 600

      "negative disposable income case" in {
        val journeyNegativeRemainingIncome = journey.copy(
          maybeIncome   = Some(Income(IncomeBudgetLine(MonthlyIncome, _500Amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _600Amount)))
        )

        val computedDataEvent = dataEventFactory.planNotAffordableEvent(journeyNegativeRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityCheck",
          eventId     = "event-id",
          tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail      = Json.parse(
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

        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "zero disposable income case" in {
        val journeyZeroRemainingIncome = journey.copy(
          maybeIncome   = Some(Income(IncomeBudgetLine(MonthlyIncome, _500Amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _500Amount)))
        )

        val computedDataEvent = dataEventFactory.planNotAffordableEvent(journeyZeroRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityCheck",
          eventId     = "event-id",
          tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail      = Json.parse(
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

        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "no plan no longer than 24 months" in {
        val journeyNoPlanWithin24Months = journey.copy(
          maybeIncome   = Some(Income(IncomeBudgetLine(MonthlyIncome, _600Amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _500Amount)))
        )

        val computedDataEvent = dataEventFactory.planNotAffordableEvent(journeyNoPlanWithin24Months)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityCheck",
          eventId     = "event-id",
          tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail      = Json.parse(
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

        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
    }

    "manualAffordabilityPlanSetUp" - {
      val _1000Amount = 1000
      val _500Amount = 500
      val _250Amount = 250
      val _300Amount = 300
      val _400Amount = 400
      val _28DayOfMonth = 28

      val journeySuccessfulSetUp = journey.copy(
        maybeBankDetails           = Some(BankDetails(
          sortCode          = directDebitTd.sortCode,
          accountNumber     = directDebitTd.accountNumber,
          accountName       = directDebitTd.accountName,
          maybeDDIRefNumber = Some(directDebitTd.dDIRefNumber))),
        maybeIncome                = Some(Income(IncomeBudgetLine(MonthlyIncome, _1000Amount))),
        maybeSpending              = Some(Spending(Expenses(HousingExp, _500Amount))),
        maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(_28DayOfMonth)),
        ddRef                      = Some(directDebitTd.dDIRefNumber)
      )

      "50% case (more than 12 months)" in {
        val journey50PerCent = journeySuccessfulSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_250Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journey50PerCent)(request).get

        val computedDataEvent = dataEventFactory.planSetUpSuccessEvent(journey50PerCent, schedule)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityPlanSetUp",
          eventId     = "event-id",
          tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail      = Json.parse(
            s"""
            {
              "bankDetails": {
                "accountNumber": "12345678",
                "name": "Mr John Campbell",
                "sortCode": "12-34-56"
              },
              "halfDisposableIncome": "250",
              "selectionType": "fiftyPercent",
              "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
              "schedule": {
                "totalPayable": 5038.16,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":250,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":11,
                    "paymentDate":"2020-10-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":12,
                    "paymentDate":"2020-11-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":13,
                    "paymentDate":"2020-12-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":14,
                    "paymentDate":"2021-01-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":15,
                    "paymentDate":"2021-02-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":16,
                    "paymentDate":"2021-03-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":17,
                    "paymentDate":"2021-04-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":18,
                    "paymentDate":"2021-05-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":19,
                    "paymentDate":"2021-06-28"
                  },
                  {
                    "amount":250,
                    "instalmentNumber":20,
                    "paymentDate":"2021-07-28"
                  },
                  {
                    "amount":38.16,
                    "instalmentNumber":21,
                    "paymentDate":"2021-08-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 21,
                "totalInterestCharged": 138.16,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "60% case (more than twelve months)" in {
        val journey60PerCent = journeySuccessfulSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_300Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journey60PerCent)(request).get

        val computedDataEvent = dataEventFactory.planSetUpSuccessEvent(journey60PerCent, schedule)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityPlanSetUp",
          eventId     = "event-id",
          tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail      = Json.parse(
            s"""
            {
              "bankDetails": {
                "accountNumber": "12345678",
                "name": "Mr John Campbell",
                "sortCode": "12-34-56"
              },
              "halfDisposableIncome": "250",
              "selectionType": "sixtyPercent",
              "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
              "schedule": {
                "totalPayable": 5016.53,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":300,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":11,
                    "paymentDate":"2020-10-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":12,
                    "paymentDate":"2020-11-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":13,
                    "paymentDate":"2020-12-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":14,
                    "paymentDate":"2021-01-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":15,
                    "paymentDate":"2021-02-28"
                  },
                  {
                    "amount":300,
                    "instalmentNumber":16,
                    "paymentDate":"2021-03-28"
                  },
                  {
                    "amount":216.53,
                    "instalmentNumber":17,
                    "paymentDate":"2021-04-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 17,
                "totalInterestCharged": 116.53,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "80% case (more than twelve months)" in {
        val journey80PerCent = journeySuccessfulSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_400Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journey80PerCent)(request).get

        val computedDataEvent = dataEventFactory.planSetUpSuccessEvent(journey80PerCent, schedule)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityPlanSetUp",
          eventId     = "event-id",
          tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail      = Json.parse(
            s"""
            {
              "bankDetails": {
                "accountNumber": "12345678",
                "name": "Mr John Campbell",
                "sortCode": "12-34-56"
              },
              "halfDisposableIncome": "250",
              "selectionType": "eightyPercent",
              "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
              "schedule": {
                "totalPayable": 4989.39,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":400,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":11,
                    "paymentDate":"2020-10-28"
                  },
                  {
                    "amount":400,
                    "instalmentNumber":12,
                    "paymentDate":"2020-11-28"
                  },
                  {
                    "amount":189.39,
                    "instalmentNumber":13,
                    "paymentDate":"2020-12-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 13,
                "totalInterestCharged": 89.39,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "custom amount (twelve months or less)" in {
        val customAmount = 500

        val journeyCustomAmount = journeySuccessfulSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(customAmount)))),
        )

        val schedule = calculatorService.selectedSchedule(journeyCustomAmount)(request).get

        val computedDataEvent = dataEventFactory.planSetUpSuccessEvent(journeyCustomAmount, schedule)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityPlanSetUp",
          eventId     = "event-id",
          tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail      = Json.parse(
            s"""
            {
              "bankDetails": {
                "accountNumber": "12345678",
                "name": "Mr John Campbell",
                "sortCode": "12-34-56"
              },
              "halfDisposableIncome": "250",
              "selectionType": "customAmount",
              "lessThanOrMoreThanTwelveMonths": "twelveMonthsOrLess",
              "schedule": {
                "totalPayable": 4973.08,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":500,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":500,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":473.08,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 10,
                "totalInterestCharged": 73.08,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }

    }
  }
}