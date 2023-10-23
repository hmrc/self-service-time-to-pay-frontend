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
import journey.{Journey, JourneyId, JourneyService}
import journey.Statuses.InProgress
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import ssttpcalculator.model.AddWorkingDaysResult
import testsupport.{ItSpec, WireMockSupport}
import testsupport.stubs.{AuthStub, DateCalculatorStub}
import testsupport.testdata.{TdAll, TdRequest}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, TypeOfAccountDetails}
import _root_.model.enumsforforms.TypesOfBankAccount
import play.api.inject.bind
import uk.gov.hmrc.auth.core.AuthConnector

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration._

class CalculatorControllerDateCalculatorDisabledSpec extends ItSpec with WireMockSupport {

  def configuredMaxLengthOfPaymentPlan: Int = 24

  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  override val overrideConfig: Map[String, Any] = Map(
    "legacyCalculatorConfig.maximumLengthOfPaymentPlan" -> configuredMaxLengthOfPaymentPlan,
    "features.call-date-calculator-service" -> false
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].toInstance(fakeAuthConnector))
    .configure(configMap)
    .build()

  lazy val controller = app.injector.instanceOf[CalculatorController]
  lazy val journeyService: JourneyService = app.injector.instanceOf[JourneyService]

  "The date calculator service should not be called if not enabled" in {
    val today = LocalDate.now()
    val addWorkingDaysResult = AddWorkingDaysResult(today, 5, today.plusDays(10))

    val (journey, fakeRequest) = saveJourney(createJourneyWithMaxLengthPlan)

    val res = controller.getCalculateInstalments()(fakeRequest)
    status(res) shouldBe Status.OK

    DateCalculatorStub.verifyAddWorkingDaysNotCalled()

    // journey should be updated
    journey.maybeDateFirstPaymentCanBeTaken shouldBe None
    journeyService.getJourney()(fakeRequest).futureValue.maybeDateFirstPaymentCanBeTaken shouldBe Some(addWorkingDaysResult)

  }

  protected def createJourneyWithMaxLengthPlan(journeyId: JourneyId): Journey = {
    createBaseJourney(journeyId)
      .copy(maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 620))))
      .copy(maybeSpending = Some(Spending(Expenses(HousingExp, 200))))
  }

  protected def createBaseJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id                       = journeyId,
      status                    = InProgress,
      createdOn                 = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails          = Some(BankDetails(Some(TypesOfBankAccount.Personal), "111111", "12345678", "Darth Vader", None)),
      maybeTaxpayer             = Some(TdAll.taxpayer),
      maybePaymentDayOfMonth    = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus    = Some(EligibilityStatus(Seq.empty))
    )
  }

  private def saveJourney(createJourney: JourneyId => Journey): (Journey, FakeRequest[AnyContentAsEmpty.type]) = {
    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

    val journey = createJourney(journeyId)
    val saveJourneyResult = journeyService.saveJourney(journey)(fakeRequest)

    saveJourneyResult.futureValue shouldBe (())
    journey -> fakeRequest
  }

}
