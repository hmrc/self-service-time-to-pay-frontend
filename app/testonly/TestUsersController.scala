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

package testonly

import config.AppConfig
import controllers.FrontendBaseController
import javax.inject._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.i18n.I18nSupport
import req.RequestSupport
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class TestUserForm(
    utr:                          Option[String],
    returnsJson:                  String,
    returnsResponseStatusCode:    String,
    hasSAEnrolment:               Boolean,
    isOnIA:                       Boolean,
    authorityId:                  Option[String],
    affinityGroup:                String,
    hasOverTwoHundred:            Boolean,
    debitsJson:                   String,
    debitsResponseStatusCode:     String,
    saTaxpayer:                   String,
    saTaxpayerResponseStatusCode: String,
    continueUrl:                  Option[String]
) {

  def asTestUser: TestUser = TestUser(
    utr                          = utr.map(Utr.apply).getOrElse(Utr.random()),
    hasSAEnrolment               = hasSAEnrolment,
    isOnIA                       = isOnIA,
    authorityId                  = authorityId.map(AuthorityId.apply).getOrElse(AuthorityId.random),
    affinityGroup                = AffinityGroup(affinityGroup),
    confidenceLevel              = if (hasOverTwoHundred) 200 else 100,
    returns                      = Json.parse(returnsJson),
    returnsResponseStatusCode    = returnsResponseStatusCode.toInt,
    debits                       = Json.parse(debitsJson),
    debitsResponseStatusCode     = debitsResponseStatusCode.toInt,
    saTaxpayer                   = Json.parse(saTaxpayer),
    saTaxpayerResponseStatusCode = saTaxpayerResponseStatusCode.toInt,
    continueUrl                  = continueUrl
  )
}

object TestUserForm {

  val initial = TestUserForm(
    utr                          = None,
    returnsJson                  = Json.prettyPrint(TestUserReturns.sample1),
    returnsResponseStatusCode    = "200",
    hasSAEnrolment               = true,
    isOnIA                       = true,
    authorityId                  = None,
    affinityGroup                = AffinityGroup.individual.v,
    hasOverTwoHundred            = true,
    debitsJson                   = Json.prettyPrint(TestUserDebits.sample1),
    debitsResponseStatusCode     = "200",
    saTaxpayer                   = Json.prettyPrint(TestUserSaTaxpayer.buildTaxpayer()),
    saTaxpayerResponseStatusCode = "200",
    continueUrl                  = None
  )
}

class TestUsersController @Inject() (
    loginService:     LoginService,
    saStubConnector:  SaStubConnector,
    desStubConnector: DesStubConnector,
    iaConnector:      IaConnector,
    views:            Views,
    cc:               MessagesControllerComponents,
    requestSupport:   RequestSupport)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(cc) {

  import requestSupport._

  private val testUserForm: Form[TestUserForm] = Form[TestUserForm](
    mapping(
      "utr" -> optional(text),
      "returns-response-body" -> text
        .verifying("'returns' response body must be valid json value", x => Try(Json.parse(x)).isSuccess),
      "returns-status-code" -> text
        .verifying("'returns' status code must not be empty", !_.isEmpty)
        .verifying("'returns' status code must be valid http status code", x => Try(x.toInt).isSuccess && x.toInt < 599 && x.toInt > 100),
      "has-sa-enrolment" -> boolean,
      "isOnIA" -> boolean,
      "authority-id" -> optional(text),
      "affinity-group" -> text.verifying("'Affinity group' must not be 'Individual', 'Organisation' or 'Agent'",
        x => List("Individual", "Organisation", "Agent").contains(x)),
      "over200" -> boolean,
      "debits-response-body" -> text
        .verifying("'debits' response body must be valid json value", x => Try(Json.parse(x)).isSuccess),
      "debits-status-code" -> text
        .verifying("'debits' status code must not be empty", !_.isEmpty)
        .verifying("'debits' status code must be valid http status code", x => Try(x.toInt).isSuccess && x.toInt < 599 && x.toInt > 100),
      "sa-taxpayer-response-body" -> text
        .verifying("'sa-taxpayer-response-body' response body must be valid json value", x => Try(Json.parse(x)).isSuccess),
      "sa-taxpayer-status-code" -> text
        .verifying("'sa-taxpayer-status-code' status code must not be empty", !_.isEmpty)
        .verifying("'sa-taxpayer-status-code' status code must be valid http status code", x => Try(x.toInt).isSuccess && x.toInt < 599 && x.toInt > 100),
      "continue-url" -> optional(text)
    )(TestUserForm.apply)(TestUserForm.unapply)
  )

  def testUsers(): Action[AnyContent] = Action { implicit request =>
    Ok(views.create_user_and_log_in(testUserForm.fill(TestUserForm.initial)))
  }

  def logIn(): Action[AnyContent] = Action.async { implicit request =>
    testUserForm.bindFromRequest().fold(
      formWithErrors => Future.successful(
        BadRequest(views.create_user_and_log_in(formWithErrors))
      ),
      tu =>
        logIn(tu.asTestUser)
    )
  }

  private def logIn(tu: TestUser)(implicit request: Request[AnyContent]): Future[Result] = {
    val loginSessionF = loginService.logIn(tu)
    val setTaxpayerResponseF = saStubConnector.setTaxpayerResponse(tu)
    val setReturnsF = desStubConnector.setReturns(tu)
    val setDebitsF = desStubConnector.setDebits(tu)
    val result = for {
      loginSession <- loginSessionF
      _ <- setTaxpayerResponseF
      _ <- setReturnsF
      _ <- setDebitsF
      _ <- if (tu.isOnIA) iaConnector.uploadUtr(tu.utr.v) else Future.successful(())
      newSession = Session(loginSession.data)
      url = tu.continueUrl.getOrElse(routes.InspectorController.inspect().url)
    } yield Redirect(url).withSession(newSession)
    result
  }

  private lazy val redirectToSessionView = Redirect(routes.InspectorController.inspect())
}