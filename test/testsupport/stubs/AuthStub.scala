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

package testsupport.stubs

import java.time.Instant

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.Matchers
import play.api.http.HeaderNames
import play.api.mvc.{Cookie, Cookies, Session}
import testsupport.WireMockSupport
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.CryptoImplicits

object AuthStub extends Matchers {

  def unathorisedMissingSession(): StubMapping = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(
              "WWW-Authenticate", """MDTP detail="MissingBearerToken""""
            )
            .withBody(
              s"""{}""")))
  }

  def serviceIsAvailable(): StubMapping = {
    stubFor(
      get(urlPathEqualTo("/auth/authority"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
       {
         "uri": "http://wat.wat",
         "userDetailsLink": "http://localhost:${WireMockSupport.port}/user-details"
       }
       """)))

    stubFor(
      get(urlPathEqualTo("/user-details"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
       {
         "name": "Bob McBobson"
       }
       """)))
  }

  val createSessionPath = "/create-test-session"

  def createSessionResponse(sessionId: String, authToken: Option[String], crypto: uk.gov.hmrc.crypto.Encrypter): StubMapping = {
    //Implementation based on CookieCryptoFilter trait and auth-login-stub project
    val session = Session(
      Map(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.lastRequestTimestamp -> Instant.now().toEpochMilli.toString) ++
        authToken.map(token => SessionKeys.authToken -> token))
    val rawCookie: Cookie = Session.encodeAsCookie(session)
    val cookie: Cookie = new CryptoImplicits {
      val cookie: Cookie = rawCookie.copy(value = crypto.encrypt(rawCookie.value))
    }.cookie
    val headerValue: String = Cookies.encodeSetCookieHeader(List(cookie))

    stubFor(
      get(urlPathEqualTo(createSessionPath))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("A session has been created." + authToken.map(_ => " User has been logged in."))
            .withHeader(HeaderNames.SET_COOKIE, headerValue)))
  }
}
