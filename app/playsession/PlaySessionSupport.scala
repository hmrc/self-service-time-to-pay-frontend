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

package playsession

import journey.JourneyId
import play.api.libs.json.Format
import play.api.mvc.{Request, RequestHeader, Result}

object PlaySessionSupport {
  private val journeyIdKey = "journeyId"

  implicit class ResultOps(result: Result)(implicit request: RequestHeader) {

    def placeInSession(journeyId: JourneyId): Result = result.withSession(journeyIdKey -> journeyId.value)

    def removeJourneyIdFromSession: Result = result.withSession(result.session - journeyIdKey)
  }

  implicit class RequestOps(request: Request[_]) {

    def readJourneyId: JourneyId = request
      .session
      .get(journeyIdKey)
      .map(JourneyId.apply)
      .getOrElse(throw new RuntimeException(s"'$journeyIdKey' Not found in the play session"))

  }
}
