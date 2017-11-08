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

package uk.gov.hmrc.selfservicetimetopay.controllers

import java.util.UUID
import javax.inject._

import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.mvc.Session
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.selfservicetimetopay.config.WSHttp
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.controllers.TestOnlyControllerHelper._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.Future
import scala.util.Random
import scala.util.control.NonFatal

class TestOnlyController @Inject()(val messagesApi: play.api.i18n.MessagesApi, ddConnector: DirectDebitConnector,
                                   calculatorConnector: CalculatorConnector,
                                   taxPayerConnector: TaxPayerConnector,
                                   eligibilityConnector: EligibilityConnector
                                  )
extends TimeToPayController with play.api.i18n.I18nSupport with ServicesConfig {

  def quickLogIn() = Action.async { implicit request =>
    val ag = AffinityGroup.Individual
    val algF = logIn(ag)
    for {
      (at, au, gt) <- algF
      newSession = buildSession(au, at, gt, ag)
    } yield redirectToSessionView.withSession(newSession)

  }

  def clearPlaySession() = Action { implicit request =>
    redirectToSessionView.withSession()
  }

  def clearTtpSessionIdCookie() = Action { implicit request =>
    redirectToSessionView.withCookies(
      sessionProvider.createTtpCookie()
    )
  }

  def inspector() = Action.async { implicit request =>

    val sessionCacheF = sessionCache.get
    val authorityF = authConnector.currentAuthority.map(_.json).recover{
      case NonFatal(e) => e.getMessage
    }

    for {
      maybeSubmission <- sessionCacheF
      authority <- authorityF

    } yield Ok(views.html.selfservicetimetopay.testonly.inspector(
        request.session.data,
        List(
          "eligibilityTypeOfTax" -> maybeSubmission.flatMap(_.eligibilityTypeOfTax).json,
          "eligibilityExistingTtp" -> maybeSubmission.flatMap(_.eligibilityExistingTtp).json,
          "debitDate" -> maybeSubmission.flatMap(_.debitDate).json,
          "taxpayer" -> maybeSubmission.flatMap(_.taxpayer).json,
          "schedule" -> maybeSubmission.flatMap(_.schedule).json,
          "bankDetails" -> maybeSubmission.flatMap(_.bankDetails).json,
          "existingDDBanks" -> maybeSubmission.flatMap(_.existingDDBanks).json,

          "calculatorData" -> maybeSubmission.map(_.calculatorData).json,
          "durationMonths" -> maybeSubmission.map(_.durationMonths).json,
          "eligibilityStatus" -> maybeSubmission.map(_.eligibilityStatus).json
        ),
        authority,
        implicitly[HeaderCarrier].headers
      ))

  }

  lazy val logIn: AffinityGroup => Future[(AuthToken, AuthorityUri, GatewayToken)] = TestOnlyControllerHelper.callAuthLoginApi(s"${baseUrl("auth-login-api")}/government-gateway/legacy/login")
  lazy val redirectToSessionView = Redirect(routes.TestOnlyController.inspector())

}


object TestOnlyControllerHelper {
  import play.api.libs.json._

  def buildSession(
                    authorityUri: AuthorityUri,
                    authToken: AuthToken,
                    gatewayToken: GatewayToken,
                    ag: AffinityGroup
                  ) = {
    Session(Map(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID}",
      SessionKeys.authProvider -> "GGW",
      SessionKeys.userId -> authorityUri.v,
      SessionKeys.authToken -> authToken.v,
      SessionKeys.lastRequestTimestamp -> DateTime.now.getMillis.toString,
      SessionKeys.token -> gatewayToken.v,
      SessionKeys.affinityGroup -> ag.v,
      SessionKeys.name -> AuthorityId.random.v
    ))
  }

  def callAuthLoginApi(authLoginApiUrl: String)(ag: AffinityGroup): Future[(AuthToken, AuthorityUri, GatewayToken)] = {
    implicit val hc = HeaderCarrier()
    import scala.concurrent.ExecutionContext.Implicits.global
    WSHttp.POST(authLoginApiUrl, loginJson(ag)).map(r=>
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

  def loginJson(ag: AffinityGroup): JsObject = Json.obj(
    "credId" -> "543212300016",
    "affinityGroup" -> ag.v,
    "confidenceLevel" -> 200,
    "credentialStrength" -> "weak",
    "credentialRole" -> "User",
    "usersName" -> JsNull,
    "enrolments" -> Json.arr(
      Json.obj(
        "key" -> "IR-SA",
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "UTR",
            "value" -> "9035906175"
          )
        ),
        "state" -> "Activated"
      ),
      Json.obj(
        "key" -> "",
        "identifiers" -> Json.arr(
          Json.obj(
            "key" -> "",
            "value" -> ""
          )),
        "state" -> "Activated"
      ),
      Json.obj(
        "key" -> "",
        "identifiers" -> Json.arr(Json.obj(
          "key" -> "",
          "value" -> ""
        )),
        "state" -> "Activated"
      ),
      Json.obj(
        "key" -> "",
        "identifiers" -> Json.arr(Json.obj(
          "key" -> "",
          "value" -> ""
        )),
        "state" -> "Activated"
      )),
    "delegatedEnrolments" -> Json.arr(),
    "email" -> "user@test.com",
    "gatewayInformation" -> Json.obj()
  )

  implicit class JsonReplaceOp(j: JsObject){
    def replace(node: Symbol, newValue: String): JsObject = j.transform((__ \ node).json.put(JsString(newValue))).get
    def replace(node: Symbol, newValue: Int): JsObject = j.transform((__ \ node).json.put(JsNumber(newValue))).get
  }

  implicit class JsonOps[A: Writes](a: A){
    def json: String = Json.prettyPrint(Json.toJson(a))
  }

}

case class AffinityGroup(v: String)
object AffinityGroup {
  val Individual = AffinityGroup("Individual")
}

/**
  * The same as CredId
  */
case class AuthorityId(v: String)
case class AuthorityUri(v: String)

object AuthorityId {
  def random = AuthorityId(s"authorityId-${Math.abs(Random.nextLong())}")
}

case class GatewayToken(v: String)

/**
  * The same as bearer token
  */
case class AuthToken(v: String)


