package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.models._
import views.html.selfservicetimetopay.arrangement.{application_complete, direct_debit_form}

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementController(ddConnector: DirectDebitConnector,
                                                arrangementConnector: ArrangementConnector,
                                                authConnector: AuthConnector,
                                                taxPayerConnector: TaxPayerConnector,
                                                keystoreConnector: KeystoreConnector) extends FrontendController {

  private def createDirectDebitForm: Form[ArrangementDirectDebit] = {
    Form(mapping(
      "accountHolderName" -> nonEmptyText,
      "sortCode1" -> number(min = 0, max = 99),
      "sortCode2" -> number(min = 0, max = 99),
      "sortCode3" -> number(min = 0, max = 99),
      "accountNumber" -> longNumber(min = 0, max = 999999999),
      "confirmed" -> optional(boolean),
      "ddiReferenceNumber" -> optional(text)
    )(ArrangementDirectDebit.apply)(ArrangementDirectDebit.unapply))
  }

  def submit = Action.async { implicit request =>
    createDirectDebitForm.bindFromRequest()
      .fold(error => Future.successful(BadRequest(direct_debit_form.render(error, request))),
      directDebitDetails => {
        for {
          taxpayer <- fetchTaxpayer()
          calculationSchedule <- keystoreConnector.fetch[CalculatorPaymentSchedule](KeystoreKeys.paymentSchedule)
          result <- arrangementSetUp(taxpayer, calculationSchedule, directDebitDetails)
        } yield result
      })
  }

  private def arrangementSetUp(taxPayer: Option[TaxPayer],
                               paymentSchedule: Option[CalculatorPaymentSchedule],
                               directDebit: ArrangementDirectDebit): Future[Result] = {
    (taxPayer, paymentSchedule, directDebit) match {
      case (Some(tp), Some(ps), dd) =>
        val utr = tp.selfAssessment.utr
        (for {
          ddInstruction <- ddConnector.createPaymentPlan(createPaymentPlan(dd, ps, utr), SaUtr(utr))
          ttp <- arrangementConnector.submitArrangements(createArrangement(ddInstruction, tp, ps))
        } yield ttp).flatMap {
          _.fold(error => Future.successful(Redirect("Error")), success => Future.successful(Redirect("Error"))
        }
      case (_, _, _) => Future.successful(Redirect("Error"))
    }
  }

  private def fetchTaxpayer()(implicit hc: HeaderCarrier): Future[Option[TaxPayer]] = {
    for {
      a <- authConnector.currentAuthority
      taxpayer <- taxPayerConnector.getTaxPayer(a.fold("")(_.accounts.sa.get.utr.value))
    } yield taxpayer
  }

  private def createPaymentPlan(directDebit: ArrangementDirectDebit, schedule: CalculatorPaymentSchedule, utr: String): PaymentPlanRequest = {

    val knownFact: List[KnownFact] = List(KnownFact("CESA", utr))

    val instruction = DirectDebitInstruction(Some(directDebit.sortCode),
      Some(directDebit.accountNumber.toString),
      None,
      schedule.startDate,
      Some(false),
      directDebit.ddiReferenceNumber)

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
