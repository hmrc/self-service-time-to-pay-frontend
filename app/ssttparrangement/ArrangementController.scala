/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import audit.AuditService
import config.AppConfig
import controllers.action.Actions
import controllers.{ErrorHandler, FrontendController}
import javax.inject._
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import ssttpcalculator.{CalculatorConnector, CalculatorService}
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.EligibilityConnector
import sstttaxpayer.TaxPayerConnector
import sttpsubmission.SubmissionService
import token.TokenData
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful
import scala.math.BigDecimal

class ArrangementController @Inject() (
    mcc:                  MessagesControllerComponents,
    ddConnector:          DirectDebitConnector,
    arrangementConnector: ArrangementConnector,
    calculatorService:    CalculatorService,
    calculatorConnector:  CalculatorConnector,
    taxPayerConnector:    TaxPayerConnector,
    eligibilityConnector: EligibilityConnector,
    auditService:         AuditService,
    submissionService:    SubmissionService,
    as:                   Actions,
    views:                Views)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendController(mcc) {

  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.getTtpSubmission.flatMap {
      case Some(ttp @ TTPSubmission(_, _, _, Some(taxpayer), _, _, _, _, _, _)) =>
        eligibilityCheck(taxpayer, ttp, request.utr)
      case _ => Future.successful(ErrorHandler.redirectOnError)
    }
  }

  private def isTokenStillValid(tokenData: TokenData): Boolean =
    tokenData.expirationDate.isAfter(LocalDateTime.now())

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
  def determineEligibility: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>

    for {
      tp <- taxPayerConnector.getTaxPayer(request.utr)
      newSubmission = TTPSubmission(taxpayer = Some(tp))
      _ <- submissionService.putTtpSubmission(newSubmission)
      check: Result <- eligibilityCheck(tp, newSubmission, request.utr)
    } yield check

  }

  def getInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      case ttp @ TTPSubmission(Some(schedule), _, _, _, cd @ CalculatorInput(debits, intialPayment, _, _, _, _), _, _, _, _, _) =>
        Future.successful(Ok(views.instalment_plan_summary(debits, intialPayment, schedule)))
      case _ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.determineEligibility()))
    }
  }

  def submitInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp(_ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.getDeclaration())))
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp(ttp => Future.successful(Ok(views.change_day(createDayOfForm(ttp)))))
  }

  def getDeclaration: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp(ttp => Future.successful(Ok(views.declaration())))
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      submission =>
        ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.change_day(formWithErrors)))
          },
          validFormData => {
            submission match {
              case ttp @ TTPSubmission(Some(schedule), _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _) =>
                changeScheduleDay(submission, schedule, debits, validFormData.dayOfMonth).flatMap {
                  ttpSubmission =>
                    submissionService.putTtpSubmission(ttpSubmission).map {
                      _ => Redirect(ssttparrangement.routes.ArrangementController.getInstalmentSummary())
                    }
                }
              case _ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.determineEligibility()))
            }
          })
    }
  }

  /**
   * Take the updated calculator input information and send it to the calculator service
   */
  private def changeScheduleDay(ttpSubmission: TTPSubmission, schedule: CalculatorPaymentSchedule, debits: Seq[Debit], dayOfMonth: Int)(implicit request: Request[_]): Future[TTPSubmission] = {
    val input = CalculatorService.createCalculatorInput(
      schedule.instalments.length,
      dayOfMonth,
      schedule.initialPayment,
      debits)
    calculatorConnector.calculatePaymentSchedule(input).map[TTPSubmission](seqCalcInput => ttpSubmission.copy(schedule       = Option(seqCalcInput.head), calculatorData = input))
  }

  /**
   * Call the eligibility service using the Taxpayer data and display the appropriate page based on the result
   */
  private def eligibilityCheck(taxpayer: Taxpayer, newSubmission: TTPSubmission, utr: String)(implicit request: Request[_]): Future[Result] = {
    lazy val youNeedToFile = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile())
    lazy val notOnIa = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse())
    lazy val overTenThousandOwed = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge())

      def checkSubmission(ts: TTPSubmission): Future[Result] = ts match {
        case ttp @ TTPSubmission(_, _, _, _, _, _, Some(EligibilityStatus(true, _)), _, _, _) =>
          checkSubmissionForCalculatorPage(taxpayer, ttp)
        //todo merge the bottom 3
        case ttp @ TTPSubmission(_, _, _, _, _, _, Some(EligibilityStatus(false, reasons)), _, _, _) if reasons.contains(IsNotOnIa) =>
          notOnIa
        case ttp @ TTPSubmission(_, _, _, _, _, _, Some(EligibilityStatus(false, reasons)), _, _, _) if reasons.contains(TotalDebtIsTooHigh) =>
          overTenThousandOwed
        case ttp @ TTPSubmission(_, _, _, _, _, _, Some(EligibilityStatus(false, reasons)), _, _, _) if reasons.contains(ReturnNeedsSubmitting) || reasons.contains(DebtIsInsignificant) =>
          youNeedToFile
        case ttp @ TTPSubmission(_, _, _, _, _, _, Some(EligibilityStatus(_, _)), _, _, _) =>
          Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion())
      }

    for {
      es <- eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), taxpayer), utr)
      updatedSubmission = newSubmission.copy(eligibilityStatus = Option(es))
      _ <- submissionService.putTtpSubmission(updatedSubmission)
      result <- checkSubmission(updatedSubmission)
    } yield result
  }

  def setDefaultCalculatorSchedule(newSubmission: TTPSubmission, debits: Seq[Debit])(implicit request: Request[_]): Future[CacheMap] = {
    submissionService.putTtpSubmission(newSubmission.copy(calculatorData = CalculatorInput(startDate = LocalDate.now(),
                                                                                           endDate   = LocalDate.now().plusMonths(2).minusDays(1), debits = debits)))
  }

  private def checkSubmissionForCalculatorPage(taxpayer: Taxpayer, newSubmission: TTPSubmission)(implicit request: Request[_]): Future[Result] = {

    val gotoTaxLiabilities = Redirect(ssttpcalculator.routes.CalculatorController.getTaxLiabilities())

    newSubmission match {
      case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), CalculatorInput(_, _, _, _, _, _), _, _, _, _, _) =>

        setDefaultCalculatorSchedule(newSubmission, tpSA.debits).map(_ => gotoTaxLiabilities)

      case _ =>
        Logger.error("No match found for newSubmission in determineEligibility")
        redirectOnError
    }
  }

  def submit(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    submissionService.authorizedForSsttp {
      ttp => arrangementSetUp(ttp)
    }
  }

  def applicationComplete(): Action[AnyContent] = as.authorisedSaUser.async{ implicit request =>
    submissionService.authorizedForSsttp {
      submission =>
        submissionService.remove().map(_ => Ok(views.application_complete(
          debits        = submission.taxpayer.get.selfAssessment.get.debits.sortBy(_.dueDate.toEpochDay()),
          transactionId = submission.taxpayer.get.selfAssessment.get.utr.get + LocalDateTime.now().toString,
          directDebit   = submission.arrangementDirectDebit.get,
          schedule      = submission.schedule.get,
          ddref         = submission.ddRef)
        ))
    }
  }

  private def applicationSuccessful = successful(Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(submission: TTPSubmission)(implicit request: Request[_]): Future[Result] = {
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(submission), SaUtr(utr)).flatMap[Result] {
          _.fold(_ => Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitError()),
            success => {
              val arrangement = createArrangement(success, submission)
              val result = for {
                submissionResult <- arrangementConnector.submitArrangements(arrangement)
                _ = auditService.sendSubmissionEvent(submission)
                _ = submissionService.putTtpSubmission(submission.copy(ddRef = Some(arrangement.directDebitReference)))
              } yield submissionResult

              result.flatMap {
                _.fold(error => {
                  Logger.error(s"Exception: ${error.code} + ${error.message}")
                  applicationSuccessful
                }, _ => applicationSuccessful)
              }
            })
        }
      case _ =>
        Logger.error("Taxpayer or related data not present")
        Future.successful(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled()))
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
          sortCode      = submission.bankDetails.get.sortCode,
          accountNumber = submission.bankDetails.get.accountNumber,
          accountName   = submission.bankDetails.get.accountName))
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
      val pp = PaymentPlan(ppType                    = "Time to Pay",
                           paymentReference          = s"${
          utr
        }K",
                           hodService                = cesa,
                           paymentCurrency           = paymentCurrency,
                           initialPaymentAmount      = initialPayment,
                           initialPaymentStartDate   = initialStartDate,
                           scheduledPaymentAmount    = firstInstalment.amount.toString(),
                           scheduledPaymentStartDate = firstInstalment.paymentDate,
                           scheduledPaymentEndDate   = lastInstalment.paymentDate,
                           scheduledPaymentFrequency = paymentFrequency,
                           balancingPaymentAmount    = lastInstalment.amount.toString(),
                           balancingPaymentDate      = lastInstalment.paymentDate,
                           totalLiability            = (schedule.instalments.map(_.amount).sum + schedule.initialPayment).toString())

      PaymentPlanRequest("SSTTP", ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT), knownFact, ddInstruction, pp, printFlag = true)
    }

    paymentPlanRequest.getOrElse(throw new RuntimeException(s"PaymentPlanRequest creation failed - TTPSubmission: $submission"))
  }

  /**
   * Builds and returns a TTPArrangement
   */
  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission:    TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiReferenceNo.getOrElse(throw new RuntimeException("ddReference not available"))
    val taxpayer = submission.taxpayer.getOrElse(throw new RuntimeException("Taxpayer data not present"))
    val schedule = submission.schedule.getOrElse(throw new RuntimeException("Schedule data not present"))

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }

  private def createDayOfForm(ttpSubmission: TTPSubmission) = {
    ttpSubmission.schedule.fold(ArrangementForm.dayOfMonthForm)(p => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.getMonthlyInstalmentDate))
    })
  }
}
