/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.service

import javax.inject.Singleton

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.selfservicetimetopay.config.FrontendAuditConnector
import uk.gov.hmrc.selfservicetimetopay.models.{CalculatorPaymentScheduleInstalment, DirectDebitInstructionPaymentPlan, TTPSubmission}

import scala.concurrent.ExecutionContext

@Singleton()
class AuditService {

  def auditConnector = FrontendAuditConnector

  def sendSubmissionEvent(submission: TTPSubmission)(implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext) = {
    auditConnector.sendExtendedEvent(eventFor(submission))
  }

  private def eventFor(submission: TTPSubmission)(implicit hc: HeaderCarrier) = {

    //todo Find out whether the bank account check needs to be in the event, if they get this far this should be a successful check
    //at this stage all optional values should be present
    val utr: String = submission.taxpayer.get.selfAssessment.get.utr.get
    val name: String = submission.bankDetails.map(_.accountName.get).get
    val accountNumber: String = submission.bankDetails.map(_.accountNumber.get).get
    val sortCode: String = submission.bankDetails.map(_.sortCode.get).get
    val installment: String = submission.schedule.map(_.instalments).getClass.toString
    val interestTotal: BigDecimal = submission.schedule.map(_.totalInterestCharged).get
    val total: BigDecimal = submission.schedule.map(_.totalPayable).get

   ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType = "directDebitSetup",
      tags = hc.headers.toMap,
      detail = Json.obj(
        "utr" -> utr,
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
