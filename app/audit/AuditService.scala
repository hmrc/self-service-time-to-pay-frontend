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

package audit

import bars.model.{BarsResponse, ValidateBankDetailsResponse}
import com.sun.tools.javac.tree.Pretty
import config.AppConfig

import javax.inject.{Inject, Singleton}
import journey.Journey
import play.api.libs.json.Json
import play.api.libs.json.Json.prettyPrint
import play.api.mvc.Request
import ssttparrangement.SubmissionError
import ssttpcalculator.model.PaymentSchedule
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import req.RequestSupport._
import ssttpcalculator.legacy.CalculatorService
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.selfservicetimetopay.models.TypeOfAccountDetails
import util.Logging

import scala.util.{Failure, Success}

@Singleton()
class AuditService @Inject() (
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext) extends Logging {

  def sendDirectDebitSubmissionFailedEvent(
      journey:         Journey,
      schedule:        PaymentSchedule,
      submissionError: SubmissionError
  )(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.directDebitSubmissionFailedEvent(journey, schedule, submissionError)
    sendEvent(event)
    ()
  }

  def sendPlanNotAffordableEvent(journey: Journey)(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.planNotAvailableEvent(journey)
    sendEvent(event)
  }

  def sendPlanFailsNDDSValidationEvent(journey: Journey)(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.planNotAvailableEvent(journey, failsNDDSValidation = true)
    sendEvent(event)
  }

  def sendPlanSetUpSuccessEvent(
      journey:           Journey,
      schedule:          PaymentSchedule,
      calculatorService: CalculatorService
  )(implicit request: Request[_], appConfig: AppConfig): Unit = {
    val event = DataEventFactory.planSetUpEvent(journey, schedule, calculatorService)
    sendEvent(event)
  }

  def sendBarsValidateEvent(
      sortCode:                  String,
      accountNumber:             String,
      accountName:               String,
      maybeTypeOfAccountDetails: Option[TypeOfAccountDetails],
      saUtr:                     SaUtr,
      barsResp:                  ValidateBankDetailsResponse
  )(implicit request: Request[_]): Unit = {

    val event: ExtendedDataEvent = DataEventFactory.barsValidateEvent(
      sortCode                  = sortCode,
      accountNumber             = accountNumber,
      accountName               = accountName,
      maybeTypeOfAccountDetails = maybeTypeOfAccountDetails,
      saUtr                     = saUtr,
      barsResp                  = barsResp
    )
    sendEvent(event)
  }

  private def sendEvent(event: ExtendedDataEvent)(implicit request: Request[_]): Unit = {
    val checkEventResult = auditConnector.sendExtendedEvent(event)
    checkEventResult.onComplete {
      case Success(value)       => auditLogger.info(s"Send audit event outcome: audit event ${event.auditType} successfully posted - ${value.toString}")
      case Failure(NonFatal(e)) => auditLogger.warn(s"Send audit event outcome: unable to post audit event of type ${event.auditType} to audit connector", e)
      case _                    => auditLogger.info(s"Send audit event outcome: Event audited ${event.auditType}")
    }
  }
}
