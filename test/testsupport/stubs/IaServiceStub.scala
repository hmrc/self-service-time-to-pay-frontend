/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatest.Matchers
import play.api.libs.json.Json
import testsupport.testdata.TdAll
//TODO can delete?
object IaServiceStub extends Matchers {

  val iaUrl: String = "http://localhost:8051"

  def checkIaUtr(isOnIa: Boolean): StubMapping = {
    if (isOnIa) {
      stubFor(
        get(urlPathEqualTo(iaUrl + s"/ia/${TdAll.utr}"))
          .willReturn(
            aResponse()
              .withStatus(200).withBody(
                Json.prettyPrint(Json.toJson(true)))))
    } else {
      stubFor(
        get(urlPathEqualTo(iaUrl + s"/ia/${TdAll.utr}"))
          .willReturn(
            aResponse()
              .withStatus(204).withBody(
                Json.prettyPrint(Json.toJson(false)))))
    }
  }
}
