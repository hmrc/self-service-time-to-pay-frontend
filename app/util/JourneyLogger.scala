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

package util

import play.api.{Logger => PlayLogger}
import play.api.mvc.RequestHeader
import journey.Journey

/**
 * Journey Logger is a contextual logger. It will append to the message some extra bits of information
 * like journeyId origin, path method, etc.
 * Use it everywhere
 */
class JourneyLogger(inClass: Class[_])
  extends BaseLogger(inClass)
  with DataLogging.ArrangementLogging
  with InterestRateLogger {

  override val log: play.api.Logger = PlayLogger("journey")

  def debug(message: => String)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, journey, Debug)
  def info(message: => String)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, journey, Info)
  def warn(message: => String)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, journey, Warn)
  def error(message: => String)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, journey, Error)

  def debug(message: => String, ex: Throwable)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, ex, Debug)
  def info(message: => String, ex: Throwable)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, ex, Info)
  def warn(message: => String, ex: Throwable)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, ex, Warn)
  def error(message: => String, ex: Throwable)(implicit request: RequestHeader, journey: Journey): Unit = logMessage(message, ex, Error)

  private def journeyId(journey: Journey): String = s"[journeyId: ${journey.id.value}]"

  private def journeyStatus(journey: Journey): String = s"[journeyStatus: ${journey.status}]"

  private def appendedJourneyReference(journey: Journey): String = journeyId(journey) + " " + journeyStatus(journey)

  private def logMessage(message: => String, journey: Journey, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessageWithJourney = makeRichMessage(message) + appendedJourneyReference(journey)
    level match {
      case Debug => log.debug(richMessageWithJourney)
      case Info  => log.info(richMessageWithJourney)
      case Warn  => log.warn(richMessageWithJourney)
      case Error => log.error(richMessageWithJourney)
    }
  }

}

