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

package testsupport.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import testsupport.testdata.ArrangementTd

object ArrangementStub extends Matchers {

  def arrangementSubmit: StubMapping =
    {
      stubFor(
        post(urlPathEqualTo("/pay-what-you-owe-in-instalments/arrangement/submit"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(""))
      )
    }

  def postTtpArrangement: StubMapping =
    {
      stubFor(
        post(urlPathEqualTo("/ttparrangements"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody(Json.prettyPrint(ArrangementTd.tTPArrangementJson))
          )
      )
    }
}
