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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.Matchers
import play.api.libs.json.Json
import testsupport.WireMockSupport
import testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment}
import uk.gov.hmrc.selfservicetimetopay.models.Taxpayer

object TaxpayerStub extends Matchers {

  def getTaxpayer(utr: String = TdAll.utr, returnedTaxpayer: Taxpayer = TdAll.taxpayer): StubMapping = {
    import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

    stubFor(
      post(urlPathEqualTo(s"/taxpayer/$utr"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              Json.prettyPrint(Json.toJson(returnedTaxpayer)))))
  }

  def athorised(
      utr:             String          = TdAll.utr,
      confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
      enrolments:      List[Enrolment] = Nil //TODO List(TdAll.saEnrolment)
  ): StubMapping = {

    val authoriseJsonBody = Json.obj(
      "allEnrolments" -> enrolments,
      "confidenceLevel" -> confidenceLevel,
      "saUtr" -> utr
    )

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              Json.prettyPrint(authoriseJsonBody)
            )))
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

}
