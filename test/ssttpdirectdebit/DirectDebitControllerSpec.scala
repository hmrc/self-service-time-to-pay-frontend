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

package ssttpdirectdebit

import akka.util.Timeout
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import journey.Statuses.InProgress
import journey.{Journey, JourneyId, JourneyService, PaymentToday}
import model.enumsforforms.TypesOfBankAccount
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import ssttpaffordability.model.Expense.HousingExp
import ssttpaffordability.model.IncomeCategory.MonthlyIncome
import ssttpaffordability.model.{Expenses, Income, IncomeBudgetLine, Spending}
import testsupport.ItSpec
import testsupport.JsonSyntax.toJsonOps
import testsupport.stubs.{AuditStub, BarsStub}
import testsupport.testdata.TdAll
import uk.gov.hmrc.selfservicetimetopay.models._

import java.time.LocalDateTime
import scala.concurrent.Future

class DirectDebitControllerSpec extends ItSpec {

  override val overrideConfig: Map[String, Any] = Map("auditing.enabled" -> true)
  val requestTimeOut = 5
  implicit val timeout: Timeout = Timeout(requestTimeOut.seconds)

  "DirectDebitController.submitDirectDebit" - {
    "when receiving valid form data" - {
      "when receiving valid bank details response from Bars" - {
        "when account DOES NOT support direct debit " +
          "remains on 'Set Up Direct Debit' page" in {
            val res = testSubmitDirectDebit(BarsStub.validateBankDDNotSupported)
            status(res) shouldBe Status.BAD_REQUEST

            AuditStub.verifyEventAudited(
              "BARSCheck",
              Json.parse(
                """
                   |{
                   | "utr" : "6573196998",
                   | "request" : {
                   |   "account" : {
                   |     "accountType" : "Personal",
                   |     "accountHolderName" : "Darth Vader",
                   |     "sortCode" : "111111",
                   |     "accountNumber" : "12345678"
                   |   }
                   | },
                   | "response" : {
                   |   "isBankAccountValid" : false,
                   |   "barsResponse" : {
                   |     "accountNumberIsWellFormatted" : "Yes",
                   |     "nonStandardAccountDetailsRequiredForBacs" : "No",
                   |     "sortCodeIsPresentOnEISCD" : "Yes",
                   |     "sortCodeBankName" : "Lloyds",
                   |     "sortCodeSupportsDirectDebit" : "No",
                   |     "sortCodeSupportsDirectCredit" : "Yes",
                   |     "iban" : "GB59 HBUK 1234 5678"
                   |   }
                   | }
                   |}
                   |  """.stripMargin
              ).as[JsObject]
            )
          }
        "when account DOES support direct debit " +
          "displays 'Check your Direct Debit details' page" in {

            val res = testSubmitDirectDebit(BarsStub.validateBank)
            status(res) shouldBe Status.SEE_OTHER
            redirectLocation(res) shouldBe Some("/pay-what-you-owe-in-instalments/arrangement/direct-debit-confirmation")

            AuditStub.verifyEventAudited(
              "BARSCheck",
              Json.parse(
                """
                  |{
                  | "utr" : "6573196998",
                  | "request" : {
                  |   "account" : {
                  |     "accountType" : "Personal",
                  |     "accountHolderName" : "Darth Vader",
                  |     "sortCode" : "111111",
                  |     "accountNumber" : "12345678"
                  |   }
                  | },
                  | "response" : {
                  |   "isBankAccountValid" : true,
                  |   "barsResponse" : {
                  |     "accountNumberIsWellFormatted" : "Yes",
                  |     "nonStandardAccountDetailsRequiredForBacs" : "No",
                  |     "sortCodeIsPresentOnEISCD" : "Yes",
                  |     "sortCodeBankName" : "Lloyds",
                  |     "sortCodeSupportsDirectDebit" : "Yes",
                  |     "sortCodeSupportsDirectCredit" : "Yes",
                  |     "iban" : "GB59 HBUK 1234 5678"
                  |   }
                  | }
                  |}
                  |  """.stripMargin
              ).as[JsObject]
            )
          }

      }
    }
  }

  private def testSubmitDirectDebit(barsStub: (String, String) => StubMapping): Future[Result] = {

    val sortCode = "111111"
    val accountNumber = "12345678"

    AuditStub.audit()
    barsStub(sortCode, accountNumber)
    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val journey = createJourney(journeyId)

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

    await(journeyService.saveJourney(journey)(fakeRequest)) mustBe (())
    controller.submitDirectDebit()(fakeRequest)
  }

  private def createJourney(journeyId: JourneyId): Journey = {
    Journey(
      _id                       = journeyId,
      status                    = InProgress,
      createdOn                 = LocalDateTime.now(),
      maybeTypeOfAccountDetails = Some(TypeOfAccountDetails(TypesOfBankAccount.Personal, isAccountHolder = true)),
      maybeBankDetails          = None,
      existingDDBanks           = None,
      maybeTaxpayer             = Some(TdAll.taxpayer),
      maybePaymentToday         = Some(PaymentToday(false)),
      maybeIncome               = Some(Income(IncomeBudgetLine(MonthlyIncome, 2000))),
      maybeSpending             = Some(Spending(Expenses(HousingExp, 1000))),
      maybePlanSelection        = Some(PlanSelection(SelectedPlan(490))),
      maybePaymentDayOfMonth    = Some(PaymentDayOfMonth(28)),
      maybeEligibilityStatus    = Some(EligibilityStatus(Seq.empty))
    )
  }

}
