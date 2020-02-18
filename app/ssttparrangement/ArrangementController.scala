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

package ssttparrangement

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate, LocalDateTime, ZonedDateTime}

import audit.AuditService
import config.AppConfig
import controllers.{ErrorHandler, FrontendBaseController}
import controllers.action.{Actions, AuthorisedSaUserRequest}
import javax.inject._
import journey.{Journey, JourneyService, Statuses}
import model.asTaxpayersSaUtr
import play.api.Logger
import play.api.mvc._
import playsession.PlaySessionSupport._
import req.RequestSupport
import ssttpcalculator.{CalculatorConnector, CalculatorPaymentScheduleExt, CalculatorService}
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.EligibilityConnector
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import timetopaytaxpayer.cor.{TaxpayerConnector, model}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views
import _root_.model._
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal

class ArrangementController @Inject() (
    mcc:                  MessagesControllerComponents,
    ddConnector:          DirectDebitConnector,
    arrangementConnector: ArrangementConnector,
    calculatorService:    CalculatorService,
    calculatorConnector:  CalculatorConnector,
    taxPayerConnector:    TaxpayerConnector,
    eligibilityConnector: EligibilityConnector,
    auditService:         AuditService,
    journeyService:       JourneyService,
    as:                   Actions,
    requestSupport:       RequestSupport,
    views:                Views,
    clockProvider:        ClockProvider)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext) extends FrontendBaseController(mcc) {

  import requestSupport._
  import clockProvider._

  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.start: $request")

    journeyService.getJourney.flatMap {
      case journey @ Journey(_, Statuses.InProgress, _, _, _, _, _, Some(taxpayer), _, _, _, _, _) =>
        eligibilityCheck(journey, request.utr)
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
      newJourney: Journey = Journey.newJourney.copy(maybeTaxpayer = Some(tp))
      _ <- journeyService.saveJourney(newJourney)
      result: Result <- eligibilityCheck(newJourney, request.utr)
    } yield result.placeInSession(newJourney._id)

  }

  def getInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getInstalmentSummary: $request")
    journeyService.authorizedForSsttp {
      case journey @ Journey(_, Statuses.InProgress, _, _, Some(schedule), _, _, _, Some(CalculatorInput(debits, intialPayment, _, _, _)), _, _, _, _) =>
        Future.successful(Ok(views.instalment_plan_summary(
          journey.taxpayer.selfAssessment.debits,
          intialPayment,
          schedule.schedule
        )))
      case _ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.determineEligibility()))
    }
  }

  def submitInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submitInstalmentSummary: $request")
    journeyService.authorizedForSsttp(_ => Future.successful(Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebit())))
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getChangeSchedulePaymentDay: $request")
    journeyService.authorizedForSsttp(ttp => Future.successful(Ok(views.change_day(createDayOfForm(ttp)))))
  }

  def getTermsAndConditions: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.getDeclaration: $request")
    journeyService.authorizedForSsttp(ttp => Future.successful(Ok(views.terms_and_conditions())))
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submitChangeSchedulePaymentDay: $request")
    journeyService.authorizedForSsttp {
      submission =>
        ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.change_day(formWithErrors)))
          },
          validFormData => {
            submission match {
              case ttp @ Journey(_, Statuses.InProgress, _, _, Some(schedule), _, _, _, Some(CalculatorInput(debits, _, _, _, _)), _, _, _, _) =>
                JourneyLogger.info(s"changing schedule day to [${validFormData.dayOfMonth}]")
                changeScheduleDay(submission, schedule.schedule, debits, validFormData.dayOfMonth).flatMap {
                  ttpSubmission =>
                    journeyService.saveJourney(ttpSubmission).map {
                      _ => Redirect(ssttparrangement.routes.ArrangementController.getInstalmentSummary())
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
  private def changeScheduleDay(
      journey:    Journey,
      schedule:   PaymentSchedule,
      debits:     Seq[DebitInput],
      dayOfMonth: Int
  )(implicit request: Request[_]): Future[Journey] = {

    val months = schedule.instalments.length
    val input: CalculatorInput = CalculatorService.createCalculatorInput(
      durationMonths = months,
      dayOfMonth     = dayOfMonth,
      initialPayment = schedule.initialPayment,
      debits         = debits
    )

    calculatorConnector.calculatePaymentSchedule(input)
      .map(CalculatorPaymentScheduleExt(months, _))
      .map[Journey](paymentSchedule =>
        journey.copy(
          schedule            = Some(paymentSchedule),
          maybeCalculatorData = Some(input)
        )
      )
  }

  /**
   * Call the eligibility service using the Taxpayer data
   * and display the appropriate page based on the result
   */
  private def eligibilityCheck(journey: Journey, utr: SaUtr)(implicit request: Request[_]): Future[Result] = {
    JourneyLogger.info(s"ArrangementController.eligibilityCheck")

    lazy val youNeedToFile = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile())
    lazy val notOnIa = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse())
    lazy val overTenThousandOwed = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge())
    lazy val notEligible = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getTtpCallUs())
    lazy val isEligible = Redirect(ssttpcalculator.routes.CalculatorController.getTaxLiabilities())

    for {
      es: EligibilityStatus <- eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(clockProvider.getClock), journey.taxpayer), utr)
      newJourney: Journey = journey.copy(maybeEligibilityStatus = Option(es))
      _ <- journeyService.saveJourney(newJourney)
      _ = JourneyLogger.info(s"ArrangementController.eligibilityCheck [eligible=${es.eligible}]", newJourney)
    } yield {
      if (es.eligible) isEligible
      else if (es.reasons.contains(DebtTooOld) || es.reasons.contains(OldDebtIsTooHigh)) notEligible
      else if (es.reasons.contains(NoDebt) || es.reasons.contains(TTPIsLessThenTwoMonths)) notEligible
      else if (es.reasons.contains(IsNotOnIa)) notOnIa
      else if (es.reasons.contains(TotalDebtIsTooHigh)) overTenThousandOwed
      else if (es.reasons.contains(ReturnNeedsSubmitting) || es.reasons.contains(DebtIsInsignificant)) youNeedToFile
      else {
        JourneyLogger.info(s"ArrangementController.eligibilityCheck ERROR - [eligible=${es.eligible}]. Case not implemented. It's a bug.", newJourney)
        throw new RuntimeException(s"Case not implemented. It's a bug in the eligibility reasons. [${journey.maybeEligibilityStatus}]. [$journey]")
      }
    }
  }

  def submit(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.submit: $request")
    journeyService.authorizedForSsttp {
      ttp => arrangementSetUp(ttp)
    }
  }

  def applicationComplete(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"ArrangementController.applicationComplete: $request")

    for {
      journey <- journeyService.getJourney()
    } yield {

      if (journey.status == Statuses.FinishedApplicationSuccessful) {
        Ok(views.application_complete(
          debits        = journey.taxpayer.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()),
          transactionId = journey.taxpayer.selfAssessment.utr + LocalDateTime.now(clockProvider.getClock).toString,
          directDebit   = journey.arrangementDirectDebit.get,
          schedule      = journey.schedule.get.schedule,
          ddref         = journey.ddRef
        ))
      } else {
        ErrorHandler.technicalDifficulties(journey)
      }
    }
  }

  private def applicationSuccessful = successful(Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(journey: Journey)(implicit request: Request[_]): Future[Result] = {
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
                _ = auditService.sendSubmissionEvent(journey)
                newJourney = journey
                  .copy(
                    ddRef  = Some(arrangement.directDebitReference),
                    status = Statuses.FinishedApplicationSuccessful
                  )
                _ = journeyService.saveJourney(newJourney)
              } yield submissionResult

              result.flatMap {
                _.fold(error => {
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
        Future.successful(Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getNotSaEnrolled()))
    }
  }

  /**
   * Checks if the TTPSubmission data contains an existing direct debit reference number and either
   * passes this information to a payment plan constructor function or builds a new Direct Debit Instruction
   */
  private def checkExistingBankDetails(submission: Journey)(implicit request: Request[_]) = {
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
  private def paymentPlan(submission: Journey, ddInstruction: DirectDebitInstruction): PaymentPlanRequest = {
    val paymentPlanRequest = for {
      schedule <- submission.schedule
      taxPayer <- submission.maybeTaxpayer

    } yield {
      val knownFact = List(KnownFact(cesa, taxPayer.selfAssessment.utr.value))

      val initialPayment = if (schedule.schedule.initialPayment > BigDecimal.exact(0)) Some(schedule.schedule.initialPayment.toString()) else None
      val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.schedule.startDate.plusWeeks(1)))

      val lastInstalment: Instalment = schedule.schedule.instalments.last
      val firstInstalment: Instalment = schedule.schedule.instalments.head

      val pp = PaymentPlan(ppType                    = "Time to Pay",
                           paymentReference          = s"${
          taxPayer.selfAssessment.utr.value
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
                                submission:    Journey): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiReferenceNo.getOrElse(throw new RuntimeException("ddReference not available"))
    val taxpayer = submission.maybeTaxpayer.getOrElse(throw new RuntimeException("Taxpayer data not present"))
    val schedule = submission.schedule.getOrElse(throw new RuntimeException("Schedule data not present"))

    TTPArrangement(ppReference, ddReference, taxpayer, schedule.schedule)
  }

  private def createDayOfForm(ttpSubmission: Journey) = {

    import _root_.model.PaymentScheduleSupport._

    ttpSubmission.schedule.fold(ArrangementForm.dayOfMonthForm)((p: CalculatorPaymentScheduleExt) => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.schedule.getMonthlyInstalmentDate))
    })
  }
}
