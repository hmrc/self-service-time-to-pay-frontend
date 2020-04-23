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

import javax.inject.{Inject, Singleton}
import journey.Journey
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import timetopaytaxpayer.cor.model.SaUtr
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

  private def eventFor(submission: Journey)(implicit request: Request[_]) = {

    //todo Find out whether the bank account check needs to be in the event, if they get this far this should be a successful check
    //at this stage all optional values should be present
    val utr: SaUtr = submission.taxpayer.selfAssessment.utr
    val name: String = submission.bankDetails.map(_.accountName.get).get
    val accountNumber: String = submission.bankDetails.map(_.accountNumber.get).get
    val sortCode: String = submission.bankDetails.map(_.sortCode.get).get
    val installment: String = submission.schedule.map(_.schedule.instalments).getClass.toString
    val interestTotal: BigDecimal = submission.schedule.map(_.schedule.totalInterestCharged).get
    val total: BigDecimal = submission.schedule.map(_.schedule.totalPayable).get

    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType   = "directDebitSetup",
      tags        = hc.headers.toMap,
      detail      = Json.obj(
        "utr" -> utr.value,
        "bankDetails" -> Json.obj(
          "name" -> name,
          "accountNumber" -> accountNumber,
          "sortCode" -> sortCode
        ),
        "installments" -> Json.obj(
          "installment" -> installment,
          "interestTotal" -> interestTotal,
          "total" -> total)
      )
    )
  }
}
