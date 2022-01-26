/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import req.RequestSupport
import ssttparrangement.SubmissionError
import ssttpcalculator.model.PaymentSchedule
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import req.RequestSupport._
import uk.gov.hmrc.http.HeaderCarrier

@Singleton()
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) {

  def sendDirectDebitSubmissionFailedEvent(
      journey:         Journey,
      schedule:        PaymentSchedule,
      submissionError: SubmissionError)(implicit request: Request[_]): Unit = {
    val event = makeEvent(
      journey,
      schedule,
      Json.obj(
        "status" -> "failed to submit direct debit didnt bothered to submit TTP Arrangement",
        "submissionError" -> submissionError
      ))
    auditConnector.sendExtendedEvent(event)
    ()
  }

  private def makeEvent(
      journey:   Journey,
      schedule:  PaymentSchedule,
      extraInfo: JsObject)(implicit request: Request[_]) = ExtendedDataEvent(
    auditSource = "pay-what-you-owe",
    //`directDebitSetup` was provided at the beginning.
    // I'm not changing it to make splunk events consistent with what we already have stored
    auditType = "directDebitSetup",
    tags      = AuditService.auditTags,
    detail    = extraInfo ++ makeDetails(journey, schedule)
  )

  private def makeDetails(journey: Journey, schedule: PaymentSchedule) = Json.obj(
    "utr" -> journey.taxpayer.selfAssessment.utr.value,
    "bankDetails" -> Json.obj(
      "name" -> journey.bankDetails.accountName,
      "accountNumber" -> journey.bankDetails.accountNumber,
      "sortCode" -> journey.bankDetails.sortCode
    ),
    "schedule" -> Json.obj(
      "initialPaymentAmount" -> schedule.initialPayment,
      "installments" -> Json.toJson(schedule.instalments.sortBy(_.paymentDate.toEpochDay)),
      "numberOfInstallments" -> schedule.instalments.length,
      "installmentLengthCalendarMonths" -> (ChronoUnit.MONTHS.between(schedule.startDate, schedule.endDate)),
      "totalPaymentWithoutInterest" -> schedule.amountToPay,
      "totalInterestCharged" -> schedule.totalInterestCharged,
      "totalPayable" -> schedule.totalPayable)
  )

}

object AuditService {
  def auditTags(implicit request: Request[_]): Map[String, String] = {

    val hc: HeaderCarrier = RequestSupport.hc(request)
    Map(
      "Akamai-Reputation" -> hc.akamaiReputation.map(_.value).getOrElse("-"),
      "X-Request-ID" -> hc.requestId.map(_.value).getOrElse("-"),
      "X-Session-ID" -> hc.sessionId.map(_.value).getOrElse("-"),
      "clientIP" -> hc.trueClientIp.getOrElse("-"),
      "clientPort" -> hc.trueClientPort.getOrElse("-"),
      "path" -> request.path,
      "deviceID" -> hc.deviceID.getOrElse("-")
    )
  }
}
