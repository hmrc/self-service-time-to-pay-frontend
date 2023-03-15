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

import javax.inject.{Inject, Singleton}
import journey.Journey
import play.api.mvc.Request
import ssttparrangement.SubmissionError
import ssttpcalculator.model.PaymentSchedule
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import req.RequestSupport._
import util.ApplicationLogging

import scala.util.{Failure, Success}

@Singleton()
class AuditService @Inject() (
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext) extends ApplicationLogging {

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
    val event = DataEventFactory.planNotAffordableEvent(journey)
    sendEvent(event)
  }

  def sendPlanSetUpSuccessEvent(journey: Journey, schedule: PaymentSchedule)(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.planSetUpSuccessEvent(journey, schedule)
    sendEvent(event)
  }

  private def sendEvent(event: ExtendedDataEvent)(implicit request: Request[_]): Unit = {
    val checkEventResult = auditConnector.sendExtendedEvent(event)
    checkEventResult.onComplete {
      case Failure(NonFatal(e)) ⇒ logger.error(s"Unable to post audit event of type ${event.auditType} to audit connector - ${e.getMessage}", e)
      case Success(value)       ⇒ logger.info(s"Audit event ${event.auditType} successfully posted - ${value.toString}")
      case _                    => logger.info(s"Audit event ${event.auditType} - other")
    }
  }
}
