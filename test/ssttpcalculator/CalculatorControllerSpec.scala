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
import testsupport.stubs.{ArrangementStub, AuthStub, DateCalculatorStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.{TdAll, TdRequest}
import testsupport.{ItSpec, RichMatchers, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, TypeOfAccountDetails}
import _root_.model.enumsforforms.TypesOfBankAccount.Personal
import _root_.model.enumsforforms.TypesOfBankAccount
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import ssttpcalculator.model.AddWorkingDaysResult

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
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

  lazy val controller = app.injector.instanceOf[CalculatorController]
  lazy val journeyService: JourneyService = app.injector.instanceOf[JourneyService]

  val today = LocalDate.now()
  val expectedNumberOfWorkingDaysToAdd = 5
  val addWorkingDaysResponse: LocalDate = LocalDate.now().plusDays(10)
  val dateCalculatorResponse = Json.parse(
    s"""{ "result": "${addWorkingDaysResponse.format(DateTimeFormatter.ISO_DATE)}" }"""
  )
  val addWorkingDaysResult = AddWorkingDaysResult(today, expectedNumberOfWorkingDaysToAdd, addWorkingDaysResponse)

  "CalculatorController.getCalculatorInstalments" - {
    "displays 'how much can you pay each month' page if at least one plan no longer than configurable maximum length" in {
      AuthStub.authorise()
      DateCalculatorStub.stubAddWorkingDays(Right(dateCalculatorResponse))

      val (journey, fakeRequest) = saveJourney(createJourneyWithMaxLengthPlan)

      val res = controller.getCalculateInstalments()(fakeRequest)
      status(res) shouldBe Status.OK

      DateCalculatorStub.verifyAddWorkingDaysCalled(today, expectedNumberOfWorkingDaysToAdd)

      // journey should be updated
      journey.dateFirstPaymentCanBeTaken shouldBe None
      journeyService.getJourney()(fakeRequest).futureValue.dateFirstPaymentCanBeTaken shouldBe Some(addWorkingDaysResult)
    }

    "display 'we cannot agree your payment plan' page if no plan is within the configurable maximum length" in {
      AuthStub.authorise()
      DateCalculatorStub.stubAddWorkingDays(Right(dateCalculatorResponse))

      val (journey, fakeRequest) = saveJourney(createJourneyNoAffordablePlan)

      val res = controller.getCalculateInstalments()(fakeRequest)
      redirectLocation(res) shouldBe Some("/pay-what-you-owe-in-instalments/we-cannot-agree-your-payment-plan")

      DateCalculatorStub.verifyAddWorkingDaysCalled(today, expectedNumberOfWorkingDaysToAdd)

      // journey should be updated
      journey.dateFirstPaymentCanBeTaken shouldBe None
      journeyService.getJourney()(fakeRequest).futureValue.dateFirstPaymentCanBeTaken shouldBe Some(addWorkingDaysResult)
    }

    "return an error if the request to add working days is not successful" in {
      AuthStub.authorise()
      DateCalculatorStub.stubAddWorkingDays(Left(422))

      val (journey, fakeRequest) = saveJourney(createJourneyNoAffordablePlan)

      val error = intercept[Exception](controller.getCalculateInstalments()(fakeRequest).futureValue)
      error.getCause.getMessage shouldBe "Call to date-calculator came back with unexpected http status 422"

      DateCalculatorStub.verifyAddWorkingDaysCalled(today, expectedNumberOfWorkingDaysToAdd)
      // journey should not be updated
      journey.dateFirstPaymentCanBeTaken shouldBe None
      journeyService.getJourney()(fakeRequest).futureValue.dateFirstPaymentCanBeTaken shouldBe None
    }

    "return an error if the response to add working days cannot be parsed" in {
      AuthStub.authorise()
      DateCalculatorStub.stubAddWorkingDays(Right(JsString("Hi!")))

      val (journey, fakeRequest) = saveJourney(createJourneyNoAffordablePlan)

      val error = intercept[Exception](controller.getCalculateInstalments()(fakeRequest).futureValue)
      error.getCause.getMessage shouldBe "Could not parse date calculator response"

      DateCalculatorStub.verifyAddWorkingDaysCalled(today, expectedNumberOfWorkingDaysToAdd)
      // journey should not be updated
      journey.dateFirstPaymentCanBeTaken shouldBe None
      journeyService.getJourney()(fakeRequest).futureValue.dateFirstPaymentCanBeTaken shouldBe None
    }

    "should not call the date calculator service if a calculation has already been stored in the journey" in {
      AuthStub.authorise()

      val (_, fakeRequest) = saveJourney(createJourneyWithMaxLengthPlan(_).copy(dateFirstPaymentCanBeTaken = Some(addWorkingDaysResult)))
      val res = controller.getCalculateInstalments()(fakeRequest)
      status(res) shouldBe Status.OK

      DateCalculatorStub.verifyAddWorkingDaysNotCalled()
    }

  }

  private def saveJourney(createJourney: JourneyId => Journey): (Journey, FakeRequest[AnyContentAsEmpty.type]) = {
    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

    val journey = createJourney(journeyId)
    val saveJourneyResult = journeyService.saveJourney(journey)(fakeRequest)

    saveJourneyResult.futureValue shouldBe (())
    journey -> fakeRequest
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
      maybeTaxpayer             = Some(TdAll.taxpayer),
      maybePaymentDayOfMonth    = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus    = Some(EligibilityStatus(Seq.empty))
    )
  }
}
