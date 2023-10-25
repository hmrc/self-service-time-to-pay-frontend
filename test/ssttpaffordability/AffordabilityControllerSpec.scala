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

package ssttpaffordability

import akka.util.Timeout
import journey.Statuses.InProgress
import journey.{Journey, JourneyId, JourneyService, PaymentToday}

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import testsupport.stubs.{AuditStub, AuthStub}
import testsupport.testdata.TdRequest.FakeRequestOps
import testsupport.{ItSpec, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.libs.json.{JsObject, Json}
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model._
import testsupport.testdata.TdAll
import uk.gov.hmrc.selfservicetimetopay.models._
import _root_.model.enumsforforms.TypesOfBankAccount

import java.time.LocalDateTime
import java.util.UUID

class AffordabilityControllerSpec extends ItSpec with WireMockSupport {

  override val overrideConfig: Map[String, Any] = Map(
    "auditing.enabled" -> true
  )

  val requestTimeOut = 5
  implicit val timeout: Timeout = Timeout(requestTimeOut.seconds)

  "AffordabilityController" - {
    ".getSetUpPlanWithAdviser" - {
      "audits and returns 200 OK" in {
        AuditStub.audit()

        val journeyId = JourneyId("62ce7631b7602426d74f83b0")
        val journey = Journey(
          _id                       = journeyId,
          status                    = InProgress,
          createdOn                 = LocalDateTime.now(),
          maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
          maybeBankDetails          = None,
          maybeTaxpayer             = Some(TdAll.taxpayer),
          maybePaymentToday         = Some(PaymentToday(false)),
          maybeIncome               = Some(Income(IncomeBudgetLine(MonthlyIncome, 2000))),
          maybeSpending             = Some(Spending(Expenses(HousingExp, 1000))),
          maybePlanSelection        = Some(PlanSelection(SelectedPlan(490))),
          maybePaymentDayOfMonth    = Some(PaymentDayOfMonth(28)),
          maybeEligibilityStatus    = Some(EligibilityStatus(Seq.empty))
        )
        val sessionId = UUID.randomUUID().toString
        val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)
        val journeyService = app.injector.instanceOf[JourneyService]
        journeyService.saveJourney(journey)(fakeRequest).futureValue shouldBe (())

        val controller: AffordabilityController = app.injector.instanceOf[AffordabilityController]

        val res = controller.getSetUpPlanWithAdviser(fakeRequest)

        status(res) shouldBe Status.OK

        AuditStub.verifyEventAudited(
          "ManualAffordabilityCheck",
          Json.parse(
            s"""
                 |{
                 |  "totalDebt" : "4900.00",
                 |  "halfDisposableIncome" : "500.00",
                 |  "income" : {
                 |   "monthlyIncomeAfterTax" : "2000.00",
                 |   "benefits" : "0.00",
                 |   "otherMonthlyIncome" : "0.00",
                 |   "totalIncome" : "2000.00"
                 |  },
                 |  "outgoings" : {
                 |   "housing" : "1000.00",
                 |   "pensionContributions" : "0.00",
                 |   "councilTax" : "0.00",
                 |   "utilities" : "0.00",
                 |   "debtRepayments" : "0.00",
                 |   "travel" : "0.00",
                 |   "childcareCosts" : "0.00",
                 |   "insurance" : "0.00",
                 |   "groceries" : "0.00",
                 |   "health" : "0.00",
                 |   "totalOutgoings" : "1000.00"
                 |  },
                 |  "status" : "Interest greater than or equal to regular payment",
                 |  "utr" : "6573196998"
                 |}""".stripMargin
          ).as[JsObject]

        )
      }
    }
  }

}
