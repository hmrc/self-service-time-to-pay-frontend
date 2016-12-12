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
import views.html.selfservicetimetopay.arrangement.{application_complete, instalment_plan_summary, instalment_plan_summary_print}

import scala.concurrent.Future
import scala.concurrent.Future.successful

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            calculatorConnector: CalculatorConnector,
                            taxPayerConnector: TaxPayerConnector) extends TimeToPayController {

  val cesa: String = "CESA"
  val paymentFrequency = "Monthly"
  val paymentCurrency = "GBP"


  def determineMisalignment: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      val sa = authContext.principal.accounts.sa.get

      taxPayerConnector.getTaxPayer(sa.utr.utr).flatMap {
        _.fold(throw new RuntimeException("No taxpayer found"))(t => {
          updateOrCreateInCache(found => found.copy(taxpayer = Some(t)),
            () => TTPSubmission(taxpayer = Some(t)))
            .map(cm => cm.getEntry("ttpSubmission")(submissionFormatter).get).map {
              case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(meDebits, _, _, _, _, _)) =>
                if (areEqual(tpSA.debits, meDebits)) {
                  Redirect(routes.ArrangementController.getInstalmentSummary())
                } else {
                  Redirect(routes.CalculatorController.getMisalignmentPage())
                }
              case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, _) =>
                Redirect(routes.ArrangementController.getInstalmentSummary())
              case _ =>
                Redirect(routes.SelfServiceTimeToPayController.start())
            }
        })
      }
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  def getInstalmentSummary: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      sessionCache.get.flatMap {
        _.fold(redirectToStart)(ttp => {
          Future.successful(Ok(showInstalmentSummary(ttp.schedule.getOrElse(throw new RuntimeException("No schedule data")),
            createDayOfForm(ttp), request)))
        })
      }
  }


  def getInstalmentSummaryPrint: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      sessionCache.get.flatMap {
        _.fold(redirectToStart)(ttp => {
          Future.successful(Ok(instalment_plan_summary_print.render(ttp.schedule.getOrElse(throw new RuntimeException("No schedule data")), request)))
        })
      }
  }

  private def createDayOfForm(ttpSubmission: TTPSubmission) = {
    ttpSubmission.calculatorData.firstPaymentDate.fold(ArrangementForm.dayOfMonthForm)(p => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.getDayOfMonth))
    })
  }

  private val showInstalmentSummary = instalment_plan_summary.render _

  def changeSchedulePaymentDay(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
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
          sessionCache.put(ttpSubmission.copy(schedule = Some(response.head), calculatorData = cal)).map {
            _ => Redirect(routes.ArrangementController.getInstalmentSummary())
          }
        }
      }
    })
  }

  def createCalculatorInput(ttpSubmission: TTPSubmission, formData: ArrangementDayOfMonth): Option[CalculatorInput] = {
    for {
      schedule <- ttpSubmission.schedule
      startDate <- schedule.startDate
      firstPaymentDate = startDate.plusMonths(1).withDayOfMonth(formData.dayOfMonth)
      input = ttpSubmission.calculatorData.copy(firstPaymentDate = Some(firstPaymentDate))
    } yield input
  }

  def submit(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      sessionCache.get.flatMap {
        _.fold(redirectToStart)(arrangementSetUp)
      }
  }

  def applicationComplete(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
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
        ddiRefNumber = bankDetails.ddiRefNumber)

      val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
      val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
      val pp = PaymentPlan( ppType = "Time to Pay",
        paymentReference = utr,
        hodService = cesa,
        paymentCurrency = paymentCurrency,
        initialPaymentAmount = schedule.initialPayment.toString(),
        initialPaymentStartDate = schedule.startDate.get,
        scheduledPaymentAmount = firstInstalment.amount.toString(),
        scheduledPaymentStartDate = firstInstalment.paymentDate,
        scheduledPaymentEndDate = lastInstalment.paymentDate,
        scheduledPaymentFrequency = paymentFrequency,
        balancingPaymentAmount = lastInstalment.amount.toString(),
        balancingPaymentDate = lastInstalment.paymentDate,
        totalLiability = schedule.amountToPay.toString())

      PaymentPlanRequest("SSTTP", LocalDate.now().toString, knownFact, instruction, pp, printFlag = true)
    }

    maybePaymentPlanRequest.getOrElse(throw new RuntimeException(s"PaymentPlanRequest creation failed - TTPSubmission: $submission"))
  }

  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiReferenceNo.get
    val taxpayer = submission.taxpayer.getOrElse(throw new RuntimeException("Taxpayer data not present"))
    val schedule = submission.schedule.getOrElse(throw new RuntimeException("Schedule data not present"))

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }
}