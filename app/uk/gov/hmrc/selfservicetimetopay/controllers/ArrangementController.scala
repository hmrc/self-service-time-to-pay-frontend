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
import java.time.temporal.ChronoUnit.DAYS

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

class ArrangementController(ddConnector: DirectDebitConnector,
                            arrangementConnector: ArrangementConnector,
                            calculatorConnector: CalculatorConnector,
                            taxPayerConnector: TaxPayerConnector,
                            eligibilityConnector: EligibilityConnector) extends TimeToPayController {

  val cesa: String = "CESA"
  val paymentFrequency = "Monthly"
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

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]) = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  def getInstalmentSummary: Action[AnyContent] = AuthorisedSaUser {
    implicit authContext => implicit request =>
      authorizedForSsttp {
        sessionCache.get.flatMap {
          _.fold(redirectToStart)(ttp => {
            Future.successful(Ok(instalment_plan_summary(ttp.schedule.getOrElse(throw new RuntimeException("No schedule data")),
              createDayOfForm(ttp), signedIn = true)))
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

  private def createDayOfForm(ttpSubmission: TTPSubmission) = {
    ttpSubmission.calculatorData.firstPaymentDate.fold(ArrangementForm.dayOfMonthForm)(p => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.getDayOfMonth))
    })
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

  // TODO Refactor post MVP
  def createCalculatorInput(ttpSubmission: TTPSubmission, formData: ArrangementDayOfMonth): Option[CalculatorInput] = {
    val schedule = ttpSubmission.schedule.get
    val startDate = schedule.startDate.get

    var firstPmnttDate = startDate.withDayOfMonth(formData.dayOfMonth)
    // set end date base on number of months the user selected
    var endDate = LocalDate.of(schedule.endDate.get.getYear, schedule.endDate.get.getMonth, formData.dayOfMonth).minusDays(1)

    // if there is no initial payment then first payment becomes initial payment but its startdate can be changed by user
    if(ttpSubmission.calculatorData.initialPayment.equals(BigDecimal(0))) {
      // if the day entered by the user is older than today, then set the firstPaymentDate to next month
      if (formData.dayOfMonth.compareTo(LocalDate.now.getDayOfMonth) < 0) {
        firstPmnttDate = startDate.plusMonths(1).withDayOfMonth(formData.dayOfMonth)
        endDate = firstPmnttDate.plusMonths(ttpSubmission.durationMonths.get).withDayOfMonth(formData.dayOfMonth).minusDays(1)
      } else {
        firstPmnttDate = startDate.withDayOfMonth(formData.dayOfMonth)
        endDate = firstPmnttDate.plusMonths(ttpSubmission.durationMonths.get).withDayOfMonth(formData.dayOfMonth).minusDays(1)
      }
    } else {
      // if there is an initial payment and if first payment is less than 14 days from now+5 days move by a month
      if(firstPmnttDate.isBefore(startDate))  {
        firstPmnttDate = firstPmnttDate.plusMonths(1)
        if(DAYS.between(startDate, firstPmnttDate) <= 14) {
          firstPmnttDate = firstPmnttDate.plusMonths(1)
        }
        endDate = firstPmnttDate.plusMonths(ttpSubmission.durationMonths.get).minusDays(1)
      } else {
        if(DAYS.between(startDate, firstPmnttDate) <= 14) {
          firstPmnttDate = firstPmnttDate.plusMonths(1)
        }
      }
    }

    val input = ttpSubmission.calculatorData.copy(firstPaymentDate = Some(firstPmnttDate), endDate = endDate)

    Some(input)
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

  private def redirectToStart = successful[Result](Redirect(routes.SelfServiceTimeToPayController.start()))

  private def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        ddConnector.createPaymentPlan(paymentPlan(submission), SaUtr(utr)).flatMap[Result] {
          _.fold(error => Future.successful(Redirect(routes.DirectDebitController.getDirectDebitAssistance())),
            success => {
              val result = for {
                ttp <- arrangementConnector.submitArrangements(createArrangement(success, submission))
              } yield ttp

              result.flatMap {
                _.fold(error => Future.failed(new RuntimeException(s"Exception: ${error.code} + ${error.message}")), success => applicationSuccessful)
              }
            })
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
      val pp = PaymentPlan(ppType = "Time to Pay",
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