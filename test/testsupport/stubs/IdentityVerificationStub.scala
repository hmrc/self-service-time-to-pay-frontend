/*
 * Copyright 2021 HM Revenue & Customs
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

object IdentityVerificationStub extends Matchers {

  val identityVerificationPagePath: String = "/identityVerificationPath"

  def identityVerificationStubbedPage(): StubMapping = {
    stubFor(
      get(urlPathEqualTo(identityVerificationPagePath))
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

  val mdtpUpliftPagePath: String = "/mdtp/uplift"

  def mdtpUpliftStubbedPage(): StubMapping = {
    stubFor(
      get(urlPathEqualTo(mdtpUpliftPagePath))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
                    <html>
                      <head/>
                      <body>
                        MDTP uplift here ...
                      </body>
                    </html>
                """)))
  }

}

