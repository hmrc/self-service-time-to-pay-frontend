
package ssttpdirectdebit

import akka.util.Timeout
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import journey.Statuses.InProgress
import journey.{Journey, JourneyId, JourneyService, PaymentToday}
import model.enumsforforms.TypesOfBankAccount
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import testsupport.JsonSyntax.toJsonOps
import testsupport.{ItSpec, RichMatchers}
import testsupport.stubs.{ArrangementStub, AuthStub, BarsStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.TdAll
import testsupport.testdata.TdRequest.FakeRequestOps
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, EligibilityStatus, PaymentDayOfMonth, PlanSelection, SelectedPlan, TypeOfAccountDetails}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DirectDebitControllerSpec extends ItSpec {

  val requestTimeOut = 5
  implicit val timeout: Timeout = Timeout(requestTimeOut.seconds)


  "DirectDebitController.submitDirectDebit" - {
    "when receiving valid form data" - {
      "when receiving valid bank details response from Bars" - {
        "when account DOES NOT support direct debit " +
          "remains on 'Set Up Direct Debit' page" in {
          eventually(RichMatchers.timeout(Span(requestTimeOut, Seconds))) {
            val res = testSubmitDirectDebit(BarsStub.validateBankDDNotSupported)
              status(res) shouldBe Status.BAD_REQUEST
          }
        }
        "when account DOES support direct debit " +
          "displays 'Check your Direct Debit details' page" in {
          eventually(RichMatchers.timeout(Span(requestTimeOut, Seconds))) {
            val res = testSubmitDirectDebit(BarsStub.validateBank)
            status(res) shouldBe Status.SEE_OTHER
            redirectLocation(res) shouldBe Some("/pay-what-you-owe-in-instalments/arrangement/direct-debit-confirmation")
          }

        }
      }
    }
  }

  private def testSubmitDirectDebit(barsStub: (String, String) => StubMapping): Future[Result] = {
    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val journey = createJourney(journeyId)

    val sortCode = "111111"
    val accountNumber = "12345678"

    val fakeRequest = FakeRequest()
      .withSession(
        "ssttp.journeyId" -> journeyId.toHexString
      )
      .withJsonBody(
        s"""{
              "accountName": "Darth Vader",
              "sortCode": $sortCode,
              "accountNumber": $accountNumber
            }""".asJson

      )

    val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
    val controller: DirectDebitController = app.injector.instanceOf[DirectDebitController]

    journeyService.saveJourney(journey)(fakeRequest)
      .flatMap{ _ =>
        barsStub(sortCode, accountNumber)
        controller.submitDirectDebit()(fakeRequest)
      }
  }

  private def createJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id = journeyId,
      status = InProgress,
      createdOn = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails = None,
      existingDDBanks = None,
      maybeTaxpayer = Some(TdAll.taxpayer),
      maybePaymentToday = Some(PaymentToday(false)),
      maybeIncome = Some(Income(IncomeBudgetLine(MonthlyIncome, 2000))),
      maybeSpending = Some(Spending(Expenses(HousingExp, 1000))),
      maybePlanSelection = Some(PlanSelection(SelectedPlan(490))),
      maybePaymentDayOfMonth = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus = Some(EligibilityStatus(Seq.empty))
    )
  }

}
