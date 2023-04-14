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

package ssttpcalculator

import akka.util.Timeout
import journey.Statuses.InProgress
import journey.{Journey, JourneyId, JourneyService}
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import testsupport.stubs.{ArrangementStub, AuthStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.{TdAll, TdRequest}
import testsupport.{ItSpec, RichMatchers, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, TypeOfAccountDetails}
import _root_.model.enumsforforms.TypesOfBankAccount.Personal
import _root_.model.enumsforforms.TypesOfBankAccount
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.mvc.Result

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

class CalculatorControllerMaxLengthPlan12 extends CalculatorControllerSpec {
  override def configuredMaxLengthOfPaymentPlan: Int = 12

  override protected def createJourneyWithMaxLengthPlan(journeyId: JourneyId): Journey = {
    createBaseJourney(journeyId)
      .copy(maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 1020))))
      .copy(maybeSpending = Some(Spending(Expenses(HousingExp, 200))))
  }

  override protected def createJourneyNoAffordablePlan(journeyId: JourneyId): Journey = {
    createBaseJourney(journeyId)
      .copy(maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 1000))))
      .copy(maybeSpending = Some(Spending(Expenses(HousingExp, 200))))
  }
}

class CalculatorControllerSpec extends ItSpec with WireMockSupport {

  def configuredMaxLengthOfPaymentPlan: Int = 24

  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  override val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> CalculatorType.Legacy.value,
    "legacyCalculatorConfig.maximumLengthOfPaymentPlan" -> configuredMaxLengthOfPaymentPlan
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(configMap)
    .build()

  val requestTimeOut = 5

  "CalculatorController.getCalculatorInstalments" - {
    "displays 'how much can you pay each month' page if at least one plan no longer than configurable maximum length" in {
      eventually(RichMatchers.timeout(Span(requestTimeOut, Seconds))) {
        val res = testGetCalculateInstalments(createJourneyWithMaxLengthPlan)
        status(res) shouldBe Status.OK
      }
    }
    "display 'we cannot agree your payment plan' page if no plan is within the configurable maximum length" in {
      eventually(RichMatchers.timeout(Span(requestTimeOut, Seconds))) {
        val res = testGetCalculateInstalments(createJourneyNoAffordablePlan)
        status(res) shouldBe Status.SEE_OTHER
        redirectLocation(res) shouldBe Some("/pay-what-you-owe-in-instalments/we-cannot-agree-your-payment-plan")
      }
    }
  }

  private def testGetCalculateInstalments(createJourney: JourneyId => Journey): Future[Result] = {
    AuthStub.authorise()

    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement
    TaxpayerStub.getTaxpayer()

    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val sessionId = UUID.randomUUID().toString
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

    val journey = createJourney(journeyId)
    val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
    journeyService.saveJourney(journey)(fakeRequest)

    val controller: CalculatorController = app.injector.instanceOf[CalculatorController]

    controller.getCalculateInstalments()(fakeRequest)
  }

  protected def createJourneyWithMaxLengthPlan(journeyId: JourneyId): Journey = {
    createBaseJourney(journeyId)
      .copy(maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 620))))
      .copy(maybeSpending = Some(Spending(Expenses(HousingExp, 200))))
  }

  protected def createJourneyNoAffordablePlan(journeyId: JourneyId): Journey = {
    createBaseJourney(journeyId)
      .copy(maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 600))))
      .copy(maybeSpending = Some(Spending(Expenses(HousingExp, 200))))
  }

  protected def createBaseJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id                       = journeyId,
      status                    = InProgress,
      createdOn                 = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails          = Some(BankDetails(Some(Personal), "111111", "12345678", "Darth Vader", None)),
      existingDDBanks           = None,
      maybeTaxpayer             = Some(TdAll.taxpayer),
      maybePaymentDayOfMonth    = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus    = Some(EligibilityStatus(Seq.empty))
    )
  }
}
