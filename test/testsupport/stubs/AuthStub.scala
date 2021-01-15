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
import play.api.libs.json._
import testsupport.WireMockSupport
import testsupport.testdata.TdAll
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier}

object AuthStub extends Matchers {

  def unathorisedMissingSession(): StubMapping = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader(
              "WWW-Authenticate",
              """MDTP detail="MissingBearerToken""""
            )
            .withBody("")))
  }

  /**
   * Defines response for POST /auth/authorise
   */
  def authorise(
      utr:             Option[SaUtr]           = Some(TdAll.saUtr),
      confidenceLevel: Option[ConfidenceLevel] = Some(ConfidenceLevel.L200),
      allEnrolments:   Option[Set[Enrolment]]  = Some(Set(TdAll.saEnrolment))
  ): StubMapping = {

    implicit val enrolmentFormat: OFormat[Enrolment] = {
      implicit val f = Json.format[EnrolmentIdentifier]
      Json.format[Enrolment]
    }

    val confidenceLevelJsonPart: JsObject = confidenceLevel.map(confidenceLevel =>
      Json.obj("confidenceLevel" -> confidenceLevel)
    ).getOrElse(Json.obj())

    val saUtrJsonPart: JsObject = utr.map(utr =>
      Json.obj("saUtr" -> utr)
    ).getOrElse(Json.obj())

    val optionalCredentialsPart = Json.obj(
      "optionalCredentials" -> Json.obj(
        "providerId" -> "authId-999",
        "providerType" -> "GovernmentGateway"
      )
    )
    val enrolments: Set[Enrolment] = allEnrolments.getOrElse(Set())
    val allEnrolmentsJsonPart: JsObject = Json.obj(
      "allEnrolments" -> enrolments
    )

    val authoriseJsonBody = allEnrolmentsJsonPart ++ confidenceLevelJsonPart ++ saUtrJsonPart ++ optionalCredentialsPart

    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        //        .withRequestBody() //TODO make constraint on body
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
