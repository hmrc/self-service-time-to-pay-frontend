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

import scala.concurrent.Future
import scala.concurrent.Future.successful

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            calculatorConnector: CalculatorConnector) extends TimeToPayController {

  val cesa: String = "CESA"
  val paymentFrequency = "Monthly"
  val paymentCurrency = "GBP"

  def getInstalmentSummary = Action.async { implicit request =>
    sessionCache.get.flatMap {
      _.fold(redirectToStart)(ttp => {
        Future.successful(Ok(showInstalmentSummary(ttp.schedule.getOrElse(throw new RuntimeException("No schedule data")),
          createDayOfForm(ttp), request)))
      })
    }
  }

  private def createDayOfForm(ttpSubmission: TTPSubmission)  = {
     ttpSubmission.paymentScheduleDayOfMonth.fold(ArrangementForm.dayOfMonthForm)(
       dayOfMonth => ArrangementForm.dayOfMonthForm.fill(dayOfMonth)
     )
  }

  private val showInstalmentSummary = instalment_plan_summary.render _

  def changeSchedulePaymentDay(): Action[AnyContent] = Action.async { implicit request =>

    ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
      formWithErrors => {
        sessionCache.get.map {
          submission => BadRequest(showInstalmentSummary(submission.get.schedule.get, formWithErrors, request))
        }
      },
      validFormData => {
        sessionCache.get.flatMap {
          _.fold(redirectToStart)(ttp => changeScheduleDay(ttp, validFormData))
        }
      })
  }

  def changeScheduleDay(ttpSubmission: TTPSubmission, formData: ArrangementDayOfMonth)(implicit hc: HeaderCarrier): Future[Result] = {

    createCalculatorInput(ttpSubmission, formData).fold(throw new RuntimeException("Could not create calculator input"))(cal => {
      calculatorConnector.calculatePaymentSchedule(cal).flatMap {
        response => {
          sessionCache.put(ttpSubmission.copy(schedule = Some(response.head), paymentScheduleDayOfMonth = Some(formData))).map {
            _ => Redirect(routes.ArrangementController.getInstalmentSummary())
          }
        }
      }
    })
  }


  def createCalculatorInput(ttpSubmission: TTPSubmission, formData: ArrangementDayOfMonth): Option[CalculatorInput] = {
    for {
      taxPayer <- ttpSubmission.taxPayer
      schedule <- ttpSubmission.schedule
      startDate <- schedule.startDate
      endDate <- schedule.endDate
      firstPaymentDate = startDate.plusMonths(1).withDayOfMonth(formData.dayOfMonth)
      input = CalculatorInput(taxPayer.selfAssessment.debits, schedule.initialPayment, startDate, Some(endDate), Some(firstPaymentDate))
    } yield input
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
        successful(Ok(application_complete.render(submission.taxpayer.get.selfAssessment.get.debits.sortBy(_.dueDate.toEpochDay()),
          submission.arrangementDirectDebit.get, submission.schedule.get, request)))
      })
    }
  }

  private def redirectToStart = successful[Result](Redirect(routes.SelfServiceTimeToPayController.start()))

  private def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        val result = for {
          ddInstruction: DirectDebitInstructionPaymentPlan <- ddConnector.createPaymentPlan(paymentPlan(submission), SaUtr(utr))
          ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, submission))
        } yield ttp

        result.flatMap {
          _.fold(error => Future.failed(new RuntimeException(s"Exception: ${error.code} + ${error.message}")), success => applicationSuccessful)
        }
      case _ => throw new RuntimeException("Taxpayer or related data not present")
    }
  }

  private def paymentPlan(submission: TTPSubmission): PaymentPlanRequest = {
    val maybePaymentPlanRequest = for {
      bankDetails <- submission.bankDetails
      schedule <- submission.schedule
      taxPayer <- submission.taxpayer
      sa <- taxPayer.selfAssessment
      utr <- sa.utr
    } yield {
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

      PaymentPlanRequest("SSTTP", LocalDate.now().toString, knownFact, instruction, pp, printFlag = true)
    }

    maybePaymentPlanRequest.getOrElse(throw new RuntimeException(s"PaymentPlanRequest creation failed - TTPSubmission: $submission"))
  }

  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    val taxpayer = submission.taxpayer.getOrElse(throw new RuntimeException("Taxpayer data not present"))
    val schedule = submission.schedule.getOrElse(throw new RuntimeException("Schedule data not present"))

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }
}