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

package playsession

import java.time.{Clock, LocalDate, LocalDateTime, LocalTime, ZoneId, ZoneOffset}

import journey.JourneyId
import play.api.libs.json.Format
import play.api.mvc.{Request, RequestHeader, Result}

object PlaySessionSupport {
  private val journeyIdKey = "ssttp.journeyId"
  private val frozenDateTimeKey: String = "ssttp.frozenDateTime"

  implicit class ResultOps(result: Result)(implicit request: RequestHeader) {

    def placeInSession(journeyId: JourneyId): Result = result.addingToSession(journeyIdKey -> journeyId.toHexString)

    def placeInSessionIfPresent(frozenDateTime: Option[LocalDateTime]): Result = {
      frozenDateTime
        .map(frozenDateTime => result.addingToSession(frozenDateTimeKey -> frozenDateTime.toString))
        .getOrElse(result)
    }
  }

  implicit class RequestOps(request: Request[_]) {

    def readJourneyId: Option[JourneyId] = request.session.get(journeyIdKey).map(JourneyId.apply)

    def readFrozenClock(): Option[Clock] = request.session.get(frozenDateTimeKey).map(toFrozenClock)

    private def toFrozenClock(frozenDateTimeString: String) = {
      val fixedInstant = LocalDateTime.parse(frozenDateTimeString).toInstant(ZoneOffset.UTC)
      Clock.fixed(fixedInstant, ZoneId.systemDefault)
    }
  }
}
