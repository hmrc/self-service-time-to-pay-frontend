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

import _root_.model._
import audit.AuditService
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.{Actions, AuthorisedSaUserRequest}
import journey.Statuses.ApplicationComplete
import journey.{Journey, JourneyService}
import play.api.data.Form
import play.api.mvc._
import playsession.PlaySessionSupport._
import req.RequestSupport
import ssttparrangement.ArrangementForm.dayOfMonthForm
import ssttparrangement.ArrangementSubmissionStatus.{PermanentFailure, QueuedForRetry, Success}
import ssttpcalculator.{CalculatorType, PaymentPlansService}
import ssttpcalculator.legacy.CalculatorService
import ssttpcalculator.legacy.util.CalculatorSwitchSelectedScheduleHelper
import ssttpcalculator.model.{Instalment, PaymentSchedule}
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.{EligibilityService, IaService}
import times.ClockProvider
import timetopaytaxpayer.cor.{TaxpayerConnector, model}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.selfservicetimetopay.models._
import util.Logging
import views.Views

import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.{LocalDate, ZonedDateTime}
import javax.inject._
import scala.concurrent.Future.successful
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode.HALF_UP
import scala.math.BigDecimal.exact

class ArrangementController @Inject() (
    mcc:                     MessagesControllerComponents,
    ddConnector:             DirectDebitConnector,
    arrangementConnector:    ArrangementConnector,
    val paymentPlansService: PaymentPlansService, // calculator type feature flag: used by PaymentOptimised calculator feature
    val calculatorService:   CalculatorService, // calculator type feature flag: used by Legacy calculator feature
    eligibilityService:      EligibilityService,
    taxPayerConnector:       TaxpayerConnector,
    auditService:            AuditService,
    journeyService:          JourneyService,
    as:                      Actions,
    requestSupport:          RequestSupport,
    views:                   Views,
    clockProvider:           ClockProvider,
    iaService:               IaService,
    mongoLockRepository:     MongoLockRepository,
    directDebitConnector:    DirectDebitConnector
)(
    implicit
    val appConfig: AppConfig,
    ec:            ExecutionContext
)
  extends FrontendBaseController(mcc)
  with CalculatorSwitchSelectedScheduleHelper
  with Logging {

  import clockProvider._
  import requestSupport._

  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap {
      case journey if journey.inProgress && journey.maybeSelectedPlanAmount.isDefined =>
        eligibilityCheck(journey)
      case j =>
        val msg = "Illegal state, journey must be in progress with defined payment amount"
        val ex = new RuntimeException(msg)
        journeyLogger.warn(msg)(request, j)
        throw ex
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
  def determineEligibility: Action[AnyContent] = as.authorisedSaUser.async { implicit request: AuthorisedSaUserRequest[AnyContent] =>
    for {
      tp: model.Taxpayer <- taxPayerConnector.getTaxPayer(request.utr)
      maybeJourney <- journeyService.getMaybeJourney()
      journey = maybeJourney.filterNot(_.isFinished).getOrElse(Journey.newJourney).copy(maybeTaxpayer = Some(tp))
      _ <- journeyService.saveJourney(journey)
      result: Result <- eligibilityCheck(journey)
    } yield result.placeInSession(journey._id)

  }

  def getCheckPaymentPlan: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'Check payment plan'")

      journey.requireScheduleIsDefined()

      val schedule = selectedSchedule(journey)

      val leftOverIncome: BigDecimal = journey.remainingIncomeAfterSpending
      val monthlyPaymentAmountChosen = journey.maybeSelectedPlanAmount.getOrElse(
        throw new IllegalArgumentException("a selection should have been made of monthly payment")
      )

      if (isAccruedInterestValid(schedule, monthlyPaymentAmountChosen)) {
        Future.successful(Ok(views.check_payment_plan(schedule, leftOverIncome, monthlyPaymentAmountChosen)))
      } else Future.successful(Redirect(ssttpaffordability.routes.AffordabilityController.getSetUpPlanWithAdviser()))

    }
  }

  /**
   * The following method checks if the chosen plan has total accrued interest that is less than the monthly payment.
   * If the total accrued interest is equal or higher than the monthly payment, it will cause NDDS to reject the plan,
   * because of the validation that is performed on the balancingDate.
   */
  private def isAccruedInterestValid(paymentSchedule: PaymentSchedule, monthlyPaymentAmountChosen: BigDecimal): Boolean = {
    paymentSchedule.totalInterestCharged < monthlyPaymentAmountChosen
  }

  def submitCheckPaymentPlan: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Submit 'Check payment plan'")

      Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit()))
    }
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Get 'Change schedule payment day'")

      val form = dayOfMonthForm
      val formWithData: Form[ArrangementForm] = journey.maybePaymentDayOfMonth match {
        case _: Option[PaymentDayOfMonth] =>
          form.fill(ArrangementForm(journey.selectedDay))
        case _ => form.fill(ArrangementForm(None))
      }
      Future.successful(Ok(views.change_day(formWithData)))
    }
  }

  def getTermsAndConditions: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp{ implicit journey =>
      journeyLogger.info("Get 'Terms and conditions'")

      Future.successful(Ok(views.terms_and_conditions()))
    }
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp { implicit journey =>
      journeyLogger.info("Submit 'Change schedule payment day'")

      dayOfMonthForm.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.change_day(formWithErrors)))
        },
        (validFormData: ArrangementForm) => {
          journeyLogger.info(s"Changing schedule payment day to [${validFormData.dayOfMonth}]")
          val updatedJourney = journey.copy(maybePaymentDayOfMonth = Some(PaymentDayOfMonth(validFormData.dayOfMonth)))
          journeyService.saveJourney(updatedJourney).map {
            _ => Redirect(ssttpaffordability.routes.AffordabilityController.getCheckYouCanAfford())
          }
        }
      )
    }
  }

  /**
   * Call the eligibility service using the Taxpayer data
   * and display the appropriate page based on the result
   */
  private def eligibilityCheck(journey: Journey)(implicit request: Request[_]): Future[Result] = {
    journeyLogger.info("Eligibility check")(request, journey)

    val taxpayer = journey.taxpayer
    val utr = taxpayer.selfAssessment.utr

    for {
      onIa <- iaService.checkIaUtr(utr.value)
      directDebits <- directDebitConnector.getBanks(utr)
      eligibilityStatus = eligibilityService.checkEligibility(clockProvider.nowDate, taxpayer, directDebits, onIa)
      newJourney: Journey = journey.copy(maybeEligibilityStatus = Option(eligibilityStatus))
      _ <- journeyService.saveJourney(newJourney)
    } yield {
      journeyLogger.info(s"Eligibility check outcome [eligible: ${eligibilityStatus.eligible}]")(request, newJourney)

      if (eligibilityStatus.eligible) Redirect(ssttpcalculator.routes.CalculatorController.getTaxLiabilities())
      else Redirect(ineligibleStatusRedirect(eligibilityStatus, newJourney))
    }
  }

  //TODO improve this under OPS-4941
  private def ineligibleStatusRedirect(eligibilityStatus: EligibilityStatus, journey: Journey)(implicit rh: RequestHeader) = {
    if (eligibilityStatus.reasons.contains(DebtTooOld) ||
      eligibilityStatus.reasons.contains(OldDebtIsTooHigh)) {
      ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooOld()

    } else if (eligibilityStatus.reasons.contains(NoDebt) ||
      eligibilityStatus.reasons.contains(TTPIsLessThenTwoMonths) ||
      eligibilityStatus.reasons.contains(NoDueDate)) {
      journeyLogger.info(s"Sending user to call us page [ineligibility reasons: ${eligibilityStatus.reasons}]")(rh, journey)
      ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUs()

    } else if (eligibilityStatus.reasons.contains(IsNotOnIa)) {
      ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse()

    } else if (eligibilityStatus.reasons.contains(TotalDebtIsTooHigh))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge()

    else if (eligibilityStatus.reasons.contains(ReturnNeedsSubmitting) || eligibilityStatus.reasons.contains(DebtIsInsignificant))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getFileYourTaxReturn()

    else if (eligibilityStatus.reasons.contains(DirectDebitCreatedWithinTheLastYear))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getYouAlreadyHaveAPaymentPlan()

    else {
      journeyLogger.warn(
        s"Eligibility check ERROR - [eligible: ${eligibilityStatus.eligible}]. " +
          s"Case not implemented. It's a bug.")(rh, journey)
      throw new RuntimeException(
        s"Case not implemented. It's a bug in the eligibility reasons. [${journey.maybeEligibilityStatus}]. [$journey]")
    }
  }

  def submit(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    tryLock(request.request.session.get(SessionKeys.sessionId).getOrElse("unknown-session")) {
      journeyService.authorizedForSsttp { implicit journey =>
        journeyLogger.info("Submit arrangement")

        journey.requireScheduleIsDefined()
        journey.requireDdIsDefined()
        val paymentSchedule = selectedSchedule(journey)
        arrangementSetUp(journey, paymentSchedule)
      }
    }.map {
      case Some(res) =>
        res
      case err =>
        appLogger.warn(s"Submit arrangement: locked, duplicate request: $request, err: $err")
        Redirect(routes.ArrangementController.getTermsAndConditions())
    }
  }

  def applicationComplete(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>

    journeyService.getJourney().map { implicit journey =>
      if (journey.status == ApplicationComplete) {
        journeyLogger.info("Application complete")

        // to do a FinishedJourney class without Options would be nice
        val directDebit =
          journey.arrangementDirectDebit.getOrElse(
            throw new RuntimeException(s"arrangementDirectDebit not found for journey [$journey]"))

        val schedule = selectedSchedule(journey)
        Ok(views.application_complete(
          debits        = journey.taxpayer.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()),
          transactionId = journey.taxpayer.selfAssessment.utr + clockProvider.now.toString,
          directDebit,
          schedule,
          journey.ddRef
        ))
      } else technicalDifficulties(journey)
    }
  }

  def viewPaymentPlan(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap { implicit journey =>
      journeyLogger.info("View payment plan")

      val schedule = selectedSchedule(journey)
      Future.successful(Ok(views.view_payment_plan(schedule, journey.ddRef)))
    }
  }

  private def applicationSuccessful(journey: Journey, schedule: PaymentSchedule)(implicit request: Request[_]): Future[Result] = {
    successful(Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))
  }

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(journey: Journey, paymentSchedule: PaymentSchedule)(implicit request: Request[_]): Future[Result] = {
    journeyLogger.info("Arrangement set up: (create a DD and make an Arrangement)")(request, journey)
    val paymentPlanRequest: PaymentPlanRequest = makePaymentPlanRequest(journey)
    val utr = journey.taxpayer.selfAssessment.utr

    ddConnector.submitPaymentPlan(paymentPlanRequest, utr).flatMap[Result] {
      _.fold(submissionError => {
        journeyLogger.info(s"Arrangement set up: dd setup failed, redirecting to error [$submissionError]")(request, journey)
        auditService.sendDirectDebitSubmissionFailedEvent(journey, paymentSchedule, submissionError)
        Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitError())
      },
        directDebitInstructionPaymentPlan => {
          journeyLogger.info("Arrangement set up: dd setup succeeded, now creating arrangement")(request, journey)
          val arrangement: TTPArrangement = makeArrangement(directDebitInstructionPaymentPlan, journey)
          val submitArrangementResult: Future[arrangementConnector.SubmissionResult] = for {
            submissionResult <- arrangementConnector.submitArrangement(arrangement)
            arrangementSubmissionStatus = submissionResult match {
              case Left(submissionError) if submissionError.message.contains("Queued for retry: true") => QueuedForRetry
              case Right(_) => Success
              case _ => PermanentFailure
            }
            newJourney = journey
              .copy(
                ddRef                            = Some(arrangement.directDebitReference),
                status                           = ApplicationComplete,
                maybeArrangementSubmissionStatus = Some(arrangementSubmissionStatus)
              )
            //we finish the journey regardless of the result ...
            _ <- journeyService.saveJourney(newJourney)
            _ = auditService.sendPlanSetUpSuccessEvent(newJourney, selectedSchedule(newJourney), calculatorService)
          } yield submissionResult

          submitArrangementResult.flatMap {
            _.fold(submissionError => {
              journeyLogger.warn(
                s"Arrangement set up outcome - Exception: " +
                  s"ZONK ERROR! Arrangement submission failed, [${submissionError.code}: ${submissionError.message}] " +
                  " but redirecting to application successful " +
                  s"[Payment plan reference: ${arrangement.paymentPlanReference}] " +
                  s"[Direct debit reference: ${arrangement.directDebitReference}]"
              )(request, journey)

              applicationSuccessful(journey, paymentSchedule)
            }, _ => {
              journeyLogger.info(s"Arrangement set up outcome: Arrangement submission Succeeded!")(request, journey)
              applicationSuccessful(journey, paymentSchedule)
            }
            )
          }
        })
    }
  }

  /**
   * Checks if the TTPSubmission data contains an existing direct debit reference number and either
   * passes this information to a payment plan constructor function or builds a new Direct Debit Instruction
   */
  def makePaymentPlanRequest(journey: Journey)(implicit request: Request[_]): PaymentPlanRequest = {

    paymentPlan(
      journey,
      DirectDebitInstruction(
        sortCode      = Some(journey.bankDetails.sortCode),
        accountNumber = Some(journey.bankDetails.accountNumber.reverse.padTo(8, '0').reverse),
        accountName   = Some(journey.bankDetails.accountName),
        ddiRefNumber  = journey.bankDetails.maybeDDIRefNumber))
  }

  /**
   * Builds and returns a payment plan
   */
  def paymentPlan(journey: Journey, ddInstruction: DirectDebitInstruction)(implicit request: Request[_]): PaymentPlanRequest = {
    val knownFact = List(KnownFact(cesa, journey.taxpayer.selfAssessment.utr.value))

    val schedule = selectedSchedule(journey)

    val initialPayment = if (schedule.initialPayment > exact(0)) Some(schedule.initialPayment.toString()) else None
    val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.startDate.plusDays(appConfig.daysToProcessFirstPayment)))

    val lastInstalment: Instalment = schedule.lastInstallment
    val firstInstalment: Instalment = schedule.firstInstalment

    val totalLiability = (schedule.instalments.map(_.amount).sum + schedule.initialPayment).setScale(2, HALF_UP)

    val pp = PaymentPlan(ppType                    = "Time to Pay",
                         paymentReference          = s"${journey.taxpayer.selfAssessment.utr.value}K",
                         hodService                = cesa,
                         paymentCurrency           = paymentCurrency,
                         initialPaymentAmount      = initialPayment,
                         initialPaymentStartDate   = initialStartDate,
                         scheduledPaymentAmount    = firstInstalment.amount.setScale(2, HALF_UP).toString(),
                         scheduledPaymentStartDate = firstInstalment.paymentDate,
                         scheduledPaymentEndDate   = lastInstalment.paymentDate,
                         scheduledPaymentFrequency = paymentFrequency,
                         balancingPaymentAmount    = lastInstalment.amount.setScale(2, HALF_UP).toString(),
                         balancingPaymentDate      = lastInstalment.paymentDate,
                         totalLiability            = totalLiability.toString())

    PaymentPlanRequest(
      "SSTTP",
      ZonedDateTime.now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS).format(ISO_INSTANT),
      knownFact,
      ddInstruction,
      pp,
      printFlag = true
    )
  }

  /**
   * Builds and returns a TTPArrangement
   */
  private def makeArrangement(ddInstruction: DirectDebitInstructionPaymentPlan, journey: Journey)(implicit request: Request[_]): TTPArrangement = {
    val ppReference =
      ddInstruction.paymentPlan.headOption.getOrElse(
        throw new RuntimeException(s"No payment plans for [$ddInstruction]")).ppReferenceNo

    val ddReference =
      ddInstruction.directDebitInstruction
        .headOption.getOrElse(throw new RuntimeException(s"No direct debit instructions for [$ddInstruction]"))
        .ddiReferenceNo.getOrElse(throw new RuntimeException("ddReference not available"))

    val taxpayer = journey.taxpayer
    val schedule = selectedSchedule(journey)

    TTPArrangement(ppReference, ddReference, taxpayer, journey.bankDetails, schedule)
  }

  private def tryLock[T](lockName: String)(body: => Future[T])(implicit ec: ExecutionContext): Future[Option[T]] = {
    val lockKeeper = LockService(mongoLockRepository, lockId = s"des-lock-" + lockName, ttl = 5.minutes)

    lockKeeper.withLock(body)
  }

}
