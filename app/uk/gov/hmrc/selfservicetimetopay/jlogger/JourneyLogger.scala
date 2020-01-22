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

package uk.gov.hmrc.selfservicetimetopay.jlogger

import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.selfservicetimetopay.models.{SelfAssessment, TTPArrangement, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

object JourneyLogger {

  private val logger = Logger("journey-logger")
  def info(sessionId: SessionId, message: => String, data: JsValue): Unit = logger.info(s"$message [sessionId=${sessionId.value}]\n${Json.prettyPrint(data)}")
  def info(message: => String, data: JsValue)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(hc.sessionId.getOrElse(SessionId("NoSessionId")), message, data)
  def info(message: => String, tTPSubmission: TTPSubmission)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(tTPSubmission.obfuscate))
  def info(message: => String, selfAssessment: SelfAssessment)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(selfAssessment.obfuscate))
  def info(message: => String, tTPSubmission: Option[TTPSubmission] = None)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(tTPSubmission.map(_.obfuscate)))
  def info(message: => String, arrangement: TTPArrangement)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(arrangement.obfuscate))
}
