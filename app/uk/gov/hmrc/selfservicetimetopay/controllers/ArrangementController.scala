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
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.forms.ArrangementForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement.{application_complete, instalment_plan_summary}
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._

import scala.concurrent.Future
import scala.concurrent.Future.successful

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            calculatorConnector: CalculatorConnector) extends TimeToPayController {

  val cesa: String = "CESA"
  val paymentFrequency = "Monthly"
  val paymentCurrency = "GBP"

  def getInstalmentSummary = Action.async { implicit request =>
    sessionCache.get.map {
      case Some(submission@TTPSubmission(Some(schedule), _, _, _, _, _, _, _)) =>
        Ok(showInstalmentSummary(schedule, ArrangementForm.dayOfMonthForm, request))
      case _ => Ok(showInstalmentSummary(generatePaymentSchedules(BigDecimal.exact("5000"), None).head, ArrangementForm.dayOfMonthForm, request))
      case _ => throw new RuntimeException("No data found")
    }
  }

  private val showInstalmentSummary = instalment_plan_summary.render _

  def changeSchedulePaymentDay = Action.async { implicit request =>

    sessionCache.get.map {
      case Some(ttp) => {
        val schedule = ttp.schedule.get
        val taxPayer = ttp.taxPayer.get
        val calculatorInput =

          (for {
             schedule <- ttp.schedule
             taxPayer <- ttp.taxPayer
             calculatorInput = createInput(schedule, taxPayer)
             newSchedule <- calculatorConnector.calculatePaymentSchedule(calculatorInput)
          } yield newSchedule )

      }
      case _ => throw new RuntimeException("No data found")
    }

//   play.api.mvc.Call Future(
//      ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
//        formWithErrors => BadRequest(routes.ArrangementController.getInstalmentSummary()),
//        _ =>
//          calculatorConnector.calculatePaymentSchedule()
//          Redirect(routes.ArrangementController.getInstalmentSummary()))
//    )
  }

  def submitInstalmentSchedule = Action.async { implicit request =>
    Future(ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
      formWithErrors => BadRequest(showInstalmentSummary(generatePaymentSchedules(BigDecimal.exact("5000"), None).head, formWithErrors, request)),
      _ => Redirect(routes.DirectDebitController.getDirectDebit()))
    )
  }

  def submit(): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      _.fold(redirectToStart)(arrangementSetUp)
    }
  }

  def applicationComplete(): Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.flatMap {
      _.fold(redirectToStart)(submission => {
        sessionCache.remove()
        successful(Ok(application_complete.render(submission.taxPayer.get.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()),
          submission.arrangementDirectDebit.get, submission.schedule.get, request)))
      })
    }
  }

  private def redirectToStart = successful[Result](Redirect(routes.SelfServiceTimeToPayController.start()))

  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {

    def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

    val utr = submission.taxPayer.getOrElse(throw new RuntimeException("Taxpayer data not present")).selfAssessment.utr

    val result = for {
      ddInstruction: DirectDebitInstructionPaymentPlan <- ddConnector.createPaymentPlan(paymentPlan(submission), SaUtr(utr.get))
      ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, submission))
    } yield ttp

    result.flatMap {
      _.fold(error => Future.failed(new RuntimeException(s"Exception: ${error.code} + ${error.message}")), success => applicationSuccessful)
    }
  }

  private def paymentPlan(submission: TTPSubmission): PaymentPlanRequest = {
    val maybePaymentPlanRequest = for {
      bankDetails <- submission.bankDetails
      schedule <- submission.schedule
      taxPayer <- submission.taxPayer
    } yield {
      val utr = taxPayer.selfAssessment.utr
      val knownFact = List(KnownFact(cesa, utr.get))
      val instruction = DirectDebitInstruction(sortCode = Some(bankDetails.sortCode),
        accountNumber = Some(bankDetails.accountNumber.toString),
        creationDate = schedule.startDate,
        ddiRefNo = bankDetails.ddiRefNumber)

      val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
      val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
      val pp = PaymentPlan("Time to Pay",
        utr.get,
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

      PaymentPlanRequest("SSTTP", LocalDate.now().toString, knownFact, instruction, pp, printFlag = true)
    }

    maybePaymentPlanRequest.getOrElse(throw new RuntimeException(s"PaymentPlanRequest creation failed - TTPSubmission: $submission"))
  }

  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    val taxpayer = submission.taxPayer.getOrElse(throw new RuntimeException("Taxpayer data not present"))
    val schedule = submission.schedule.getOrElse(throw new RuntimeException("Schedule data not present"))

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }
}