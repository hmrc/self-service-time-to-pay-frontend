/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.temporal.ChronoUnit

import javax.inject.{Inject, Singleton}
import journey.Journey
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton()
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  import req.RequestSupport._

  def sendSubmissionEvent(submission: Journey)(implicit request: Request[_]): Future[Unit] = {
    JourneyLogger.info(s"Sending audit event for successful submission")

    val event = eventFor(submission)
    val result = auditConnector.sendExtendedEvent(event)
    result.onFailure {
      case NonFatal(e) => Logger.error(s"Unable to post audit event of type [${event.auditType}] to audit connector - [${e.getMessage}]", e)
    }

    result.map(_ => ())
  }

  private def eventFor(journey: Journey)(implicit request: Request[_]) =
    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "directDebitSetup",
      tags        = hc.headers.toMap,
      detail      = Json.obj(
        "utr" -> journey.taxpayer.selfAssessment.utr.value,
        "bankDetails" -> Json.obj(
          "name" -> journey.bankDetails.accountName,
          "accountNumber" -> journey.bankDetails.accountNumber,
          "sortCode" -> journey.bankDetails.sortCode
        ),
        "schedule" -> Json.obj(
          "initialPaymentAmount" -> journey.schedule.initialPayment,
          "installments" -> Json.toJson(journey.schedule.instalments.sortBy(_.paymentDate.toEpochDay)),
          "numberOfInstallments" -> journey.schedule.instalments.length,
          "installmentLengthCalendarDays" -> (ChronoUnit.DAYS.between(journey.schedule.startDate, journey.schedule.endDate) + 1),
          "totalPaymentWithoutInterest" -> journey.schedule.amountToPay,
          "totalInterestCharged" -> journey.schedule.totalInterestCharged,
          "totalPayable" -> journey.schedule.totalPayable)
      )
    )
}
