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

import langswitch.Languages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest

/**
 * Test Data All
 */
object TdAll {

  val email = "sau@hotmail.com"

  val authToken = "authorization-value"
  val akamaiReputatinoValue = "akamai-reputation-value"
  val requestId = "request-id-value"
  val sessionId = "TestSession-4b87460d-6f43-4c4c-b810-d6f87c774854"
  val trueClientIp = "client-ip"
  val trueClientPort = "client-port"
  val deviceId = "device-id"

  import TdRequest._

  val request = FakeRequest()
    .withSessionId()
    .withLangEnglish()
    .withAuthToken()
    .withAkamaiReputationHeader()
    .withRequestId()
    .withSessionId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

}
