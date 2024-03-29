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

import bars.model.ValidateBankDetailsResponse
import journey.Journey
import play.api.mvc.Request
import req.RequestSupport._
import ssttparrangement.SubmissionError
import ssttpcalculator.CalculatorService
import ssttpcalculator.model.PaymentSchedule
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, TypeOfAccountDetails}
import util.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton()
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) extends Logging {

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
    val event = DataEventFactory.manualAffordabilityCheckEvent(journey, failsLeftOverIncomeValidation = true)
    sendEvent(event)
  }

  def sendPlanFailsNDDSValidationEvent(journey: Journey)(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.manualAffordabilityCheckEvent(journey, failsNDDSValidation = true)
    sendEvent(event)
  }

  def sendManualAffordabilityCheckPassEvent(journey: Journey)(implicit request: Request[_]): Unit = {
    val event = DataEventFactory.manualAffordabilityCheckEvent(journey)
    sendEvent(event)
  }

  def sendPlanSetUpSuccessEvent(
      journey:           Journey,
      schedule:          PaymentSchedule,
      calculatorService: CalculatorService
  )(implicit request: Request[_]): Unit = {
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

  def sendEligibilityNotEnrolledEvent(credentials: Option[Credentials])(implicit request: Request[_]): Unit =
    sendEvent(DataEventFactory.eligibilityNotEnrolledEvent(credentials))

  def sendEligibilityInactiveEnrolmentEvent(saUtr:       Option[SaUtr],
                                            credentials: Option[Credentials]
  )(implicit request: Request[_]): Unit =
    sendEvent(DataEventFactory.eligibilityInactiveEnrolmentEvent(saUtr, credentials))

  def sendEligibilityResultEvent(eligibilityStatus: EligibilityStatus,
                                 totalDebt:         BigDecimal,
                                 saUtr:             SaUtr,
                                 credentials:       Option[Credentials]
  )(implicit request: Request[_]): Unit =
    sendEvent(DataEventFactory.eligibilityResultEvent(eligibilityStatus, totalDebt, saUtr, credentials))

  private def sendEvent(event: ExtendedDataEvent)(implicit request: Request[_]): Unit = {
    val checkEventResult = auditConnector.sendExtendedEvent(event)
    checkEventResult.onComplete {
      case Success(value)       => auditLogger.info(s"Send audit event outcome: audit event ${event.auditType} successfully posted - ${value.toString}")
      case Failure(NonFatal(e)) => auditLogger.warn(s"Send audit event outcome: unable to post audit event of type ${event.auditType} to audit connector", e)
      case _                    => auditLogger.info(s"Send audit event outcome: Event audited ${event.auditType}")
    }
  }
}
