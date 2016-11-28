/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement.application_complete

import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            sessionCache: SessionCacheConnector) extends FrontendController {

  val cesa: String = "CESA"
  val paymentFrequency = "Monthly"
  val paymentCurrency = "GBP"

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      _.fold(redirectToStart)(arrangementSetUp)
    }
  }

  def applicationComplete(): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      _.fold(redirectToStart)(submission => {
        sessionCache.remove()
        successful(Ok(application_complete.render(submission, request)))
      })
    }
  }

  private def redirectToStart = successful[Result](Redirect(routes.SelfServiceTimeToPayController.start()))

  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {

    def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

    val utr = submission.taxPayer.selfAssessment.utr
    (for {
      ddInstruction <- ddConnector.createPaymentPlan(paymentPlan(submission), SaUtr(utr))
      ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, submission))
    } yield ttp).flatMap {
      //TODO: Waiting for failed application page
      _.fold(error => successful(Redirect("")), success => applicationSuccessful)
    }
  }

  private def paymentPlan(submission: TTPSubmission): PaymentPlanRequest = {
    val bankDetails = submission.bankDetails
    val schedule = submission.schedule
    val utr = submission.taxPayer.selfAssessment.utr
    val knownFact = List(KnownFact(cesa, utr))

    val instruction = DirectDebitInstruction(sortCode = Some(bankDetails.sortCode),
      accountNumber = Some(bankDetails.accountNumber.toString),
      creationDate = schedule.startDate,
      ddiRefNo = bankDetails.ddiRefNumber)

    val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
    val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
    val pp = PaymentPlan("Time to Pay",
      utr,
      cesa,
      paymentCurrency,
      schedule.initialPayment,
      schedule.startDate.get,
      firstInstalment.amount,
      firstInstalment.paymentDate,
      lastInstalment.paymentDate,
      paymentFrequency,
      lastInstalment.amount,
      lastInstalment.paymentDate,
      schedule.amountToPay)

    PaymentPlanRequest("SSTTP", LocalDate.now().toString, knownFact, instruction, pp, printFlag=true)
  }

  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    TTPArrangement(ppReference, ddReference, submission.taxPayer, submission.schedule)
  }

}