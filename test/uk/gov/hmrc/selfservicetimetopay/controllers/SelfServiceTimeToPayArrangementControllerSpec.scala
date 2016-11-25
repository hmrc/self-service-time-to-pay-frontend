package uk.gov.hmrc.selfservicetimetopay.controllers

import java.time.LocalDate

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.selfservicetimetopay.config.FrontendGlobal
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.models._

import scala.concurrent.Future

class SelfServiceTimeToPayArrangementControllerSpec extends UnitSpec
  with MockitoSugar with WithFakeApplication with ScalaFutures {
  type SubmissionResult = Either[SubmissionError, SubmissionSuccess]

  val authConnector = mock[AuthConnector]
  val ddConnector = mock[DirectDebitConnector]
  val arrangementConnector = mock[ArrangementConnector]
  val taxPayerConnector = mock[TaxPayerConnector]
  val sessionCache = mock[SessionCache]

  val controller = new SelfServiceTimeToPayArrangementController(
    ddConnector,
    arrangementConnector,
    sessionCache
  )

  private def ttpSubmission(): TTPSubmission = TTPSubmission(
    CalculatorPaymentSchedule(
      Some(new LocalDate("2001-01-01")),
      Some(new LocalDate("2001-01-01")),
      BigDecimal(1024.12),
      BigDecimal(20123.76),
      BigDecimal(1024.12),
      BigDecimal(102.67),
      BigDecimal(20123.76),
      Seq(CalculatorPaymentScheduleInstalment(
        LocalDate.now(),
        BigDecimal(1234.22))
      )
    ),
    BankDetails("012131", "1234567890", None, None, Some("0987654321")),
    TaxPayer("Bob", List(), SelfAssessment("utr", None, List(), None))
  )

  private def directDebitInstructionPaymentPlan(): DirectDebitInstructionPaymentPlan = {
    DirectDebitInstructionPaymentPlan(LocalDate.now().toString, "1234567890", List(), List())
  }

  private def paymentPlanRequest(): PaymentPlanRequest = PaymentPlanRequest(
    "Requesting service",
    "2017-01-01",
    List(),
    DirectDebitInstruction(
      None,
      None,
      None,
      None,
      true,
      None),
    PaymentPlan(
      "ppType",
      "paymentRef",
      "hodService",
      "GBP",
      BigDecimal(192.22),
      LocalDate.now(),
      BigDecimal(722.22),
      LocalDate.now(),
      LocalDate.now(),
      "scheduledPaymentFrequency",
      BigDecimal(162.11),
      LocalDate.now(),
      BigDecimal(282.11)),
    true)

  private def ttpArrangement(): TTPArrangement = TTPArrangement(
    "paymentPlanReference",
    "directDebitReference",
    TaxPayer(
      "Bob",
      List(),
      SelfAssessment(
        "utr",
        None,
        List(),
        None)),
    CalculatorPaymentSchedule(
      Some(new LocalDate("2001-01-01")),
      Some(new LocalDate("2001-01-01")),
      BigDecimal(1024.12),
      BigDecimal(20123.76),
      BigDecimal(1024.12),
      BigDecimal(102.67),
      BigDecimal(20123.76),
      Seq(CalculatorPaymentScheduleInstalment(
        LocalDate.now(),
        BigDecimal(1234.22))
      )
    )
  )

  "Self Service Time To Pay Arrangement Controller" should {
    "return success and display the application complete page" in {

      when(sessionCache.fetchAndGetEntry[TTPSubmission](FrontendGlobal.sessionCacheKey))
        .thenReturn(Future.successful(Some(ttpSubmission())))

      when(ddConnector.createPaymentPlan(paymentPlanRequest(), SaUtr("utr")))
        .thenReturn(Future.successful(directDebitInstructionPaymentPlan()))

      when(arrangementConnector.submitArrangements(ttpArrangement()))
        .thenReturn(Future.successful(Right(SubmissionSuccess())))

      implicit val hc = new HeaderCarrier
      val result = controller.submit()

      //verify result
    }

  }
}
