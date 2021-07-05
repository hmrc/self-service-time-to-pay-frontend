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

package pagespecs

import play.api.libs.json.{JsValue, Json}
import testsupport.stubs.AuthStub
import testsupport.{ItSpec, WireMockSupport}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient, HttpResponse, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global

class AuthStubDebuggerSpec extends ItSpec {

  /**
   * This one can be handy when
   * verifying if authStub is
   * returning the same json
   * as real auth microservice.
   */
  "debugging auth stub" ignore {

    implicit val hc = HeaderCarrier(
      authorization = Some(Authorization(
        "Bearer BXQ3/Treo4kQCZvVcCqKPqSU1yLyn1ZRvC5Sp3km0lcaOW24OzCQ8kES1Q7JuVdfmkAVmfHYjkEE0z1hww06q2Rv98l/kLpxUpDu5wWN6rYmga9+Gtguu7xG5YiwkTDVZwgJk8ivo45ODylpIgnQDRAt9rEAgjhFKQcS5xpq+v39KwIkeIPK/mMlBESjue4V"
      )),
      sessionId     = Some(SessionId("whatever"))
    )

    val inputJson = Json.parse("""{"authorise":[],"retrieve":["allEnrolments","confidenceLevel","saUtr"]}""")
    val http = app.injector.instanceOf[HttpClient]

    val realAuthResponse = {
      val serviceUrl = "http://localhost:8500"
      val r = http.POST[JsValue, HttpResponse](s"$serviceUrl/auth/authorise", inputJson).futureValue
      r.json
    }

    val authStubResponse = {
      AuthStub.authorise()
      val serviceUrl = s"http://localhost:${WireMockSupport.port}"
      val r = http.POST[JsValue, HttpResponse](s"$serviceUrl/auth/authorise", inputJson).futureValue
      r.json
    }

    authStubResponse shouldBe realAuthResponse withClue "USE http://jsondiff.com/ to compare"
  }

}
