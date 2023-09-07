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

package audit.legacycalculator

import audit.DataEventFactory
import config.AppConfig
import journey.Journey
import journey.Statuses.ApplicationComplete
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import ssttparrangement.ArrangementSubmissionStatus
import ssttparrangement.ArrangementSubmissionStatus.{PermanentFailure, QueuedForRetry}
import ssttpcalculator.CalculatorType.Legacy
import ssttpcalculator.legacy.CalculatorService
import testsupport.ItSpec
import testsupport.testdata.{DirectDebitTd, TdAll, TdRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, PaymentDayOfMonth, PlanSelection, SelectedPlan}

import java.time.ZoneId.systemDefault
import java.time.ZoneOffset.UTC
import java.time.{Clock, LocalDateTime}

class DataEventFactoryLegacyCalculatorSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> Legacy.value
  )

  private val td = TdAll
  private val tdRequest = TdRequest
  private val directDebitTd = DirectDebitTd
  private implicit val request: FakeRequest[AnyContentAsEmpty.type] = tdRequest.request
  private implicit val appConfig: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
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

  private val auditTypePlanNotAvailable = "ManualAffordabilityCheckFailed"

  "Splunk audit events" - {

    "manualAffordabilityCheck" - {
      val _500Amount = 500
      val _600Amount = 600
      val _50Amount = 50

      "negative disposable income case" in {
        val journeyNegativeRemainingIncome = journey.copy(
          maybeIncome   = Some(Income(IncomeBudgetLine(MonthlyIncome, _500Amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _600Amount)))
        )

        val computedDataEvent = DataEventFactory.planNotAvailableEvent(journeyNegativeRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = auditTypePlanNotAvailable,
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

        val computedDataEvent = DataEventFactory.planNotAvailableEvent(journeyZeroRemainingIncome)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = auditTypePlanNotAvailable,
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

        val computedDataEvent = DataEventFactory.planNotAvailableEvent(journeyNoPlanWithin24Months)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = auditTypePlanNotAvailable,
          eventId     = "event-id",
          tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail      = Json.parse(
            s"""
              {
                "totalDebt": "4900",
                "spending": "500",
                "income": "600",
                "halfDisposableIncome": "50",
                "status": "Plan duration would exceed maximum",
                "utr": "6573196998"
              }
              """)
        )

        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "fails NDDS validation check" in {
        val testJourney = journey.copy(
          maybeIncome   = Some(Income(IncomeBudgetLine(MonthlyIncome, _600Amount))),
          maybeSpending = Some(Spending(Expenses(HousingExp, _50Amount)))
        )

        val computedDataEvent = DataEventFactory.planNotAvailableEvent(testJourney, maybeFailsNDDSValidation = Some(true))

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = auditTypePlanNotAvailable,
          eventId     = "event-id",
          tags        = splunkEventTags("cannot-agree-self-assessment-time-to-pay-plan-online"),
          detail      = Json.parse(
            s"""
              {
                "totalDebt": "4900",
                "spending": "50",
                "income": "600",
                "halfDisposableIncome": "275",
                "status": "Interest greater than or equal to regular payment",
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
      val _245Amount = 245
      val _257_89Amount = 257.89
      val _272_22Amount = 272.22
      val _490Amount = 490
      val _28DayOfMonth = 28

      val journeyPlanSetUp = journey.copy(
        status                           = ApplicationComplete,
        maybeBankDetails                 = Some(BankDetails(
          sortCode          = directDebitTd.sortCode,
          accountNumber     = directDebitTd.accountNumber,
          accountName       = directDebitTd.accountName,
          maybeDDIRefNumber = Some(directDebitTd.dDIRefNumber))),
        maybeIncome                      = Some(Income(IncomeBudgetLine(MonthlyIncome, _1000Amount))),
        maybeSpending                    = Some(Spending(Expenses(HousingExp, _500Amount))),
        maybePaymentDayOfMonth           = Some(PaymentDayOfMonth(_28DayOfMonth)),
        ddRef                            = Some(directDebitTd.dDIRefNumber),
        maybeArrangementSubmissionStatus = Some(ArrangementSubmissionStatus.Success)
      )

      "basic plan (more than 12 months)" in {
        val journeyBasicPlan = journeyPlanSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_245Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journeyBasicPlan)(request)

        val computedDataEvent = DataEventFactory.planSetUpEvent(journeyBasicPlan, schedule, calculatorService)

        val expectedDataEvent = ExtendedDataEvent(
          auditSource = "pay-what-you-owe",
          auditType   = "ManualAffordabilityPlanSetUp",
          eventId     = "event-id",
          tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
          detail      = detailBasicPlan(ArrangementSubmissionStatus.Success)
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "higher plan (more than twelve months)" in {
        val journeyHigherPlan = journeyPlanSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_257_89Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journeyHigherPlan)(request)

        val computedDataEvent = DataEventFactory.planSetUpEvent(journeyHigherPlan, schedule, calculatorService)

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
              "selectionType": "higher",
              "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
              "schedule": {
                "totalPayable": 5034.1,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":257.89,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":11,
                    "paymentDate":"2020-10-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":12,
                    "paymentDate":"2020-11-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":13,
                    "paymentDate":"2020-12-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":14,
                    "paymentDate":"2021-01-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":15,
                    "paymentDate":"2021-02-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":16,
                    "paymentDate":"2021-03-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":17,
                    "paymentDate":"2021-04-28"
                  },
                  {
                    "amount":257.89,
                    "instalmentNumber":18,
                    "paymentDate":"2021-05-28"
                  },
                  {
                    "amount":391.99,
                    "instalmentNumber":19,
                    "paymentDate":"2021-06-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 19,
                "totalInterestCharged": 134.1,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "ApplicationComplete",
              "arrangementSubmissionStatus": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "additional plan (more than twelve months)" in {
        val journeyAdditionalPlan = journeyPlanSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(_272_22Amount)))),
        )

        val schedule = calculatorService.selectedSchedule(journeyAdditionalPlan)(request)

        val computedDataEvent = DataEventFactory.planSetUpEvent(journeyAdditionalPlan, schedule, calculatorService)

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
              "selectionType": "additional",
              "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
              "schedule": {
                "totalPayable": 5027.47,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":272.22,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":11,
                    "paymentDate":"2020-10-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":12,
                    "paymentDate":"2020-11-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":13,
                    "paymentDate":"2020-12-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":14,
                    "paymentDate":"2021-01-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":15,
                    "paymentDate":"2021-02-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":16,
                    "paymentDate":"2021-03-28"
                  },
                  {
                    "amount":272.22,
                    "instalmentNumber":17,
                    "paymentDate":"2021-04-28"
                  },
                  {
                    "amount":399.69,
                    "instalmentNumber":18,
                    "paymentDate":"2021-05-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 18,
                "totalInterestCharged": 127.47,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "ApplicationComplete",
              "arrangementSubmissionStatus": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "customAmount plan (twelve months or less)" in {
        val customAmount = _490Amount

        val journeyCustomAmount = journeyPlanSetUp.copy(
          maybePlanSelection = Some(PlanSelection(Left(SelectedPlan(customAmount)))),
        )

        val schedule = calculatorService.selectedSchedule(journeyCustomAmount)(request)

        val computedDataEvent = DataEventFactory.planSetUpEvent(journeyCustomAmount, schedule, calculatorService)

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
                "totalPayable": 4974.3,
                "instalmentDate": 28,
                "instalments": [
                  {
                    "amount":490,
                    "instalmentNumber":1,
                    "paymentDate":"2019-12-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":2,
                    "paymentDate":"2020-01-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":3,
                    "paymentDate":"2020-02-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":4,
                    "paymentDate":"2020-03-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":5,
                    "paymentDate":"2020-04-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":6,
                    "paymentDate":"2020-05-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":7,
                    "paymentDate":"2020-06-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":8,
                    "paymentDate":"2020-07-28"
                  },
                  {
                    "amount":490,
                    "instalmentNumber":9,
                    "paymentDate":"2020-08-28"
                  },
                  {
                    "amount":564.3,
                    "instalmentNumber":10,
                    "paymentDate":"2020-09-28"
                  }
                ],
                "initialPaymentAmount": 0,
                "totalNoPayments": 10,
                "totalInterestCharged": 74.3,
                "totalPaymentWithoutInterest": 4900
              },
              "status": "ApplicationComplete",
              "arrangementSubmissionStatus": "Success",
              "paymentReference": "123ABC123",
              "utr": "6573196998"
          }"""
          )
        )
        computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
          expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
      }
      "Arrangement submission status" - {
        "Not successful - queued for retry" in {
          val journeyArrangementNotSuccessfulQueued = journeyPlanSetUp.copy(
            maybePlanSelection               = Some(PlanSelection(Left(SelectedPlan(_245Amount)))),
            maybeArrangementSubmissionStatus = Some(QueuedForRetry)
          )

          val schedule = calculatorService.selectedSchedule(journeyArrangementNotSuccessfulQueued)(request)

          val computedDataEvent = DataEventFactory.planSetUpEvent(journeyArrangementNotSuccessfulQueued, schedule, calculatorService)

          val expectedDataEvent = ExtendedDataEvent(
            auditSource = "pay-what-you-owe",
            auditType   = "ManualAffordabilityPlanSetUp",
            eventId     = "event-id",
            tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
            detail      = detailBasicPlan(ArrangementSubmissionStatus.QueuedForRetry)
          )
          computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
            expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)
        }
        "Permanent failure" in {
          val journeyArrangementNotSuccessfulQueued = journeyPlanSetUp.copy(
            maybePlanSelection               = Some(PlanSelection(Left(SelectedPlan(_245Amount)))),
            maybeArrangementSubmissionStatus = Some(PermanentFailure)
          )

          val schedule = calculatorService.selectedSchedule(journeyArrangementNotSuccessfulQueued)(request)

          val computedDataEvent = DataEventFactory.planSetUpEvent(journeyArrangementNotSuccessfulQueued, schedule, calculatorService)

          val expectedDataEvent = ExtendedDataEvent(
            auditSource = "pay-what-you-owe",
            auditType   = "ManualAffordabilityPlanSetUp",
            eventId     = "event-id",
            tags        = splunkEventTags("setup-new-self-assessment-time-to-pay-plan"),
            detail      = detailBasicPlan(ArrangementSubmissionStatus.PermanentFailure)
          )
          computedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant) shouldBe
            expectedDataEvent.copy(eventId     = "event-id", generatedAt = td.instant)

        }
      }
    }
  }

  def detailBasicPlan(arrangementSubmissionStatus: ArrangementSubmissionStatus): JsValue = Json.parse(
    s"""
              {
                "bankDetails": {
                  "accountNumber": "12345678",
                  "name": "Mr John Campbell",
                  "sortCode": "12-34-56"
                },
                "halfDisposableIncome": "250",
                "selectionType": "basic",
                "lessThanOrMoreThanTwelveMonths": "moreThanTwelveMonths",
                "schedule": {
                  "totalPayable": 5040.73,
                  "instalmentDate": 28,
                  "instalments": [
                    {
                      "amount":245,
                      "instalmentNumber":1,
                      "paymentDate":"2019-12-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":2,
                      "paymentDate":"2020-01-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":3,
                      "paymentDate":"2020-02-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":4,
                      "paymentDate":"2020-03-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":5,
                      "paymentDate":"2020-04-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":6,
                      "paymentDate":"2020-05-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":7,
                      "paymentDate":"2020-06-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":8,
                      "paymentDate":"2020-07-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":9,
                      "paymentDate":"2020-08-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":10,
                      "paymentDate":"2020-09-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":11,
                      "paymentDate":"2020-10-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":12,
                      "paymentDate":"2020-11-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":13,
                      "paymentDate":"2020-12-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":14,
                      "paymentDate":"2021-01-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":15,
                      "paymentDate":"2021-02-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":16,
                      "paymentDate":"2021-03-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":17,
                      "paymentDate":"2021-04-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":18,
                      "paymentDate":"2021-05-28"
                    },
                    {
                      "amount":245,
                      "instalmentNumber":19,
                      "paymentDate":"2021-06-28"
                    },
                    {
                      "amount":385.73,
                      "instalmentNumber":20,
                      "paymentDate":"2021-07-28"
                    }
                  ],
                  "initialPaymentAmount": 0,
                  "totalNoPayments": 20,
                  "totalInterestCharged": 140.73,
                  "totalPaymentWithoutInterest": 4900
                },
                "status": "ApplicationComplete",
                "arrangementSubmissionStatus": "$arrangementSubmissionStatus",
                "paymentReference": "123ABC123",
                "utr": "6573196998"
            }"""
  )
}
