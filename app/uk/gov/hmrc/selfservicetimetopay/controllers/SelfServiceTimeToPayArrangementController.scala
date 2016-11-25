package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.config.FrontendGlobal
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementController(ddConnector: DirectDebitConnector,
                                                arrangementConnector: ArrangementConnector,
                                                taxPayerConnector: TaxPayerConnector,
                                                authConnector: AuthConnector,
                                                sessionCache: SessionCache) extends FrontendController {

  def submit = Action.async { implicit request =>
    for {
      keystoreData <- sessionCache.fetchAndGetEntry[TTPSubmission](FrontendGlobal.sessionCacheKey)
      submission: TTPSubmission = keystoreData.get
      bankDetails: BankDetails = submission.bankDetails
      taxpayer <- fetchTaxpayer()
      calculationSchedule <- submission.schedule
      result <- arrangementSetUp(taxpayer, calculationSchedule, bankDetails.ddiRefNumber.get, bankDetails)
    } yield result
  }

  private def arrangementSetUp(taxPayer: Option[TaxPayer],
                               paymentSchedule: Option[CalculatorPaymentSchedule],
                               ddiRefNumber: String,
                               bankDetails: BankDetails): Future[Result] = {
    (taxPayer, paymentSchedule, ddiRefNumber, bankDetails) match {
      case (Some(tp), Some(ps), ddiRefNo, bd) =>
        val utr = tp.selfAssessment.utr
        (for {
          ddInstruction <- ddConnector.createPaymentPlan(createPaymentPlan(bd, ddiRefNo, ps, utr), SaUtr(utr))
          ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, tp, ps))
        } yield ttp).flatMap {
          _.fold(error => Future.successful(Redirect("Error")), success => Future.successful(Redirect("Error")))
        }
      case (_, _, _, _, _) => Future.successful(Redirect("Error"))
    }
  }

  private def fetchTaxpayer()(implicit hc: HeaderCarrier): Future[Option[TaxPayer]] = {
    for {
      a <- authConnector.currentAuthority
      taxpayer <- taxPayerConnector.getTaxPayer(a.fold("")(_.accounts.sa.get.utr.value))
    } yield taxpayer
  }

  private def createPaymentPlan(bankDetails: BankDetails, ddiRefNumber: String, schedule: CalculatorPaymentSchedule, utr: String): PaymentPlanRequest = {

    val knownFact: List[KnownFact] = List(KnownFact("CESA", utr))

    val instruction = DirectDebitInstruction(sortCode = Some(bankDetails.sortCode),
      accountNumber = Some(bankDetails.accountNumber.toString),
      creationDate = schedule.startDate,
      ddiRefNo = Some(ddiRefNumber))

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
                                taxPayer: TaxPayer, schedule: CalculatorPaymentSchedule): TTPArrangement = {
    val ppReference: String = ddInstruction.paymentPlan.head.ppReferenceNo
    val ddReference: String = ddInstruction.directDebitInstruction.head.ddiRefNo.get
    TTPArrangement(ppReference, ddReference, taxPayer, schedule)
  }
}
