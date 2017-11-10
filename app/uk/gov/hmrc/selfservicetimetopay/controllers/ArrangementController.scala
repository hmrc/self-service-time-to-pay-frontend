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
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import javax.inject._

import play.api.Logger
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.forms.ArrangementForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement.{application_complete, instalment_plan_summary}

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.math.BigDecimal

class ArrangementController @Inject()(val messagesApi: play.api.i18n.MessagesApi, ddConnector: DirectDebitConnector,
                                      arrangementConnector: ArrangementConnector,
                                      calculatorConnector: CalculatorConnector,
                                      taxPayerConnector: TaxPayerConnector,
                                      eligibilityConnector: EligibilityConnector) extends TimeToPayController with play.api.i18n.I18nSupport {
  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      sessionCache.get.flatMap {
        case Some(ttp@TTPSubmission(_, _, _, Some(taxpayer), _, _, _, _, _, _)) => eligibilityCheck(taxpayer, ttp)
        case _ => Future.successful(redirectOnError)
      }
  }

  /**
    * This step is performed immediately after the user has logged in. It grabs the Taxpayer data and
    * then performs several checks to determine where the user should go next. This is because there are
    * two points where the user could log in, via the sign in question or via the calculator page.
    * Firstly the debits in the Taxpayer are checked to see if they are less than £32. Next a check is
    * performed to see if the calculator input debits are empty, this is to check to see if the user
    * came from the sign in question. The third check is whether schedule data is present in the
    * TTPSubmission. If not, then the user should be directed to the pay today question.
    * Lastly, a check is performed to see if the user input debits match the Taxpayer
    * debits. If not, display misalignment page otherwise perform an eligibility check.
    */
  //todo refactor this give's makes my head sore
  def determineMisalignment: Action[AnyContent] = authorisedSaUser { implicit authContext =>
    implicit request =>
      sessionCache.get.flatMap {
        case None => Future.successful(redirectOnError)

        case Some(ttp@TTPSubmission(_, _, _, _, _, _, _, _, _, _)) =>
          taxPayerConnector.getTaxPayer(authContext.principal.accounts.sa.get.utr.utr).flatMap[Result] {
            tp =>
              tp.fold(Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs())))(taxPayer => {
                val newSubmission = ttp.copy(taxpayer = Some(taxPayer))
                eligibilityCheck(taxPayer, newSubmission)
              }
              )
          }
      }
  }

  def getInstalmentSummary: Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        authorizedForSsttp {
          case ttp@TTPSubmission(Some(schedule), _, _, _, _, _, cd@CalculatorInput(debits, _, _, _, _, _), _, _, _)
            if areEqual(ttp.taxpayer.get.selfAssessment.get.debits, ttp.calculatorData.debits) =>
            Future.successful(Ok(instalment_plan_summary(debits, schedule, createDayOfForm(ttp), signedIn = true)))
          case _ => Future.successful(Redirect(routes.CalculatorController.getMisalignmentPage()))
        }
  }

  def submitInstalmentSummary: Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        authorizedForSsttp(_ => Future.successful(Redirect(routes.DirectDebitController.getDirectDebit())))
  }

  def changeSchedulePaymentDay(): Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        authorizedForSsttp {
          submission =>
            ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(instalment_plan_summary(
                  submission.taxpayer.get.selfAssessment.get.debits,
                  submission.schedule.get,
                  formWithErrors,
                  signedIn = true)))
              },
              validFormData => changeScheduleDay(submission, validFormData.dayOfMonth).flatMap {
                ttpSubmission =>
                  sessionCache.put(ttpSubmission).map {
                    _ => Redirect(routes.ArrangementController.getInstalmentSummary())
                  }
              })
        }
  }

  /**
    * Take the updated calculator input information and send it to the calculator service
    */
  private def changeScheduleDay(ttpSubmission: TTPSubmission, dayOfMonth: Int)(implicit hc: HeaderCarrier): Future[TTPSubmission] = {
    createCalculatorInput(ttpSubmission, dayOfMonth).fold(throw new RuntimeException("Could not create calculator input"))(cal => {
      calculatorConnector.calculatePaymentSchedule(cal)
        .map[TTPSubmission](seqCalcInput => ttpSubmission.copy(schedule = Option(seqCalcInput.head), calculatorData = cal))
    })
  }

  /**
    * Call the eligibility service using the Taxpayer data and display the appropriate page based on the result
    */
  private def eligibilityCheck(taxpayer: Taxpayer, newSubmission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    lazy val youNeedToFile = Redirect(routes.SelfServiceTimeToPayController.getYouNeedToFile()).successfullF
    val isDebtToLittle = taxpayer.selfAssessment.get.debits.map(debit => debit.amount).sum < BigDecimal.exact("32.00")

    def checkSubmission(ts: TTPSubmission): Future[Result] = ts match {
      case ttp@TTPSubmission(_, _, _, _, _, _, _, _, Some(EligibilityStatus(true, _)), _) =>
        checkSubmissionForCalculatorPage(taxpayer, ttp)
      case ttp@TTPSubmission(_, _, _, _, _, _, _, _, Some(EligibilityStatus(_, reasons)), _) if reasons.contains(ReturnNeedsSubmitting) =>
        youNeedToFile
      case ttp@TTPSubmission(_, _, _, _, _, _, _, _, Some(EligibilityStatus(_, _)), _) =>
        Redirect(routes.SelfServiceTimeToPayController.getTtpCallUs()).successfullF
    }

    if (isDebtToLittle) youNeedToFile
    else {
      for {
        es <- eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), taxpayer))
        updatedSubmission = newSubmission.copy(eligibilityStatus = Option(es))
        _ <- sessionCache.put(updatedSubmission)
        result <- checkSubmission(updatedSubmission)
      } yield result
    }
  }

  def setDefaultCalculatorSchedule(newSubmission: TTPSubmission, debits: Seq[Debit])(implicit hc: HeaderCarrier): Future[CacheMap] = {
    sessionCache.put(newSubmission.copy(calculatorData = CalculatorInput(startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(2).minusDays(1), debits = debits)))
  }

  private def checkSubmissionForCalculatorPage(taxpayer: Taxpayer, newSubmission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {

    val gotoTaxLiabilities = Redirect(routes.CalculatorController.getTaxLiabilities())
    val gotoInstalmentSummary = Redirect(routes.ArrangementController.getInstalmentSummary())
    val gotoMisalignementPage = Redirect(routes.CalculatorController.getMisalignmentPage())

    newSubmission match {
      case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(empty@Seq(), _, _, _, _, _), _, _, _) =>

        setDefaultCalculatorSchedule(newSubmission, tpSA.debits).map(_ => gotoTaxLiabilities)

      case TTPSubmission(None, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(debits, _, _, _, _, _), _, _, _) =>
        gotoTaxLiabilities.successfullF

      case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), _, _, CalculatorInput(debits, _, _, _, _, _), _, _, _) =>
        if (areEqual(debits, tpSA.debits)) {
          setDefaultCalculatorSchedule(newSubmission, tpSA.debits).map {
            _ => gotoInstalmentSummary
          }
        }
        else {
          sessionCache.put(newSubmission).map(_ => gotoMisalignementPage)
        }

      case _ =>
        Logger.error("No match found for newSubmission in determineMisalignment")
        redirectOnError.successfullF
    }
  }

  /**
    * As the user can change which day of the month they wish to make their payments, then a recalculation
    * must be made as this would effect the interest amounts. Rules here must be applied and this function
    * calculates the first payment date and the last payment date by applying these rules.
    *
    * Rules:
    * - First payment must be at least 7 days from today's date
    * - The day of the month cannot be greater than 28, if it is then use the 1st of the following month
    * - There must be at least a 14 day gap between the initial payment date and the first scheduled payment date
    */
  private def createCalculatorInput(ttpSubmission: TTPSubmission, dayOfMonth: Int): Option[CalculatorInput] = {
    val startDate = LocalDate.now()
    val durationMonths = ttpSubmission.durationMonths.get
    val initialDate = startDate.withDayOfMonth(checkDayOfMonth(dayOfMonth))

    val (firstPaymentDate: LocalDate, lastPaymentDate: LocalDate) = if (ttpSubmission.calculatorData.initialPayment.equals(BigDecimal(0))) {
      if (DAYS.between(startDate, initialDate) < 7 && DAYS.between(startDate, initialDate.plusMonths(1)) < 7)
        (initialDate.plusMonths(2), initialDate.plusMonths(durationMonths + 2).minusDays(1))
      else if (DAYS.between(startDate, initialDate) < 7)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else
        (initialDate, initialDate.plusMonths(durationMonths).minusDays(1))
    } else {
      if (initialDate.isBefore(startDate.plusWeeks(1)) && DAYS.between(startDate.plusWeeks(1), initialDate.plusMonths(1)) < 14)
        (initialDate.plusMonths(2), initialDate.plusMonths(durationMonths + 2).minusDays(1))
      else if (initialDate.isBefore(startDate.plusWeeks(1)))
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else if (DAYS.between(startDate.plusWeeks(1), initialDate) < 14)
        (initialDate.plusMonths(1), initialDate.plusMonths(durationMonths + 1).minusDays(1))
      else
        (initialDate, initialDate.plusMonths(durationMonths).minusDays(1))
    }

    Some(ttpSubmission.calculatorData.copy(startDate = startDate,
      firstPaymentDate = Some(firstPaymentDate),
      endDate = lastPaymentDate))
  }

  def submit(): Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        authorizedForSsttp {
          ttp => arrangementSetUp(ttp)
        }
  }

  def applicationComplete(): Action[AnyContent] = authorisedSaUser {
    implicit authContext =>
      implicit request =>
        authorizedForSsttp {
          submission =>
            sessionCache.remove().map(_ => Ok(application_complete(
              debits = submission.taxpayer.get.selfAssessment.get.debits.sortBy(_.dueDate.toEpochDay()),
              transactionId = submission.taxpayer.get.selfAssessment.get.utr.get + LocalDateTime.now().toString,
              directDebit = submission.arrangementDirectDebit.get,
              schedule = submission.schedule.get,
              isSignedIn))
            )
        }
  }

  private def checkDayOfMonth(dayOfMonth: Int): Int = dayOfMonth match {
    case day if day > 28 => 1
    case _ => dayOfMonth
  }

  private def areEqual(tpDebits: Seq[Debit], meDebits: Seq[Debit]): Boolean = tpDebits.map(_.amount).sum == meDebits.map(_.amount).sum

  private def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

  /**
    * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
    * As the arrangement details are persisted in a database, the user is directed to the application
    * complete page if we get an error response from DES passed back by the arrangement service.
    */
  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(submission), SaUtr(utr)).flatMap[Result] {
          _.fold(_ => Redirect(routes.DirectDebitController.getDirectDebitError()).successfullF,
            success => {
              val result = for {
                ttp <- arrangementConnector.submitArrangements(createArrangement(success, submission))
              } yield ttp

              result.flatMap {
                _.fold(error => {
                  Logger.error(s"Exception: ${error.code} + ${error.message}")
                  Redirect(routes.ArrangementController.applicationComplete()).successfullF
                }, _ => applicationSuccessful)
              }
            })
        }
      case _ =>
        Logger.error("Taxpayer or related data not present")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable()))
    }
  }

  /**
    * Checks if the TTPSubmission data contains an existing direct debit reference number and either
    * passes this information to a payment plan constructor function or builds a new Direct Debit Instruction
    */
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

  /**
    * Builds and returns a payment plan
    */
  private def paymentPlan(submission: TTPSubmission, ddInstruction: DirectDebitInstruction): PaymentPlanRequest = {
    val paymentPlanRequest = for {
      schedule <- submission.schedule
      taxPayer <- submission.taxpayer
      sa <- taxPayer.selfAssessment
      utr <- sa.utr
    } yield {
      val knownFact = List(KnownFact(cesa, utr))

      val initialPayment = if (schedule.initialPayment > BigDecimal.exact(0)) Some(schedule.initialPayment.toString()) else None
      val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.startDate.get.plusWeeks(1)))

      val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
      val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
      val pp = PaymentPlan(ppType = "Time to Pay",
        paymentReference = s"${
          utr
        }K",
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

    paymentPlanRequest.getOrElse(throw new RuntimeException(s"PaymentPlanRequest creation failed - TTPSubmission: $submission"))
  }

  /**
    * Builds and returns a TTPArrangement
    */
  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiReferenceNo.getOrElse(throw new RuntimeException("ddReference not available"))
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
