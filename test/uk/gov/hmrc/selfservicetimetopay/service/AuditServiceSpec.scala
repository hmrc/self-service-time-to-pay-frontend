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

import audit.AuditService
import org.scalatest.concurrent.Eventually
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.controllers.PlayMessagesSpec
import uk.gov.hmrc.selfservicetimetopay.resources._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import testsupport.testdata.TdAll
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
class AuditServiceSpec extends PlayMessagesSpec with Eventually with ScalaFutures with MockitoSugar {

  val AuditService = new AuditService(
    mock[AuditConnector]
  )

  implicit val headerCarrier = HeaderCarrier()

  "AuditService" should {
    "send an audit " in {

      implicit val request = TdAll.request

      AuditService.sendSubmissionEvent(ttpSubmissionWithBankDetails).futureValue
    }
  }
}
