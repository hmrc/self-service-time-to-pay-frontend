
package ssttpcalculator

import akka.util.Timeout
import journey.Statuses.InProgress
import journey.{Journey, JourneyId, JourneyService, PaymentToday, PaymentTodayAmount}
import org.scalatest.Assertion
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import testsupport.RichMatchers.eventually
import testsupport.stubs.{ArrangementStub, AuthStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.TdAll.selectedRegularPaymentAmount300
import testsupport.testdata.{TdAll, TdRequest}
import testsupport.{RichMatchers, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, PlanSelection, SelectedPlan, TypeOfAccountDetails}
import _root_.model.enumsforforms.TypesOfBankAccount.Personal
import _root_.model.enumsforforms.TypesOfBankAccount
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.http.Status.{OK, SEE_OTHER}

import java.time.LocalDateTime
import java.util.UUID

class CalculatorControllerSpec extends PlaySpec with GuiceOneAppPerTest with WireMockSupport {

  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  val testPort: Int = 19001

  val overrideConfig: Map[String, Any] = Map(
    "calculatorType" -> CalculatorType.Legacy.value
  )

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
    "microservice.services.identity-verification-frontend.callback.reject-path" -> "/pay-what-you-owe-in-instalments/eligibility/not-enrolled") ++ overrideConfig

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(configMap)
    .build()

  val testConfigMaxLengths: Seq[Int] = Seq(6, 12, 24)


  "CalculatorController.getCalculatorInstalments" should {
    "display 'how much can you pay each month' page if at least one plan no longer than configurable maximum length" when {
      testConfigMaxLengths.foreach { configuredMaxLength =>
        s"maximum length of payment plan is set to $configuredMaxLength months" in {
          testControllerRoutingToHowMuchCanYouPay(configuredMaxLength)
        }
      }
    }
    "display 'we cannot agree your payment plan' page if no plan is within the configurable maximum length" when {
      testConfigMaxLengths.foreach { configuredMaxLength =>
        s"maximum length of payment plan is set to $configuredMaxLength months" in {
          testControllerRoutingToCannotAgreePlan(configuredMaxLength)
        }
      }
    }
  }

  def testControllerRoutingToHowMuchCanYouPay (configuredMaxLength: Int): Assertion = {
    AuthStub.authorise()

    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement
    TaxpayerStub.getTaxpayer()

    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val sessionId = UUID.randomUUID().toString
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

    val journey = createSuccessJourney(journeyId)
    val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
    journeyService.saveJourney(journey)(fakeRequest)

    val controller: CalculatorController = app.injector.instanceOf[CalculatorController]



    eventually(RichMatchers.timeout(Span(5, Seconds))) {
      val res = controller.getCalculateInstalments()(fakeRequest)
      status(res) mustBe Status.OK
    }
  }


  def testControllerRoutingToCannotAgreePlan(configuredMaxLength: Int): Assertion = {
    AuthStub.authorise()

    DirectDebitStub.postPaymentPlan
    ArrangementStub.postTtpArrangement
    TaxpayerStub.getTaxpayer()

    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val sessionId = UUID.randomUUID().toString
    val fakeRequest = FakeRequest().withAuthToken().withSession(SessionKeys.sessionId -> sessionId, "ssttp.journeyId" -> journeyId.toHexString)

    val journey = createFailJourney(journeyId)
    val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
    journeyService.saveJourney(journey)(fakeRequest)

    val controller: CalculatorController = app.injector.instanceOf[CalculatorController]


    eventually(RichMatchers.timeout(Span(5, Seconds))) {
      val res = controller.getCalculateInstalments()(fakeRequest)
      println(s"res: $res")
      status(res) mustBe Status.SEE_OTHER
      redirectLocation(res) mustBe Some("/pay-what-you-owe-in-instalments/we-cannot-agree-your-payment-plan")

    }
  }

  private def createSuccessJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id = journeyId,
      status = InProgress,
      createdOn = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails = Some(BankDetails(Some(Personal), "111111", "12345678", "Darth Vader", None)),
      existingDDBanks = None,
      maybeTaxpayer = Some(TdAll.taxpayer),
      maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 1000))),
      maybeSpending = Some(Spending(Expenses(HousingExp, 200))),
      maybePaymentDayOfMonth = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus = Some(EligibilityStatus(Seq.empty))
    )
  }

  private def createFailJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id = journeyId,
      status = InProgress,
      createdOn = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails = Some(BankDetails(Some(Personal), "111111", "12345678", "Darth Vader", None)),
      existingDDBanks = None,
      maybeTaxpayer = Some(TdAll.taxpayer),
      maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 300))),
      maybeSpending = Some(Spending(Expenses(HousingExp, 200))),
      maybePaymentDayOfMonth = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus = Some(EligibilityStatus(Seq.empty))
    )
  }
}
