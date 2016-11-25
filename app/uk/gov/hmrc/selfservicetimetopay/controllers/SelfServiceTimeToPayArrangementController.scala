package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.selfservicetimetopay.config.FrontendGlobal
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementController(ddConnector: DirectDebitConnector,
                                                arrangementConnector: ArrangementConnector,
                                                sessionCache: SessionCache) extends FrontendController {

  def submit = Action.async { implicit request =>
    for {
      keystoreData <- sessionCache.fetchAndGetEntry[TTPSubmission](FrontendGlobal.sessionCacheKey)
      submission = keystoreData.get
      result <- arrangementSetUp(submission.taxPayer, submission.schedule, submission.bankDetails)
    } yield result
  }

  private def arrangementSetUp(taxPayer: TaxPayer,
                               paymentSchedule: CalculatorPaymentSchedule,
                               bankDetails: BankDetails): Future[Result] = {
    val utr = taxPayer.selfAssessment.utr
    (for {
      ddInstruction <- ddConnector.createPaymentPlan(createPaymentPlan(bankDetails, paymentSchedule, utr), SaUtr(utr))
      ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, taxPayer, paymentSchedule))
    } yield ttp).flatMap {
      _.fold(error => Future.successful(Redirect("Error")), success => Future.successful(Redirect("Error")))
    }
  }

  private def createPaymentPlan(bankDetails: BankDetails, schedule: CalculatorPaymentSchedule, utr: String): PaymentPlanRequest = {
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
                                taxPayer: TaxPayer,
                                schedule: CalculatorPaymentSchedule): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    TTPArrangement(ppReference, ddReference, taxPayer, schedule)
  }
}
