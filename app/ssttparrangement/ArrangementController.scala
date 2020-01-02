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
import controllers.FrontendBaseController
import controllers.action.{Actions, AuthorisedSaUserRequest}
import javax.inject._
import journey.{Journey, JourneyService}
import model.asTaxpayersSaUtr
import play.api.Logger
import play.api.mvc._
import playsession.PlaySessionSupport._
import req.RequestSupport
import ssttpcalculator.{CalculatorConnector, CalculatorService}
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.EligibilityConnector
import timetopaycalculator.cor.model.{CalculatorInput, DebitInput, Instalment, PaymentSchedule}
import timetopaytaxpayer.cor.model.{SelfAssessmentDetails, Taxpayer}
import timetopaytaxpayer.cor.{TaxpayerConnector, model}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.selfservicetimetopay.models._
import views.Views
import _root_.model._

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
    clock:                Clock)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._

  val cesa: String = "CESA"
  val paymentFrequency = "Calendar Monthly"
  val paymentCurrency = "GBP"

  def start: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.getJourney.flatMap {
      case journey @ Journey(_, _, _, _, _, Some(taxpayer), _, _, _, _, _) =>
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

    for {
      tp: model.Taxpayer <- taxPayerConnector.getTaxPayer(asTaxpayersSaUtr(request.utr))
      newJourney: Journey = Journey.newJourney().copy(maybeTaxpayer = Some(tp))
      _ <- journeyService.saveJourney(newJourney)
      result: Result <- eligibilityCheck(newJourney, request.utr)
    } yield result.placeInSession(newJourney._id)

  }

  def getInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp {
      case journey @ Journey(_, _, Some(schedule), _, _, _, Some(CalculatorInput(debits, intialPayment, _, _, _)), _, _, _, _) =>
        Future.successful(Ok(views.instalment_plan_summary(
          journey.taxpayer.selfAssessment.debits,
          intialPayment,
          schedule
        )))
      case _ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.determineEligibility()))
    }
  }

  def submitInstalmentSummary: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp(_ => Future.successful(Redirect(ssttparrangement.routes.ArrangementController.getDeclaration())))
  }

  def getChangeSchedulePaymentDay: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp(ttp => Future.successful(Ok(views.change_day(createDayOfForm(ttp)))))
  }

  def getDeclaration: Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp(ttp => Future.successful(Ok(views.declaration())))
  }

  def submitChangeSchedulePaymentDay(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp {
      submission =>
        ArrangementForm.dayOfMonthForm.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.change_day(formWithErrors)))
          },
          validFormData => {
            submission match {
              case ttp @ Journey(_, _, Some(schedule), _, _, _, Some(CalculatorInput(debits, _, _, _, _)), _, _, _, _) =>
                changeScheduleDay(submission, schedule, debits, validFormData.dayOfMonth).flatMap {
                  ttpSubmission =>
                    journeyService.saveJourney(ttpSubmission).map {
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
  private def changeScheduleDay(
      journey:    Journey,
      schedule:   PaymentSchedule,
      debits:     Seq[DebitInput],
      dayOfMonth: Int
  )(implicit request: Request[_]): Future[Journey] = {

    val input: CalculatorInput = CalculatorService.createCalculatorInput(
      schedule.instalments.length,
      dayOfMonth,
      schedule.initialPayment,
      debits,
      clock
    )

    calculatorConnector.calculatePaymentSchedule(input).map[Journey](paymentSchedule =>
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
    lazy val youNeedToFile = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getYouNeedToFile())
    lazy val notOnIa = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getIaCallUse())
    lazy val overTenThousandOwed = Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.getDebtTooLarge())
    lazy val isEligible = Redirect(ssttpcalculator.routes.CalculatorController.getTaxLiabilities())

    for {
      es: EligibilityStatus <- eligibilityConnector.checkEligibility(EligibilityRequest(LocalDate.now(), journey.taxpayer), utr)
      newJourney = journey.copy(maybeEligibilityStatus = Option(es))
      _ <- journeyService.saveJourney(newJourney)
    } yield {

      if (es.eligible) isEligible
      else if (es.reasons.contains(IsNotOnIa)) notOnIa
      else if (es.reasons.contains(TotalDebtIsTooHigh)) overTenThousandOwed
      else if (es.reasons.contains(ReturnNeedsSubmitting) || es.reasons.contains(DebtIsInsignificant)) youNeedToFile
      else throw new RuntimeException(s"Case not implemented. It's a bug. See eligibility reasons. [$journey]")
    }
  }

  def setDefaultCalculatorSchedule(
      journey: Journey,
      debits:  Seq[DebitInput])(
      implicit
      request: Request[_]
  ): Future[Unit] = {

    //TODO: delte it
    //    submissionService.saveJourney(
    //      journey.copy(
    //        maybeCalculatorData = Some(
    //          CalculatorInput(
    //            startDate = LocalDate.now(),
    //            endDate   = LocalDate.now().plusMonths(2).minusDays(1),
    //            debits    = debits
    //          )
    //        ))
    //    )

    ???
  }

  def submit(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    journeyService.authorizedForSsttp {
      ttp => arrangementSetUp(ttp)
    }
  }

  def applicationComplete(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>

    journeyService.authorizedForSsttp {
      submission =>

        val result = Ok(views.application_complete(
          debits        = submission.taxpayer.selfAssessment.debits.sortBy(_.dueDate.toEpochDay()),
          transactionId = submission.taxpayer.selfAssessment.utr + LocalDateTime.now().toString,
          directDebit   = submission.arrangementDirectDebit.get,
          schedule      = submission.schedule.get,
          ddref         = submission.ddRef
        ))
          .removeJourneyIdFromSession //TODO: if user refreshes the page the content is lost

        Future.successful(result)
    }
  }

  private def applicationSuccessful = successful(Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))

  /**
   * Submits a payment plan to the direct-debit service and then submits the arrangement to the arrangement service.
   * As the arrangement details are persisted in a database, the user is directed to the application
   * complete page if we get an error response from DES passed back by the arrangement service.
   */
  private def arrangementSetUp(submission: Journey)(implicit request: Request[_]): Future[Result] = {
    submission.maybeTaxpayer match {
      case Some(Taxpayer(_, _, SelfAssessmentDetails(utr, _, _, _))) =>
        ddConnector.createPaymentPlan(checkExistingBankDetails(submission), utr).flatMap[Result] {
          _.fold(_ => Redirect(ssttpdirectdebit.routes.DirectDebitController.getDirectDebitError()),
            success => {
              val arrangement = createArrangement(success, submission)
              val result = for {
                submissionResult <- arrangementConnector.submitArrangements(arrangement)
                _ = auditService.sendSubmissionEvent(submission)
                _ = journeyService.saveJourney(submission.copy(ddRef = Some(arrangement.directDebitReference)))
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
  private def checkExistingBankDetails(submission: Journey): PaymentPlanRequest = {
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
  private def paymentPlan(submission: Journey, ddInstruction: DirectDebitInstruction): PaymentPlanRequest = {
    val paymentPlanRequest = for {
      schedule <- submission.schedule
      taxPayer <- submission.maybeTaxpayer

    } yield {
      val knownFact = List(KnownFact(cesa, taxPayer.selfAssessment.utr.value))

      val initialPayment = if (schedule.initialPayment > BigDecimal.exact(0)) Some(schedule.initialPayment.toString()) else None
      val initialStartDate = initialPayment.fold[Option[LocalDate]](None)(_ => Some(schedule.startDate.plusWeeks(1)))

      val lastInstalment: Instalment = schedule.instalments.last
      val firstInstalment: Instalment = schedule.instalments.head

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
                           totalLiability            = (schedule.instalments.map(_.amount).sum + schedule.initialPayment).toString())

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

    TTPArrangement(ppReference, ddReference, taxpayer, schedule)
  }

  private def createDayOfForm(ttpSubmission: Journey) = {

    import _root_.model.PaymentScheduleSupport._

    ttpSubmission.schedule.fold(ArrangementForm.dayOfMonthForm)(p => {
      ArrangementForm.dayOfMonthForm.fill(ArrangementDayOfMonth(p.getMonthlyInstalmentDate))
    })
  }
}
