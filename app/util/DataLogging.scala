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

package util

import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import ssttparrangement.SubmissionError
import uk.gov.hmrc.selfservicetimetopay.models.{PaymentPlanRequest, TTPArrangement}

object DataLogging {

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
        case Info  => log.info(richMessageWithArrangement)
        case Warn  => log.warn(richMessageWithArrangement)
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
        case Info  => log.info(richMessageWithArrangement)
        case Warn  => log.warn(richMessageWithArrangement)
        case Error => log.error(richMessageWithArrangement)
      }
    }
  }
}

