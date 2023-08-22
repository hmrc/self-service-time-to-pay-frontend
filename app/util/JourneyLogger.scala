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

import play.api.Logger
import play.api.mvc.RequestHeader
import journey.Journey
import play.api.libs.json.Json

/**
 * Journey Logger is a contextual logger. It will append to the message some extra bits of information
 * like journeyId origin, path method, etc.
 * Use it everywhere
 */
class JourneyLogger(inClass: Class[_])
  extends BaseLogger(inClass)
  with ArrangementLogging {

  override val log: Logger = Logger("journey")

  def debug(message: => String, journey: Journey)(implicit request: RequestHeader): Unit = logMessage(message, journey, Debug)

  def info(message: => String, journey: Journey)(implicit request: RequestHeader): Unit = logMessage(message, journey, Info)

  def warn(message: => String, journey: Journey)(implicit request: RequestHeader): Unit = logMessage(message, journey, Warn)

  def error(message: => String, journey: Journey)(implicit request: RequestHeader): Unit = logMessage(message, journey, Error)

  private def appendedJourney(journey: Journey): String = s" [journey: ${appendedData(Json.toJson(journey.obfuscate))}]"

  private def logMessage(message: => String, journey: Journey, level: LogLevel)(implicit request: RequestHeader): Unit = {
    lazy val richMessageWithJourney = makeRichMessage(message) + appendedJourney(journey)
    level match {
      case Debug => log.debug(richMessageWithJourney)
      case Info  => log.info(richMessageWithJourney)
      case Warn  => log.warn(richMessageWithJourney)
      case Error => log.error(richMessageWithJourney)
    }
  }

}

