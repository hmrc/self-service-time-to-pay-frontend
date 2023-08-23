
package util

import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import ssttparrangement.SubmissionError
import uk.gov.hmrc.selfservicetimetopay.models.{PaymentPlanRequest, TTPArrangement}

object DataLogging {
  trait ArrangementLogging {
    self: BaseLogger =>

    def debug(message: => String, arrangement: TTPArrangement)(implicit request: RequestHeader): Unit = logMessage(message, arrangement, Debug)

    def info(message: => String, arrangement: TTPArrangement)(implicit request: RequestHeader): Unit = logMessage(message, arrangement, Info)

    def warn(message: => String, arrangement: TTPArrangement)(implicit request: RequestHeader): Unit = logMessage(message, arrangement, Warn)

    def error(message: => String, arrangement: TTPArrangement)(implicit request: RequestHeader): Unit = logMessage(message, arrangement, Error)

    private def appendedArrangement(arrangement: TTPArrangement): String = s" [arrangement: ${appendedData(Json.toJson(arrangement.obfuscate))}]"

    private def logMessage(message: => String, arrangement: TTPArrangement, level: LogLevel)(implicit request: RequestHeader): Unit = {
      lazy val richMessageWithArrangement = makeRichMessage(message) + appendedArrangement(arrangement)
      level match {
        case Debug => log.debug(richMessageWithArrangement)
        case Info => log.info(richMessageWithArrangement)
        case Warn => log.warn(richMessageWithArrangement)
        case Error => log.error(richMessageWithArrangement)
      }
    }
  }

  trait SubmissionErrorLogging {
    self: BaseLogger =>

    def debug(message: => String, submissionError: SubmissionError)(implicit rh: RequestHeader): Unit = logMessage(message, submissionError, Debug)

    def info(message: => String, submissionError: SubmissionError)(implicit rh: RequestHeader): Unit = logMessage(message, submissionError, Info)

    def warn(message: => String, submissionError: SubmissionError)(implicit rh: RequestHeader): Unit = logMessage(message, submissionError, Warn)

    def error(message: => String, submissionError: SubmissionError)(implicit rh: RequestHeader): Unit = logMessage(message, submissionError, Error)

    private def appendedSubmissionError(submissionError: SubmissionError): String = s" [submission error: ${appendedData(Json.toJson(submissionError))}]"

    private def logMessage(message: => String, submissionError: SubmissionError, level: LogLevel)(implicit rh: RequestHeader): Unit = {
      lazy val richMessageWithArrangement = makeRichMessage(message) + appendedSubmissionError(submissionError)
      level match {
        case Debug => log.debug(richMessageWithArrangement)
        case Info => log.info(richMessageWithArrangement)
        case Warn => log.warn(richMessageWithArrangement)
        case Error => log.error(richMessageWithArrangement)
      }
    }
  }

  trait PaymentPlanRequestLogging {
    self: BaseLogger =>

    def debug(message: => String, paymentPlanRequest: PaymentPlanRequest)(implicit request: RequestHeader): Unit = logMessage(message, paymentPlanRequest, Debug)

    def info(message: => String, paymentPlanRequest: PaymentPlanRequest)(implicit request: RequestHeader): Unit = logMessage(message, paymentPlanRequest, Info)

    def warn(message: => String, paymentPlanRequest: PaymentPlanRequest)(implicit request: RequestHeader): Unit = logMessage(message, paymentPlanRequest, Warn)

    def error(message: => String, paymentPlanRequest: PaymentPlanRequest)(implicit request: RequestHeader): Unit = logMessage(message, paymentPlanRequest, Error)

    private def appendedPaymentPlanRequest(arrangement: PaymentPlanRequest): String = s" [arrangement: ${appendedData(Json.toJson(arrangement.obfuscate))}]"

    private def logMessage(message: => String, paymentPlanRequest: PaymentPlanRequest, level: LogLevel)(implicit request: RequestHeader): Unit = {
      lazy val richMessageWithArrangement = makeRichMessage(message) + appendedPaymentPlanRequest(paymentPlanRequest)
      level match {
        case Debug => log.debug(richMessageWithArrangement)
        case Info => log.info(richMessageWithArrangement)
        case Warn => log.warn(richMessageWithArrangement)
        case Error => log.error(richMessageWithArrangement)
      }
    }
  }
}

