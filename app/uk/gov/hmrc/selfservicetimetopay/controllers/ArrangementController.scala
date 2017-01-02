/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.time.{LocalDate, ZonedDateTime}

import play.api.Logger
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
import scala.math.BigDecimal

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            calculatorConnector: CalculatorConnector,
                            taxPayerConnector: TaxPayerConnector,
                            eligibilityConnector: EligibilityConnector) extends TimeToPayController {
  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def eligibilityCheck(taxpayer: Taxpayer)(implicit hc: HeaderCarrier): Future[Result] = {
    eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), taxpayer)).flatMap[Result] {
      case EligibilityStatus(true, _) => Future.successful(Redirect(routes.ArrangementController.getInstalmentSummary()))
      case EligibilityStatus(_, reasons) =>
        Logger.info(s"Failed eligibility check because: $reasons")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs()))
    }
  }

  def determineMisalignment: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        val sa = authContext.principal.accounts.sa.get

        taxPayerConnector.getTaxPayer(sa.utr.utr).flatMap[Result] {
          _.fold(Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs())))(t => {

            sessionCache.get.flatMap[Result] {
              _.fold(redirectToStart)(ttp => {
                val newSubmission = ttp.copy(taxpayer = Some(t))
                newSubmission match {
                  case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(empty@Seq(), _, _, _, _, _), _) =>
                    sessionCache.put(newSubmission.copy(calculatorData = CalculatorInput(startDate = LocalDate.now(),
                      endDate = LocalDate.now().plusMonths(3).minusDays(1), debits = tpSA.debits))).map[Result] {
                      _ => Redirect(routes.CalculatorController.getPaymentToday())
                    }
                  case TTPSubmission(None, _, _, Some(tp@Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(meDebits, _, _, _, _, _), _) =>
                    sessionCache.put(newSubmission.copy(calculatorData = CalculatorInput(startDate = LocalDate.now(),
                      endDate = LocalDate.now().plusMonths(3).minusDays(1), debits = tpSA.debits))).map[Result] {
                      _ => Redirect(routes.CalculatorController.getPaymentToday())
                    }
                  case TTPSubmission(_, _, _, Some(tp@Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(meDebits, _, _, _, _, _), _) =>
                    if (areEqual(tpSA.debits, meDebits)) {
                      sessionCache.put(newSubmission).flatMap[Result] {
                        _ => eligibilityCheck(tp)
                      }
                    } else {
                      sessionCache.put(newSubmission).flatMap[Result] {
                        _ => Future.successful(Redirect(routes.CalculatorController.getMisalignmentPage()))
                      }
                    }
                  case _ =>
                    Future.successful(Redirect(routes.SelfServiceTimeToPayController.start()))
                }
              })
            }
          })
        }
      }
  }

  def getInstalmentSummary: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.flatMap {
          _.fold(redirectToStart)(ttp => {
            if (areEqual(ttp.taxpayer.get.selfAssessment.get.debits, ttp.calculatorData.debits)) {
              Future.successful(Ok(instalment_plan_summary(ttp.schedule.getOrElse(throw new RuntimeException("No schedule data")),
                createDayOfForm(ttp), signedIn = true)))
            } else {
              Future.successful(Redirect(routes.CalculatorController.getMisalignmentPage()))
            }
          })
        }
      }
  }

  def submitInstalmentSummary: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        Future.successful(Redirect(routes.DirectDebitController.getDirectDebit()))
      }
  }

  def changeSchedulePaymentDay(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            sessionCache.get.map {
              submission => BadRequest(instalment_plan_summary(submission.get.schedule.get, formWithErrors, signedIn = true))
            }
          },
          validFormData => {
            sessionCache.get.flatMap {
              _.fold(redirectToStart)(ttp => changeScheduleDay(ttp, validFormData))
            }
          })
      }
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
    val startDate = ttpSubmission.schedule.get.startDate.get
    val durationMonths = ttpSubmission.durationMonths.get
    val initialDate = startDate.withDayOfMonth(formData.dayOfMonth)

    val (firstPaymentDate: LocalDate, lastPaymentDate: LocalDate) = if (ttpSubmission.calculatorData.initialPayment.equals(BigDecimal(0))) {
      if (formData.dayOfMonth.compareTo(LocalDate.now.getDayOfMonth) < 0)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else
        (initialDate, initialDate.plusMonths(durationMonths).minusDays(1))
    } else {
      if (initialDate.isBefore(startDate) && DAYS.between(startDate, initialDate.plusMonths(1)) <= 14)
        (initialDate.plusMonths(2), initialDate.plusMonths(durationMonths + 2).minusDays(1))
      else if (initialDate.isBefore(startDate))
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else if (DAYS.between(startDate, initialDate) <= 14)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
    }

    Some(ttpSubmission.calculatorData.copy(firstPaymentDate = Some(firstPaymentDate), endDate = lastPaymentDate))
  }

  def submit(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.flatMap {
          _.fold(redirectToStart)(arrangementSetUp)
        }
      }
  }

  def applicationComplete(): Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.flatMap {
          _.fold(redirectToStart)(submission => {
            sessionCache.remove()
            successful(Ok(application_complete(submission.taxpayer.get.selfAssessment.get.debits.sortBy(_.dueDate.toEpochDay()),
              submission.arrangementDirectDebit.get, submission.schedule.get, loggedIn = true)))
          })
        }
      }
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  private def redirectToStart = successful[Result](Redirect(routes.SelfServiceTimeToPayController.start()))

  private def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(submission), SaUtr(utr)).flatMap[Result] {
          _.fold(_ => Future.successful(Redirect(routes.DirectDebitController.getDirectDebitAssistance())),
            success => {
              val result = for {
                ttp <- arrangementConnector.submitArrangements(createArrangement(success, submission))
              } yield ttp

              result.flatMap {
                _.fold(error => Future.failed(new RuntimeException(s"Exception: ${error.code} + ${error.message}")), _ => applicationSuccessful)
              }
            })
        }
      case _ => throw new RuntimeException("Taxpayer or related data not present")
    }
  }

  private def checkExistingBankDetails(submission: TTPSubmission): PaymentPlanRequest = {
    submission.bankDetails.get.ddiRefNumber match {
      case Some(refNo) =>
        paymentPlan(submission, DirectDebitInstruction(ddiRefNumber = Some(refNo)))
      case None =>
        paymentPlan(submission, DirectDebitInstruction(
          sortCode = submission.bankDetails.get.sortCode,
          accountNumber = submission.bankDetails.get.accountNumber,
          accountName = submission.bankDetails.get.accountName))
    }
  }

  private def paymentPlan(submission: TTPSubmission, ddInstruction: DirectDebitInstruction): PaymentPlanRequest = {
    val maybePaymentPlanRequest = for {
      schedule <- submission.schedule
      taxPayer <- submission.taxpayer
      sa <- taxPayer.selfAssessment
      utr <- sa.utr
    } yield {
      val knownFact = List(KnownFact(cesa, utr))

      val initialPayment = if (schedule.initialPayment > BigDecimal.exact(0)) Some(schedule.initialPayment.toString()) else None
      val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.startDate.get.plusDays(7)))

      val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
      val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
      val pp = PaymentPlan(ppType = "Time to Pay",
        paymentReference = s"${utr}K",
        hodService = cesa,
        paymentCurrency = paymentCurrency,
        initialPaymentAmount = initialPayment,
        initialPaymentStartDate = initialStartDate,
        scheduledPaymentAmount = firstInstalment.amount.toString(),
        scheduledPaymentStartDate = firstInstalment.paymentDate,
        scheduledPaymentEndDate = lastInstalment.paymentDate,
        scheduledPaymentFrequency = paymentFrequency,
        balancingPaymentAmount = lastInstalment.amount.toString(),
        balancingPaymentDate = lastInstalment.paymentDate,
        totalLiability = (schedule.instalments.map(_.amount).sum + schedule.initialPayment).toString())

      PaymentPlanRequest("SSTTP", ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT), knownFact, ddInstruction, pp, printFlag = true)
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

  private def createDayOfForm(ttpSubmission: TTPSubmission) = {
    ttpSubmission.calculatorData.firstPaymentDate.fold(ArrangementForm.dayOfMonthForm)(p => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.getDayOfMonth))
    })
  }
}
