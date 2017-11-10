/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.testonly

import java.util.UUID
import javax.inject.Singleton

import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.mvc.Session
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import play.api.libs.json.{Json, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoginService extends ServicesConfig {

  def logIn(tu: TestUser)(implicit executionContext: ExecutionContext): Future[Session] = for {
    (at, au, gt) <- callAuthLoginApi(tu)
  } yield buildSession(au, at, gt, tu)


  private def buildSession(
    authorityUri: AuthorityUri,
    authToken: AuthToken,
    gatewayToken: GatewayToken,
    tu: TestUser
  ) = {
    Session(Map(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
      SessionKeys.authProvider -> "GGW",
      SessionKeys.userId -> authorityUri.v,
      SessionKeys.authToken -> authToken.v,
      SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString,
      SessionKeys.token -> gatewayToken.v,
      SessionKeys.affinityGroup -> tu.affinityGroup.v,
      SessionKeys.name -> tu.authorityId.v
    ))
  }

  private lazy val authLoginApiUrl = baseUrl("auth-login-api")

  private def callAuthLoginApi(tu: TestUser)(implicit executionContext: ExecutionContext): Future[(AuthToken, AuthorityUri, GatewayToken)] = {
    implicit val hc = HeaderCarrier()

    val requestBody = loginJson(tu)
    WSHttp.POST(s"$authLoginApiUrl/government-gateway/legacy/login", requestBody).map(r=>
      (
        AuthToken(
          r.header(HeaderNames.AUTHORIZATION).getOrElse(throw new RuntimeException(s"missing 'AUTHORIZATION' header: $r"))
        ),
        AuthorityUri(
          r.header(HeaderNames.LOCATION).getOrElse(throw new RuntimeException(s"missing 'LOCATION' header: $r"))
        ),
        GatewayToken(
          (r.json \ "gatewayToken").asOpt[String].getOrElse(throw new RuntimeException(s"missing 'gatewayToken': ${r.body}"))
        )
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

    val enrolments = if(tu.hasSAEnrolment) Json.arr(saEnrolment) else Json.arr()

    Json.obj(

    "credId" -> tu.authorityId.v,
    "affinityGroup" -> tu.affinityGroup.v,
    "confidenceLevel" -> tu.confidenceLevel,
    "credentialStrength" -> "weak",
    "credentialRole" -> "User",
    "usersName" -> JsNull,
    "enrolments" -> enrolments,
    "delegatedEnrolments" -> Json.arr(),
    "email" -> "user@test.com",
    "gatewayInformation" -> Json.obj()
  )}

  private implicit class JsonReplaceOp(j: JsObject){
    def replace(node: Symbol, newValue: String): JsObject = j.transform((__ \ node).json.put(JsString(newValue))).get
    def replace(node: Symbol, newValue: Int): JsObject = j.transform((__ \ node).json.put(JsNumber(newValue))).get
  }

}
