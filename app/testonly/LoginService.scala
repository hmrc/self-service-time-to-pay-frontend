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

package testonly

import com.google.inject.Inject
import play.api.http.HeaderNames
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import play.api.mvc.{Request, Session}
import times.ClockProvider
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID.randomUUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoginService @Inject() (httpClient: HttpClientV2, servicesConfig: ServicesConfig, clockProvider: ClockProvider)
  (implicit ec: ExecutionContext) {

  def logIn(tu: TestUser)(implicit request: Request[_]): Future[Session] = for {
    at <- callAuthLoginApi(tu)
  } yield buildSession(at)

  private def buildSession(authToken: AuthToken)
    (implicit request: Request[_]) =
    Session(Map(
      SessionKeys.sessionId -> s"session-$randomUUID",
      SessionKeys.authToken -> authToken.v,
      SessionKeys.lastRequestTimestamp -> clockProvider.getClock.millis().toString))

  private lazy val authLoginApiUrl = servicesConfig.baseUrl("auth-login-api")

  private def callAuthLoginApi(tu: TestUser): Future[AuthToken] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val requestBody = loginJson(tu)
    httpClient.post(url"$authLoginApiUrl/government-gateway/session/login")
      .withBody(Json.toJson(requestBody))
      .execute[HttpResponse]
      .map(r =>
        AuthToken(
          r.header(HeaderNames.AUTHORIZATION).getOrElse(throw new RuntimeException(s"missing 'AUTHORIZATION' header: $r"))
        )
      )
  }

  private def loginJson(tu: TestUser): JsObject = {
    val saEnrolment = Json.obj(
      "key" -> "IR-SA",
      "identifiers" -> Json.arr(
        Json.obj(
          "key" -> "UTR",
          "value" -> tu.utr.v
        )
      ),
      "state" -> "Activated"
    )

    val enrolments: JsArray = if (tu.hasSAEnrolment) Json.arr(saEnrolment) else Json.arr()
    val credId: String = if (tu.hasExistingDirectDebit) "192837465" else tu.authorityId.v

    val maybeNino = tu.nino.fold(Json.obj())(v => Json.obj("nino" -> v.v))

    Json.obj(
      "credId" -> credId,
      "affinityGroup" -> tu.affinityGroup.v,
      "confidenceLevel" -> tu.confidenceLevel,
      "credentialStrength" -> "strong",
      "credentialRole" -> "User",
      "usersName" -> JsNull,
      "enrolments" -> enrolments,
      "delegatedEnrolments" -> Json.arr(),
      "email" -> "user@test.com",
      "gatewayInformation" -> Json.obj()
    ) ++ maybeNino
  }

}
