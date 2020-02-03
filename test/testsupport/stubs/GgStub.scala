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

import java.net.URLEncoder

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.Matchers

object GgStub extends Matchers {

  def signInPage(port: Int): StubMapping = {
    stubFor(
      get(urlPathEqualTo(signInPath))
        .withQueryParam("continue", equalTo(
          s"""http://localhost:$port/pay-what-you-owe-in-instalments/arrangement/determine-eligibility"""
        ))
        .withQueryParam("origin", equalTo(s"""pay-online"""))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              singInPageSource)))
  }

  val singInPageSource = s"""
<html>
  <head/>
  <body>
    Sign in using Government Gateway
  </body>
</html>
    """
  val signInPath = "/gg/sign-in"

}

