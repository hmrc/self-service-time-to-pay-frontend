package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.config.FrontendGlobal
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllerVariables._
import uk.gov.hmrc.selfservicetimetopay.controllers.ArrangementController._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement.application_complete
import views.html.selfservicetimetopay.core.service_start

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementController(ddConnector: DirectDebitConnector,
                                                arrangementConnector: ArrangementConnector,
                                                sessionCache: SessionCache) extends FrontendController {


  def submit = Action.async { implicit request =>
    sessionCache.fetchAndGetEntry[TTPSubmission](FrontendGlobal.sessionCacheKey).flatMap {
      _.fold(redirectToStart)(arrangementSetUp)
    }
  }

  def applicationComplete() = Action.async { implicit request =>
    sessionCache.fetchAndGetEntry[TTPSubmission](FrontendGlobal.sessionCacheKey).flatMap {
      _.fold(redirectToStart)(submission => {
        sessionCache.remove().flatMap {
         _ => Future.successful(Ok(application_complete.render(submission.schedule, request)))
        }
      })
    }
  }


  private def redirectToStart = Future.successful(Redirect(routes.SelfServiceTimeToPayController.present()))

  private def arrangementSetUp(submission: TTPSubmission): Future[Result] = {

    def applicationSuccessful = Future.successful(Redirect(routes.SelfServiceTimeToPayArrangementController.applicationComplete()))

    val utr = submission.taxPayer.selfAssessment.utr
    (for {
      ddInstruction <- ddConnector.createPaymentPlan(paymentPlan(submission), SaUtr(utr))
      ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, submission))
    } yield ttp).flatMap {
      _.fold(error => Future.successful(Redirect("")), success => applicationSuccessful)
    }
  }

  private def paymentPlan(submission: TTPSubmission): PaymentPlanRequest = {
    val bankDetails = submission.bankDetails
    val schedule = submission.schedule
    val utr = submission.taxPayer.selfAssessment.utr
    val knownFact: List[KnownFact] = List(KnownFact("CESA", utr))

    val instruction = DirectDebitInstruction(sortCode = Some(bankDetails.sortCode),
      accountNumber = Some(bankDetails.accountNumber.toString),
      creationDate = schedule.startDate,
      ddiRefNo = bankDetails.ddiRefNumber)

    val lastInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.last
    val firstInstalment: CalculatorPaymentScheduleInstalment = schedule.instalments.head
    val pp = PaymentPlan("Time to Pay",
      utr,
      "CESA",
      "GBP",
      schedule.initialPayment,
      schedule.startDate.get,
      firstInstalment.amount,
      firstInstalment.paymentDate,
      lastInstalment.paymentDate,
      "Monthly",
      lastInstalment.amount,
      lastInstalment.paymentDate,
      schedule.amountToPay)

    PaymentPlanRequest("Requesting Service", LocalDate.now().toString, knownFact, instruction, pp)
  }

  private def createArrangement(ddInstruction: DirectDebitInstructionPaymentPlan,
                                submission: TTPSubmission): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    TTPArrangement(ppReference, ddReference, submission.taxPayer, submission.schedule)
  }
}
