/*
 * Copyright 2021 HM Revenue & Customs
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

import journey.Journey
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import timetopaytaxpayer.cor.model.SelfAssessmentDetails
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.selfservicetimetopay.models.TTPArrangement

object JourneyLogger {

  private val logger = Logger("journey-logger")
  def info(sessionId: SessionId, message: => String, data: JsValue): Unit = logger.info(s"$message [sessionId=${sessionId.value}]\n${Json.prettyPrint(data)}")
  def info(message: => String, data: JsValue)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(hc.sessionId.getOrElse(SessionId("NoSessionId")), message, data)
  def info(message: => String, journey: Journey)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(journey.obfuscate))
  def info(message: => String, selfAssessment: SelfAssessmentDetails)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(selfAssessment.obfuscate))
  def info(message: => String, journey: Option[Journey] = None)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(journey.map(_.obfuscate)))
  def info(message: => String, arrangement: TTPArrangement)(implicit hc: HeaderCarrier): Unit = JourneyLogger.info(message, Json.toJson(arrangement.obfuscate))

  def error(sessionId: SessionId, message: => String, data: JsValue): Unit = logger.error(s"$message [sessionId=${sessionId.value}]\n${Json.prettyPrint(data)}")
  def error(message: => String, ex: Throwable)(implicit hc: HeaderCarrier): Unit = logger.error(s"$message [sessionId=${hc.sessionId.getOrElse(SessionId("NoSessionId"))}]", ex)
  def error(message: => String, journey: Journey)(implicit hc: HeaderCarrier): Unit = error(hc.sessionId.getOrElse(SessionId("NoSessionId")), message, Json.toJson(journey.obfuscate))
}
