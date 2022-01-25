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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json.prettyPrint
import play.api.libs.json.{JsObject, Json}
import testsupport.JsonSyntax._
import testsupport.WireMockSupport
import testsupport.stubs.DirectDebitStub.OK
import timetopaytaxpayer.cor.model.SaUtr

/**
 * A stub representing an endpoint in add-taxes-frontend which we
 * call when sending user if he misses required enrolments.
 */
object AddTaxesFeStub extends Matchers {

  def enrolForSaStub(maybeSaUtr: Option[SaUtr]): StubMapping = {
    val bodyJson = Json.obj(
      "origin" -> "ssttp-sa",
      "providerId" -> "authId-999"
    ) ++ maybeSaUtr.fold(Json.obj())(utr => Json.obj("utr" -> utr))

    stubFor(
      post(urlPathEqualTo(s"/internal/self-assessment/enrol-for-sa"))
        .withRequestBody(equalToJson(Json.prettyPrint(bodyJson)))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(prettyPrint(responseBody))
        )
    )
  }

  private val responseBody: JsObject = {
    //language = JSON
    s"""
      {
        "redirectUrl": "http://localhost:${WireMockSupport.port}$enrolForSaPagePath"
      }
    """.asJson
  }
  lazy val enrolForSaPagePath: String = "/enrol-for-sa-path"

  def enrolForSaStubbedPage(): StubMapping = {
    stubFor(
      get(urlPathEqualTo(enrolForSaPagePath))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
                    <html>
                      <head/>
                      <body>
                        You can set up for SA Enrolment here ...
                      </body>
                    </html>
                """)))
  }

}

