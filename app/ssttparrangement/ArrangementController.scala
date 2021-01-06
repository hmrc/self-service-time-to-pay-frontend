/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.{LocalDate, ZonedDateTime}

import _root_.model._
import audit.AuditService
import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.{Actions, AuthorisedSaUserRequest}
import javax.inject._
import journey.Statuses.{FinishedApplicationSuccessful, InProgress}
import journey.{Journey, JourneyService}
import play.api.Logger
import play.api.mvc._
import playsession.PlaySessionSupport._
import req.RequestSupport
import ssttparrangement.ArrangementForm.dayOfMonthForm
import ssttpcalculator.CalculatorService
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.{EligibilityService, IaService}
import times.ClockProvider
import ssttpcalculator.model.{Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import timetopaytaxpayer.cor.{TaxpayerConnector, model}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.exact

class ArrangementController @Inject() (
    mcc:                  MessagesControllerComponents,
    ddConnector:          DirectDebitConnector,
    arrangementConnector: ArrangementConnector,
    calculatorService:    CalculatorService,
    eligibilityService:   EligibilityService,
    taxPayerConnector:    TaxpayerConnector,
    auditService:         AuditService,
    journeyService:       JourneyService,
    as:                   Actions,
    requestSupport:       RequestSupport,
    views:                Views,
    clockProvider:        ClockProvider,
    iaService:            IaService,
    directDebitConnector: DirectDebitConnector)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext) extends FrontendBaseController(mcc) {

  import clockProvider._
  import requestSupport._

  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.start: $request")

    journeyService.getJourney.flatMap {
      case journey @ Journey(_, InProgress, _, _, _, _, _, _, Some(_), _, _, _, _, _, _) => eligibilityCheck(journey)
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
    JourneyLogger.info(s"ArrangementController.determineEligibility: $request")

    for {
      tp: model.Taxpayer <- taxPayerConnector.getTaxPayer(asTaxpayersSaUtr(request.utr))
      maybeJourney <- journeyService.getMaybeJourney()
      journey = maybeJourney.filterNot(_.isFinished).getOrElse(Journey.newJourney).copy(maybeTaxpayer = Some(tp))
      _ <- journeyService.saveJourney(journey)
      result: Result <- eligibilityCheck(journey)
    } yield result.placeInSession(journey._id)

  }

  def getInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getInstalmentSummary: $request")
    journeyService.authorizedForSsttp { journey =>
      journey.requireScheduleIsDefined()
      val schedule: PaymentSchedule = calculatorService.computeSchedule(journey)
      Future.successful(Ok(views.instalment_plan_summary(
        journey.taxpayer.selfAssessment.debits,
        journey.safeInitialPayment,
        schedule
      )))
    }
  }

  def submitInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submitInstalmentSummary: $request")
    journeyService.authorizedForSsttp(_ => Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit())))
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getChangeSchedulePaymentDay: $request")
    journeyService.authorizedForSsttp { journey =>
      val form = dayOfMonthForm
      val formWithData = journey.maybeArrangementDayOfMonth
        .map(arrangmentDayOfMonth => form.fill(ArrangementForm(arrangmentDayOfMonth.dayOfMonth, false)))
        .getOrElse(form)
      Future.successful(Ok(views.change_day(formWithData)))
    }
  }

  def getTermsAndConditions: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getDeclaration: $request")
    journeyService.authorizedForSsttp(_ => Future.successful(Ok(views.terms_and_conditions())))
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submitChangeSchedulePaymentDay: $request")
    journeyService.authorizedForSsttp {
      journey =>
        dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.change_day(formWithErrors)))
          },
          (validFormData: ArrangementForm) => {
            JourneyLogger.info(s"changing schedule day to [${validFormData.dayOfMonth}]")
            val updatedJourney = journey.copy(maybeArrangementDayOfMonth = Some(ArrangementDayOfMonth(validFormData.dayOfMonth)))
            journeyService.saveJourney(updatedJourney).map {
              _ => Redirect(ssttpcalculator.routes.CalculatorController.getCalculateInstalments())
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
    JourneyLogger.info(s"ArrangementController.eligibilityCheck")

    val taxpayer = journey.taxpayer
    val utr = taxpayer.selfAssessment.utr

    for {
      onIa <- iaService.checkIaUtr(utr.value)
      directDebits <- directDebitConnector.getBanks(utr)
      eligibilityStatus = eligibilityService.checkEligibility(clockProvider.nowDate, taxpayer, directDebits, onIa)
      newJourney: Journey = journey.copy(maybeEligibilityStatus = Option(eligibilityStatus))
      _ <- journeyService.saveJourney(newJourney)
    } yield {
      JourneyLogger.info(s"ArrangementController.eligibilityCheck [eligible=${eligibilityStatus.eligible}]", newJourney)

      if (eligibilityStatus.eligible) Redirect(ssttpcalculator.routes.CalculatorController.getTaxLiabilities())
      else Redirect(ineligibleStatusRedirect(eligibilityStatus, newJourney))
    }
  }

  //TODO improve this under OPS-4941
  private def ineligibleStatusRedirect(eligibilityStatus: EligibilityStatus, newJourney: Journey)(implicit hc: HeaderCarrier) =
    if (eligibilityStatus.reasons.contains(DebtTooOld) ||
      eligibilityStatus.reasons.contains(OldDebtIsTooHigh) ||
      eligibilityStatus.reasons.contains(NoDebt) ||
      eligibilityStatus.reasons.contains(TTPIsLessThenTwoMonths) ||
      eligibilityStatus.reasons.contains(DirectDebitCreatedWithinTheLastYear) ||
      eligibilityStatus.reasons.contains(NoDueDate)) {
      JourneyLogger.info(s"Sent user to call us page [ineligibility reasons: ${eligibilityStatus.reasons}]")
      ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUs()
    } else if (eligibilityStatus.reasons.contains(IsNotOnIa))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse()
    else if (eligibilityStatus.reasons.contains(TotalDebtIsTooHigh))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge()
    else if (eligibilityStatus.reasons.contains(ReturnNeedsSubmitting) || eligibilityStatus.reasons.contains(DebtIsInsignificant))
      ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile()
    else {
      JourneyLogger.info(
        s"ArrangementController.eligibilityCheck ERROR - [eligible=${eligibilityStatus.eligible}]. " +
          s"Case not implemented. It's a bug.", newJourney)
      throw new RuntimeException(
        s"Case not implemented. It's a bug in the eligibility reasons. [${newJourney.maybeEligibilityStatus}]. [$newJourney]")
    }

  def submit(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submit: $request")
    journeyService.authorizedForSsttp { ttp =>
      ttp.requireScheduleIsDefined()
      ttp.requireDdIsDefined()
      val paymentSchedule = calculatorService.computeSchedule(ttp)
      arrangementSetUp(ttp, paymentSchedule)
    }
  }

  def applicationComplete(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.applicationComplete: $request")

    journeyService.getJourney().map { journey =>
      if (journey.status == FinishedApplicationSuccessful) {
        // to do a FinishedJourney class without Options would be nice
        val directDebit =
          journey.arrangementDirectDebit.getOrElse(
            throw new RuntimeException(s"arrangementDirectDebit not found for journey [$journey]"))

        val schedule = calculatorService.computeSchedule(journey)
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

  private def applicationSuccessful = successful(Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(journey: Journey, paymentSchedule: PaymentSchedule)(implicit request: Request[_]): Future[Result] = {
    JourneyLogger.info("ArrangementController.arrangementSetUp: (create a DD and make an Arrangement)")
    journey.maybeTaxpayer match {
      case Some(Taxpayer(_, _, SelfAssessmentDetails(utr, _, _, _))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(journey), utr).flatMap[Result] {
          _.fold(_ => {
            JourneyLogger.info("ArrangementController.arrangementSetUp: dd setup failed, redirecting to error page")
            Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitError())
          },
            success => {
              JourneyLogger.info("ArrangementController.arrangementSetUp: dd setup succeeded, now creating arrangement")
              val arrangement = createArrangement(success, journey)
              val result = for {
                submissionResult <- arrangementConnector.submitArrangements(arrangement)
                _ = auditService.sendSubmissionEvent(journey, paymentSchedule)
                newJourney = journey
                  .copy(
                    ddRef  = Some(arrangement.directDebitReference),
                    status = FinishedApplicationSuccessful
                  )
                _ = journeyService.saveJourney(newJourney)
              } yield submissionResult

              result.flatMap {
                _.fold(error => {
                  Logger.error(s"Exception: ${error.code} + ${error.message}")
                  JourneyLogger.info(
                    s"ArrangementController.arrangementSetUp: ZONK ERROR! Arrangement submission failed, $error but redirecting to $applicationSuccessful")
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
        Future.successful(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled()))
    }
  }

  /**
   * Checks if the TTPSubmission data contains an existing direct debit reference number and either
   * passes this information to a payment plan constructor function or builds a new Direct Debit Instruction
   */
  private def checkExistingBankDetails(journey: Journey)(implicit request: Request[_]) = {
    JourneyLogger.info("ArrangementController.checkExistingBankDetails")

    paymentPlan(
      journey,
      DirectDebitInstruction(
        sortCode      = Some(journey.bankDetails.sortCode),
        accountNumber = Some(journey.bankDetails.accountNumber),
        accountName   = Some(journey.bankDetails.accountName),
        ddiRefNumber  = journey.bankDetails.maybeDDIRefNumber))
  }

  /**
   * Builds and returns a payment plan
   */
  private def paymentPlan(journey: Journey, ddInstruction: DirectDebitInstruction)(implicit request: Request[_]): PaymentPlanRequest = {
    val knownFact = List(KnownFact(cesa, journey.taxpayer.selfAssessment.utr.value))

    val schedule = calculatorService.computeSchedule(journey)

    val initialPayment = if (schedule.initialPayment > exact(0)) Some(schedule.initialPayment.toString()) else None
    val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.startDate.plusWeeks(1)))

    val lastInstalment: Instalment = schedule.lastInstallment
    val firstInstalment: Instalment = schedule.firstInstallment

    val totalLiability = schedule.instalments.map(_.amount).sum + schedule.initialPayment

    val pp = PaymentPlan(ppType                    = "Time to Pay",
                         paymentReference          = s"${journey.taxpayer.selfAssessment.utr.value}K",
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
                         totalLiability            = totalLiability.toString())

    PaymentPlanRequest("SSTTP", ZonedDateTime.now.format(ISO_INSTANT), knownFact, ddInstruction, pp, printFlag = true)
  }

  /**
   * Builds and returns a TTPArrangement
   */
  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan, journey: Journey)(implicit request: Request[_]): TTPArrangement = {
    val ppReference =
      ddInstruction.paymentPlan.headOption.getOrElse(
        throw new RuntimeException(s"No payment plans for [$ddInstruction]")).ppReferenceNo

    val ddReference =
      ddInstruction.directDebitInstruction
        .headOption.getOrElse(throw new RuntimeException(s"No direct debit instructions for [$ddInstruction]"))
        .ddiReferenceNo.getOrElse(throw new RuntimeException("ddReference not available"))

    val taxpayer = journey.taxpayer
    val schedule = calculatorService.computeSchedule(journey)

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }

}
