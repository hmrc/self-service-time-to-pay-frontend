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
import com.github.nscala_time.time.Imports.Duration
import journey.{Journey, JourneyId, JourneyService, PaymentToday, PaymentTodayAmount}
import journey.Statuses.InProgress
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import ssttpaffordability.model.Expense.{ChildcareExp, CouncilTaxExp, DebtRepaymentsExp, GroceriesExp, HealthExp, HousingExp, InsuranceExp, PensionContributionsExp, TravelExp, UtilitiesExp}
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import testsupport.RichMatchers.whenReady
import testsupport.WireMockSupport
import testsupport.stubs.{AuthStub, TaxpayerStub}
import testsupport.testdata.{TdAll, TdRequest}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{ArrangementDayOfMonth, BankDetails, CalculatorDuration, EligibilityStatus, TypeOfAccountDetails}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CalculatorControllerSpec2023 extends PlaySpec with GuiceOneAppPerTest with WireMockSupport {

  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  val testPort: Int = 19001

  protected lazy val configMap: Map[String, Any] = Map(
    "microservice.services.direct-debit.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port,
    "microservice.services.campaign-manager.port" -> WireMockSupport.port,
    "microservice.services.ia.port" -> WireMockSupport.port,
    "microservice.services.auth.port" -> WireMockSupport.port,
    "microservice.services.company-auth.url" -> s"http://localhost:${WireMockSupport.port}",
    "microservice.services.auth.login-callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.add-taxes.port" -> WireMockSupport.port,
    "microservice.services.bars.port" -> WireMockSupport.port,
    "microservice.services.identity-verification-frontend.uplift-url" -> s"http://localhost:${WireMockSupport.port}/mdtp/uplift",
    "microservice.services.identity-verification-frontend.callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.identity-verification-frontend.callback.complete-path" -> "/pay-what-you-owe-in-instalments/arrangement/determine-eligibility",
    "microservice.services.identity-verification-frontend.callback.reject-path" -> "/pay-what-you-owe-in-instalments/eligibility/not-enrolled")

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(configMap)
    .build()

  "getPaymentOptions" should {
    "calls calculator service to generate payment plan options and saves them in the journey" in {
      AuthStub.authorise()

      TaxpayerStub.getTaxpayer()

      val journeyId = JourneyId("62ce7631b7602426d74f83b0")
      val sessionId = UUID.randomUUID().toString
      val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

      val journey = createJourney(journeyId)
      val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
      journeyService.saveJourney(journey)(fakeRequest)

      val controller: CalculatorController = app.injector.instanceOf[CalculatorController]

      val res = controller.getPaymentPlanOptions()(fakeRequest)
      status(res) mustBe Status.OK

      val updatedJourney = journeyService.getJourney()(fakeRequest)

      whenReady(updatedJourney) { journey => journey.maybePaymentPlanOptions mustBe Some(Seq())}

    }
  }

  private def createJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id                        = journeyId,
      status                     = InProgress,
      createdOn                  = LocalDateTime.now(),
      maybeTaxpayer              = Some(TdAll.taxpayer),
      maybePaymentToday          = Some(PaymentToday(true)),
      maybePaymentTodayAmount    = Some(PaymentTodayAmount(200)),
      maybeMonthlyPaymentAmount  = Some(2000),
      maybeIncome                = Some(Income(
        IncomeBudgetLine(MonthlyIncome, 2000),
        IncomeBudgetLine(Benefits, 0),
        IncomeBudgetLine(OtherIncome, 0)
      )),
      maybeSpending              = Some(Spending(
        Expenses(HousingExp, 1000),
        Expenses(PensionContributionsExp, 0),
        Expenses(CouncilTaxExp, 0),
        Expenses(UtilitiesExp, 0),
        Expenses(DebtRepaymentsExp, 0),
        Expenses(TravelExp, 0),
        Expenses(ChildcareExp, 0),
        Expenses(InsuranceExp, 0),
        Expenses(GroceriesExp, 0),
        Expenses(HealthExp, 0)
      )),
      maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(28)),
      maybeEligibilityStatus     = Some(EligibilityStatus(Seq.empty))
    )
  }
}
