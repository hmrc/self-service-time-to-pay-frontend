/*
 * Copyright 2020 HM Revenue & Customs
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
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import javax.inject._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result, Results}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.auth.{Token, TokenData}
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.forms.ArrangementForm
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.service.CalculatorService.createCalculatorInput
import uk.gov.hmrc.selfservicetimetopay.service.{AuditService, CalculatorService}
import uk.gov.hmrc.selfservicetimetopay.util.TTPSessionId
import views.html.selfservicetimetopay.arrangement.{application_complete, change_day, declaration, instalment_plan_summary}

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.math.BigDecimal
class ArrangementController @Inject() (val messagesApi: play.api.i18n.MessagesApi, ddConnector: DirectDebitConnector,
                                       arrangementConnector: ArrangementConnector,
                                       calculatorService:    CalculatorService,
                                       calculatorConnector:  CalculatorConnector,
                                       taxPayerConnector:    TaxPayerConnector,
                                       eligibilityConnector: EligibilityConnector,
                                       auditService:         AuditService) extends TimeToPayController with play.api.i18n.I18nSupport {
  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.start: $request")
    sessionCache.getTtpSessionCarrier.flatMap {
      case Some(ttp @ TTPSubmission(_, _, _, Some(taxpayer), _, _, _, _, _, _)) => eligibilityCheck(taxpayer, ttp, authContext.principal.accounts.sa.get.utr.utr)
      case maybeTtpSubmission =>
        JourneyLogger.info(s"ArrangementController.start: redirect On Error", maybeTtpSubmission)
        Future.successful(redirectOnError)
    }
  }

  def recoverTTPSession(token: String): Action[AnyContent] = Action.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.recoverTTPSession: $request")
    for {
      tokenData: TokenData <- sessionCache4TokensConnector
        .getAndRemove(Token(token))
        .map(_.getOrElse(throw new RuntimeException(s"There was no data associated with that token [token:$token]. Did someone try to reuse expired token huh?")))
      ttpSessionKV = TTPSessionId.ttpSessionId -> tokenData.associatedTTPSession.v
      isTokenValid = isTokenStillValid(tokenData)
      redirect = if (isTokenValid) {
        Results.Redirect(routes.ArrangementController.determineEligibility())
      } else {
        Logger.logger.debug(s"Token expired: $tokenData")
        JourneyLogger.info(s"ArrangementController.recoverTTPSession: redirectOnError")
        Redirect(routes.SelfServiceTimeToPayController.start())
      }
    } yield redirect.withSession(request.session + ttpSessionKV)
  }

  private def isTokenStillValid(tokenData: TokenData): Boolean = tokenData.expirationDate.isAfter(LocalDateTime.now())

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
  def determineEligibility: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.determineEligibility: $request")

    taxPayerConnector.getTaxPayer(authContext.principal.accounts.sa.get.utr.utr).flatMap[Result] {
      tp =>
        tp.fold {
          JourneyLogger.info(s"ArrangementController.determineEligibility: ERROR there is no taxpayer")
          Future.successful(Redirect(routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion()))
        }(taxPayer => {
          val newSubmission = TTPSubmission(taxpayer = Some(taxPayer))
          sessionCache.putTtpSessionCarrier(newSubmission).flatMap { _ =>

            eligibilityCheck(taxPayer, newSubmission, authContext.principal.accounts.sa.get.utr.utr)
          }
        }
        )
    }
  }

  def getInstalmentSummary: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.getInstalmentSummary: $request")
    authorizedForSsttp {
      case ttp @ TTPSubmission(Some(schedule), _, _, _, cd @ CalculatorInput(debits, intialPayment, _, _, _, _), _, _, _, _, _) =>
        Future.successful(Ok(instalment_plan_summary(debits, intialPayment, schedule.schedule)))
      case _ => Future.successful(Redirect(routes.ArrangementController.determineEligibility()))
    }
  }

  def submitInstalmentSummary: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.submitInstalmentSummary: $request")
    authorizedForSsttp(_ => Future.successful(Redirect(routes.ArrangementController.getDeclaration())))
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.getChangeSchedulePaymentDay: $request")
    authorizedForSsttp(ttp => Future.successful(Ok(change_day(createDayOfForm(ttp)))))
  }

  def getDeclaration: Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.getDeclaration: $request")
    authorizedForSsttp(ttp => Future.successful(Ok(declaration())))
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.submitChangeSchedulePaymentDay: $request")
    authorizedForSsttp {
      submission =>
        ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(change_day(formWithErrors)))
          },
          validFormData => {
            submission match {
              case ttp @ TTPSubmission(Some(schedule), _, _, _, cd @ CalculatorInput(debits, _, _, _, _, _), _, _, _, _, _) =>
                JourneyLogger.info(s"changing schedule day to [${validFormData.dayOfMonth}]")
                changeScheduleDay(submission, schedule, debits, validFormData.dayOfMonth).flatMap {
                  ttpSubmission =>
                    sessionCache.putTtpSessionCarrier(ttpSubmission).map {
                      _ => Redirect(routes.ArrangementController.getInstalmentSummary())
                    }
                }
              case _ =>
                JourneyLogger.info(s"Problematic Submission, redirecting to ${routes.ArrangementController.determineEligibility()}")
                Future.successful(Redirect(routes.ArrangementController.determineEligibility()))
            }
          })
    }
  }

  /**
   * Take the updated calculator input information and send it to the calculator service
   */
  private def changeScheduleDay(ttpSubmission: TTPSubmission, schedule: CalculatorPaymentScheduleExt, debits: Seq[Debit], dayOfMonth: Int)(implicit hc: HeaderCarrier): Future[TTPSubmission] = {
    val months = schedule.schedule.instalments.length
    val input = createCalculatorInput(
      months,
      dayOfMonth,
      schedule.schedule.initialPayment,
      debits)
    calculatorConnector.calculatePaymentSchedule(input)
      .map(_.map(CalculatorPaymentScheduleExt(months, _)))
      .map[TTPSubmission](seqCalcInput => ttpSubmission.copy(
        schedule       = Option(seqCalcInput.head),
        calculatorData = input
      ))
  }

  /**
   * Call the eligibility service using the Taxpayer data and display the appropriate page based on the result
   */
  private def eligibilityCheck(taxpayer: Taxpayer, newSubmission: TTPSubmission, utr: String)(implicit hc: HeaderCarrier): Future[Result] = {
    JourneyLogger.info(s"ArrangementController.eligibilityCheck")

    lazy val youNeedToFile = Redirect(routes.SelfServiceTimeToPayController.getYouNeedToFile()).successfulF
    lazy val notOnIa = Redirect(routes.SelfServiceTimeToPayController.getIaCallUse()).successfulF
    lazy val overTenThousandOwed = Redirect(routes.SelfServiceTimeToPayController.getDebtTooLarge()).successfulF

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
          Redirect(routes.SelfServiceTimeToPayController.getTtpCallUsSignInQuestion()).successfulF
      }

    for {
      es: EligibilityStatus <- eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), taxpayer), utr)
      updatedSubmission = newSubmission.copy(eligibilityStatus = Option(es))
      _ <- sessionCache.putTtpSessionCarrier(updatedSubmission)
      _ = JourneyLogger.info(s"ArrangementController.eligibilityCheck [eligible=${es.eligible}]", updatedSubmission)
      result <- checkSubmission(updatedSubmission)
    } yield result
  }

  def setDefaultCalculatorSchedule(newSubmission: TTPSubmission, debits: Seq[Debit])(implicit hc: HeaderCarrier): Future[CacheMap] = {
    val updatedSubmission = newSubmission.copy(calculatorData = CalculatorInput(startDate = LocalDate.now(),
                                                                                endDate   = LocalDate.now().plusMonths(2).minusDays(1), debits = debits))
    JourneyLogger.info("ArrangementController.setDefaultCalculatorSchedule, updatedSubmission is", updatedSubmission)
    sessionCache.putTtpSessionCarrier(updatedSubmission)
  }

  private def checkSubmissionForCalculatorPage(taxpayer: Taxpayer, newSubmission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {

    val gotoTaxLiabilities = Redirect(routes.CalculatorController.getTaxLiabilities())

    newSubmission match {
      case TTPSubmission(_, _, _, Some(Taxpayer(_, _, Some(tpSA))), CalculatorInput(_, _, _, _, _, _), _, _, _, _, _) =>

        setDefaultCalculatorSchedule(newSubmission, tpSA.debits).map(_ => gotoTaxLiabilities)

      case maybeSubmission =>
        Logger.error("No match found for newSubmission in determineEligibility")
        JourneyLogger.info(s"ArrangementController.checkSubmissionForCalculatorPage: match error - redirect On Error", maybeSubmission)
        redirectOnError.successfulF
    }
  }

  def submit(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.submit: $request")
    authorizedForSsttp {
      ttp => arrangementSetUp(ttp)
    }
  }

  def applicationComplete(): Action[AnyContent] = authorisedSaUser { implicit authContext => implicit request =>
    JourneyLogger.info(s"ArrangementController.applicationComplete: $request")
    authorizedForSsttp {
      submission =>
        sessionCache.remove().map(_ => Ok(application_complete(
          debits        = submission.taxpayer.get.selfAssessment.get.debits.sortBy(_.dueDate.toEpochDay()),
          transactionId = submission.taxpayer.get.selfAssessment.get.utr.get + LocalDateTime.now().toString,
          directDebit   = submission.arrangementDirectDebit.get,
          schedule      = submission.schedule.get.schedule,
          ddref         = submission.ddRef)
        ))
    }
  }

  private def applicationSuccessful = successful(Redirect(routes.ArrangementController.applicationComplete()))

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(submission: TTPSubmission)(implicit hc: HeaderCarrier): Future[Result] = {
    JourneyLogger.info("ArrangementController.arrangementSetUp: (create a DD and make an Arrangement)")
    submission.taxpayer match {
      case Some(Taxpayer(_, _, Some(SelfAssessment(Some(utr), _, _, _)))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(submission), SaUtr(utr)).flatMap[Result] { dDSubmissionResult: ddConnector.DDSubmissionResult =>
          dDSubmissionResult.fold(_ => {
            JourneyLogger.info("ArrangementController.arrangementSetUp: dd setup failed, redirecting to error page")
            Redirect(routes.DirectDebitController.getDirectDebitError()).successfulF
          },
            success => {
              JourneyLogger.info("ArrangementController.arrangementSetUp: dd setup succeeded, now creating arrangement")
              val arrangement = createArrangement(success, submission)
              val result = for {
                submissionResult <- arrangementConnector.submitArrangements(arrangement)
                _ = auditService.sendSubmissionEvent(submission)
                _ = sessionCache.putTtpSessionCarrier(submission.copy(ddRef = Some(arrangement.directDebitReference)))
              } yield submissionResult

              result.flatMap {
                submissionResult =>
                  submissionResult.fold(error => {
                    Logger.error(s"Exception: ${error.code} + ${error.message}")
                    JourneyLogger.info(s"ArrangementController.arrangementSetUp: ZONK ERROR! Arrangement submission failed, $error but redirecting to $applicationSuccessful")
                    applicationSuccessful
                  }, _ => {
                    JourneyLogger.info(s"ArrangementController.arrangementSetUp: Arrangement submission Succeeded!")
                    applicationSuccessful
                  }
                  )
              }
            })
        }
      case _ =>
        Logger.error("Taxpayer or related data not present")
        JourneyLogger.info("ArrangementController.arrangementSetUp: ERROR, Taxpayer or related data not present")
        Future.successful(Redirect(routes.SelfServiceTimeToPayController.getNotSaEnrolled()))
    }
  }

  /**
   * Checks if the TTPSubmission data contains an existing direct debit reference number and either
   * passes this information to a payment plan constructor function or builds a new Direct Debit Instruction
   */
  private def checkExistingBankDetails(submission: TTPSubmission)(implicit hc: HeaderCarrier): PaymentPlanRequest = {
    JourneyLogger.info("ArrangementController.checkExistingBankDetails")
    submission.bankDetails.get.ddiRefNumber match {
      case Some(refNo) =>
        JourneyLogger.info("ArrangementController.checkExistingBankDetails - found bankDetails")
        paymentPlan(submission, DirectDebitInstruction(ddiRefNumber = Some(refNo)))
      case None =>
        JourneyLogger.info("ArrangementController.checkExistingBankDetails - NOT found bankDetails")
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

      val initialPayment = if (schedule.schedule.initialPayment > BigDecimal.exact(0)) Some(schedule.schedule.initialPayment.toString()) else None
      val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.schedule.startDate.get.plusWeeks(1)))

      val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.schedule.instalments.last
      val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.schedule.instalments.head
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
                           totalLiability            = (schedule.schedule.instalments.map(_.amount).sum + schedule.schedule.initialPayment).toString())

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

    TTPArrangement(ppReference, ddReference, taxpayer, schedule.schedule)
  }

  private def createDayOfForm(ttpSubmission: TTPSubmission) = {
    ttpSubmission.schedule.fold(ArrangementForm.dayOfMonthForm)((p: CalculatorPaymentScheduleExt) => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.schedule.getMonthlyInstalmentDate))
    })
  }
}
