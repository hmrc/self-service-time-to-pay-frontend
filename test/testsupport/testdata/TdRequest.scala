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

package testsupport.testdata

import testsupport.Language
import testsupport.Language.{English, Welsh}
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

object TdRequest {
  val authToken = "authorization-value"
  val akamaiReputationValue = "akamai-reputation-value"
  val requestId = "request-id-value"
  val sessionId = "TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854"
  val trueClientIp = "client-ip"
  val trueClientPort = "client-port"
  val deviceId = "device-id"
  val rawSessionId: String = "TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854"

  val requestMethod: String = "GET"
  val requestPath: String = "/fake-path"
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(requestMethod, requestPath)
    .withSessionId()
    .withAuthToken()
    .withAkamaiReputationHeader()
    .withRequestId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  implicit class FakeRequestOps[T](r: FakeRequest[T]) {
    def withLang(lang: Language = English): FakeRequest[T] = {
      val code = lang match {
        case Language.English => "EN"
        case Language.Welsh   => "CY"
      }

      r.withCookies(Cookie("PLAY_LANG", code))
    }

    def withLangWelsh(): FakeRequest[T] = r.withLang(Welsh)
    def withLangEnglish(): FakeRequest[T] = r.withLang(English)

    def withAuthToken(authToken: String = authToken): FakeRequest[T] = r.withSession((SessionKeys.authToken, authToken))

    def withAkamaiReputationHeader(akamaiReputatinoValue: String = akamaiReputationValue): FakeRequest[T] =
      r.withHeaders(HeaderNames.akamaiReputation -> akamaiReputatinoValue)

    def withRequestId(requestId: String = requestId): FakeRequest[T] = r.withHeaders(HeaderNames.xRequestId -> requestId)

    def withSessionId(sessionId: String = sessionId): FakeRequest[T] = r.withSession(SessionKeys.sessionId -> sessionId)

    def withTrueClientIp(ip: String = trueClientIp): FakeRequest[T] = r.withHeaders(HeaderNames.trueClientIp -> ip)

    def withTrueClientPort(port: String = trueClientPort): FakeRequest[T] = r.withHeaders(HeaderNames.trueClientPort -> port)

    def withDeviceId(deviceId: String = deviceId): FakeRequest[T] = r.withHeaders(HeaderNames.deviceID -> deviceId)
  }
}
