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

package testsupport.testdata

import langswitch.{Language, Languages}
import play.api.libs.json.Json
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

object TdRequest {

  implicit class FakeRequestOps[T](r: FakeRequest[T]) {

    def withLang(lang: Language = Languages.English): FakeRequest[T] = r.withCookies(
      Cookie("PLAY_LANG", lang.code))

    def withLangWelsh(): FakeRequest[T] = r.withLang(Languages.Welsh)
    def withLangEnglish(): FakeRequest[T] = r.withLang(Languages.English)

    def withAuthToken(authToken: String = TdAll.authToken): FakeRequest[T] = r.withSession((SessionKeys.authToken, authToken))

    def withAkamaiReputationHeader(
        akamaiReputatinoValue: String = TdAll.akamaiReputatinoValue): FakeRequest[T] = r.withHeaders(
      HeaderNames.akamaiReputation -> akamaiReputatinoValue)

    def withRequestId(requestId: String = TdAll.requestId): FakeRequest[T] = r.withHeaders(
      HeaderNames.xRequestId -> requestId)

    def withSessionId(sessionId: String = TdAll.sessionId): FakeRequest[T] = r.withSession(
      SessionKeys.sessionId -> sessionId)

    def withTrueClientIp(ip: String = TdAll.trueClientIp): FakeRequest[T] = r.withHeaders(
      HeaderNames.trueClientIp -> ip)

    def withTrueClientPort(port: String = TdAll.trueClientPort): FakeRequest[T] = r.withHeaders(
      HeaderNames.trueClientPort -> port)

    def withDeviceId(deviceId: String = TdAll.deviceId): FakeRequest[T] = r.withHeaders(
      HeaderNames.deviceID -> deviceId)
  }

}
