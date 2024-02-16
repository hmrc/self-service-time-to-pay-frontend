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

package ssttparrangement

import org.apache.pekko.util.Timeout
import journey.Statuses.ApplicationComplete
import journey.{Journey, JourneyId, JourneyService}
import model.enumsforforms.TypesOfBankAccount.Personal
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, status, redirectLocation}
import testsupport.stubs.{ArrangementStub, AuditStub, DirectDebitStub, TaxpayerStub}
import testsupport.testdata.{TdRequest, TestJourney}
import testsupport.{FakeAuthConnector, WireMockSupport}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.selfservicetimetopay.models.{BankDetails, DirectDebitInstruction}

import java.util.UUID

class ArrangementControllerSpec extends PlaySpec with GuiceOneAppPerTest with WireMockSupport {
  import TdRequest._

  implicit val timeout: Timeout = Timeout(5.seconds)

  val testPort: Int = 19001

  val requestTimeOut = 10

  protected lazy val configMap: Map[String, Any] = Map(
    "microservice.services.direct-debit.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-arrangement.port" -> WireMockSupport.port,
    "microservice.services.time-to-pay-taxpayer.port" -> WireMockSupport.port,
    "microservice.services.campaign-manager.port" -> WireMockSupport.port,
    "microservice.services.auth.port" -> WireMockSupport.port,
    "microservice.services.company-auth.url" -> s"http://localhost:${WireMockSupport.port}",
    "microservice.services.auth.login-callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.add-taxes.port" -> WireMockSupport.port,
    "microservice.services.bars.port" -> WireMockSupport.port,
    "microservice.services.identity-verification-frontend.uplift-url" -> s"http://localhost:${WireMockSupport.port}/mdtp/uplift",
    "microservice.services.identity-verification-frontend.callback.base-url" -> s"http://localhost:$testPort",
    "microservice.services.identity-verification-frontend.callback.complete-path" -> "/pay-what-you-owe-in-instalments/arrangement/determine-eligibility",
    "microservice.services.identity-verification-frontend.callback.reject-path" -> "/pay-what-you-owe-in-instalments/eligibility/not-enrolled",
    "auditing.consumer.baseUri.port" -> WireMockSupport.port,
    "auditing.enabled" -> true,
    "logger.root" -> "WARN"
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].toInstance(new FakeAuthConnector))
    .configure(configMap)
    .build()

  class Context(journeyOverride: Option[JourneyId => Journey] = None, frozenDateTime: String = "2023-09-27T09:00:00.000") {
    AuditStub.audit()

    val journeyId = JourneyId("62ce7631b7602426d74f83b0")
    val sessionId = UUID.randomUUID().toString
    val fakeRequest =
      FakeRequest()
        .withAuthToken()
        .withSession(
          SessionKeys.sessionId -> sessionId,
          "ssttp.journeyId" -> journeyId.toHexString,
          "ssttp.frozenDateTime" -> frozenDateTime
        )

    val journey = journeyOverride.map(_(journeyId)).getOrElse(TestJourney.createJourney(journeyId))
    val journeyService: JourneyService = app.injector.instanceOf[JourneyService]
    await(journeyService.saveJourney(journey)) mustBe (())

    val controller: ArrangementController = app.injector.instanceOf[ArrangementController]
  }

  "ArrangementController" when {
    ".submit" should {
      "be able to submit a new arrangement" in new Context() {
        DirectDebitStub.postPaymentPlan
        ArrangementStub.postTtpArrangement
        TaxpayerStub.getTaxpayer()

        val res = controller.submit()(fakeRequest)
        status(res) mustBe Status.SEE_OTHER
        res.value.get.get.header.headers("Location") mustBe "/pay-what-you-owe-in-instalments/arrangement/summary"

        AuditStub.verifyEventAudited(
          "ManualAffordabilityPlanSetUp",
          Json.parse(
            """
              |{
              |  "bankDetails" : {
              |    "name" : "Darth Vader",
              |    "accountNumber" : "12345678",
              |    "sortCode" : "111111"
              |  },
              |  "halfDisposableIncome" : "750.00",
              |  "income" : {
              |    "monthlyIncomeAfterTax" : "2000.00",
              |    "benefits" : "0.00",
              |    "otherMonthlyIncome" : "0.00",
              |    "totalIncome" : "2000.00"
              |  },
              |  "outgoings" : {
              |    "housing" : "500.00",
              |    "pensionContributions" : "0.00",
              |    "councilTax" : "0.00",
              |    "utilities" : "0.00",
              |    "debtRepayments" : "0.00",
              |    "travel" : "0.00",
              |    "childcareCosts" : "0.00",
              |    "insurance" : "0.00",
              |    "groceries" : "0.00",
              |    "health" : "0.00",
              |    "totalOutgoings" : "500.00"
              |  },
              |  "selectionType" : "customAmount",
              |  "lessThanOrMoreThanTwelveMonths" : "twelveMonthsOrLess",
              |  "schedule" : {
              |    "totalPayable" : "6520.95",
              |    "instalmentDate" : 3,
              |    "instalments" : [ {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 1,
              |      "paymentDate" : "2023-11-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 2,
              |      "paymentDate" : "2023-12-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 3,
              |      "paymentDate" : "2024-01-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 4,
              |      "paymentDate" : "2024-02-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 5,
              |      "paymentDate" : "2024-03-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 6,
              |      "paymentDate" : "2024-04-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 7,
              |      "paymentDate" : "2024-05-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 8,
              |      "paymentDate" : "2024-06-03"
              |    }, {
              |      "amount" : "470.00",
              |      "instalmentNumber" : 9,
              |      "paymentDate" : "2024-07-03"
              |    }, {
              |      "amount" : "2090.95",
              |      "instalmentNumber" : 10,
              |      "paymentDate" : "2024-08-03"
              |    } ],
              |    "initialPaymentAmount" : "200.00",
              |    "totalNoPayments" : 11,
              |    "totalInterestCharged" : "1620.95",
              |    "totalPaymentWithoutInterest" : "4900.00"
              |  }
              |}
              |""".stripMargin
          ).as[JsObject]
        )
      }
    }
    ".getCheckPaymentPlan" should {
      "return a 200 and audits a successful Manual Affordability event, when leftover income passes check" in new Context(
        frozenDateTime = "2020-05-02T09:00:00.000"
      ) {
        val res = controller.getCheckPaymentPlan()(fakeRequest)
        status(res) mustBe Status.OK
        AuditStub.verifyEventAudited(
          "ManualAffordabilityCheck",
          Json.parse(
            """
              |{
              | "income" : {
              |   "monthlyIncomeAfterTax" : "2000.00",
              |   "benefits" : "0.00",
              |   "otherMonthlyIncome" : "0.00",
              |   "totalIncome" : "2000.00"
              |   },
              | "outgoings" : {
              |   "housing" : "500.00",
              |   "pensionContributions" : "0.00",
              |   "councilTax" : "0.00",
              |   "utilities" : "0.00",
              |   "debtRepayments" : "0.00",
              |   "travel" : "0.00",
              |   "childcareCosts" : "0.00",
              |   "insurance" : "0.00",
              |   "groceries" : "0.00",
              |   "health" : "0.00",
              |   "totalOutgoings" : "500.00"
              |   },
              | "status" : "Pass",
              | "utr" : "6573196998"
              |}
              |""".stripMargin
          ).as[JsObject]
        )
      }
    }

    ".applicationComplete" should {
      "show the arrangement set up page" in new Context(journeyOverride = Some {
        TestJourney.createJourney(_).copy(
          status                           = ApplicationComplete,
          maybeArrangementSubmissionStatus = Some(ArrangementSubmissionStatus.Success)
        )
      }) {
        val res = controller.applicationComplete()(fakeRequest)
        status(res) mustBe Status.OK

        AuditStub.verifyNothingAudited()
      }
      "redirect to /delete-answers if no journey id in the session is found" in {
        val controller: ArrangementController = app.injector.instanceOf[ArrangementController]

        val result = controller.applicationComplete()(FakeRequest())

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TimeoutController.killSession.url)
      }
      "redirect to /delete-answers if no journey is found for the session's journey id" in {
        val controller: ArrangementController = app.injector.instanceOf[ArrangementController]
        val unusedJourneyId = "51ba6742c7602426d74f84c0"
        val result = controller.applicationComplete()(FakeRequest().withSession("ssttp.journeyId" -> unusedJourneyId))

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TimeoutController.killSession.url)
      }
    }
    ".viewPaymentPlan" should {
      "redirect to /delete-answers if no journey id in the session is found" in {
        val controller: ArrangementController = app.injector.instanceOf[ArrangementController]

        val result = controller.viewPaymentPlan()(FakeRequest())

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TimeoutController.killSession.url)
      }
      "redirect to /delete-answers if no journey is found for the session's journey id" in {
        val controller: ArrangementController = app.injector.instanceOf[ArrangementController]
        val unusedJourneyId = "51ba6742c7602426d74f84c0"
        val result = controller.viewPaymentPlan()(FakeRequest().withSession("ssttp.journeyId" -> unusedJourneyId))

        status(result) mustBe Status.SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TimeoutController.killSession.url)
      }

    }
    ".paymentPlan" should {

      "return payment plan request with submission date to nearest millisecond (not micro-second)" in new Context() {
        val ddInstruction = DirectDebitInstruction(None, None, None)

        val result = controller.paymentPlan(journey, ddInstruction)(fakeRequest)

        result.submissionDateTime must endWith regex "\\.\\d{3}Z$"
      }

      "return payment plan request with account number left padded from 6 to 8 digits with zeros" in new Context(journeyOverride = Some {
        TestJourney.createJourney(_).copy(
          maybeBankDetails = Some(BankDetails(Some(Personal), "111111", "123456", "Darth Vader", None))
        )
      }) {
        val result = controller.makePaymentPlanRequest(journey)(fakeRequest)

        result.directDebitInstruction.accountNumber.exists(_.matches("^00\\d{6}$")) mustBe true
      }

      "return payment plan request with account number left padded from 7 to 8 digits with zeros" in new Context(journeyOverride = Some {
        TestJourney.createJourney(_).copy(
          maybeBankDetails = Some(BankDetails(Some(Personal), "111111", "1234567", "Darth Vader", None))
        )
      }) {
        val result = controller.makePaymentPlanRequest(journey)(fakeRequest)

        result.directDebitInstruction.accountNumber.exists(_.matches("^0\\d{7}$")) mustBe true
      }

      "return payment plan request with account number with 8 digits" in new Context() {
        val result = controller.makePaymentPlanRequest(journey)(fakeRequest)

        result.directDebitInstruction.accountNumber.map(_.length) mustBe Some(8)
      }
    }
    ".getCheckPaymentPlan" should {

      "display 'Set up a payment plan with an adviser' page if selected plan fails NDDS validation" in new Context() {
        val res = controller.getCheckPaymentPlan()(fakeRequest)
        status(res) mustBe Status.SEE_OTHER
        res.value.get.get.header.headers("Location") mustBe "/pay-what-you-owe-in-instalments/set-up-payment-plan-adviser"
      }

      "display 'Check your payment plan' page if selected plan passes NDDS validation" in new Context() {
        override val fakeRequest =
          FakeRequest()
            .withAuthToken()
            .withSession(
              SessionKeys.sessionId -> sessionId,
              "ssttp.journeyId" -> journeyId.toHexString,
              "ssttp.frozenDateTime" -> "2020-06-09T00:00:00.880"
            )

        val res = controller.getCheckPaymentPlan()(fakeRequest)
        status(res) mustBe Status.OK
      }
    }
  }

}
