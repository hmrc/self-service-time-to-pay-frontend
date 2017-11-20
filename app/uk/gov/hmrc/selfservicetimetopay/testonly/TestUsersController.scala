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

import javax.inject._

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.controllers.TimeToPayController
import play.api.data._
import play.api.data.Forms._
import views.html.selfservicetimetopay.testonly._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class TestUserForm (
  utr: Option[String],
  returnsJson: String,
  returnsResponseStatusCode: String,
  hasSAEnrolment: Boolean,
  authorityId: Option[String],
  affinityGroup: String,
  debitsJson: String,
  debitsResponseStatusCode: String
) {

  def asTestUser: TestUser = TestUser(
    utr = utr.map(Utr.apply).getOrElse(Utr.random()),
    hasSAEnrolment = hasSAEnrolment,
    authorityId = authorityId.map(AuthorityId.apply).getOrElse(AuthorityId.random),
    affinityGroup = AffinityGroup(affinityGroup),
    confidenceLevel = 200,
    returns = Json.parse(returnsJson),
    returnsResponseStatusCode = returnsResponseStatusCode.toInt,
    debits = Json.parse(debitsJson),
    debitsResponseStatusCode = debitsResponseStatusCode.toInt
  )
}

object TestUserForm {
  val initial = TestUserForm(
    utr = None,
    returnsJson = Json.prettyPrint(TestUserReturns.sample1),
    returnsResponseStatusCode = "200",
    hasSAEnrolment = true,
    authorityId = None,
    affinityGroup = AffinityGroup.individual.v,
    debitsJson = Json.prettyPrint(TestUserDebits.sample1),
    debitsResponseStatusCode = "200"
  )
}

class TestUsersController @Inject()(
  val messagesApi: MessagesApi,
  loginService: LoginService,
  saStubConnector: SaStubConnector,
  desStubConnector: DesStubConnector
)
extends TimeToPayController with I18nSupport with ServicesConfig {

  private val testUserForm: Form[TestUserForm] = Form[TestUserForm](
    mapping(
      "utr" -> optional(text),
      "returns-response-body" -> text
        .verifying("'returns' response body must be valid json value", x => Try(Json.parse(x)).isSuccess),
      "returns-status-code" -> text
        .verifying("'returns' status code must not be empty", !_.isEmpty)
        .verifying("'returns' status code must be valid http status code", x => Try(x.toInt).isSuccess && x.toInt < 599 && x.toInt > 100),
      "has-sa-enrolment" -> boolean,
      "authority-id" -> optional(text),
      "affinity-group" -> text.verifying("'Affinity group' must not be 'Individual', 'Organisation' or 'Agent'", x => List("Individual", "Organisation", "Agent").contains(x)),
      "debits-response-body" -> text
        .verifying("'debits' response body must be valid json value", x => Try(Json.parse(x)).isSuccess),
      "debits-status-code" -> text
        .verifying("'debits' status code must not be empty", !_.isEmpty)
        .verifying("'debits' status code must be valid http status code", x => Try(x.toInt).isSuccess && x.toInt < 599 && x.toInt > 100)
    )(TestUserForm.apply)(TestUserForm.unapply)
  )

  def testUsers(): Action[AnyContent] = Action { implicit request =>
    Ok(create_user_and_log_in(testUserForm.fill(TestUserForm.initial)))
  }

  def logIn(): Action[AnyContent] = Action.async { implicit request =>
    testUserForm.bindFromRequest().fold(
      formWithErrors => Future.successful(
        BadRequest(create_user_and_log_in(formWithErrors))
      ),
      tu =>
        logIn(tu.asTestUser)
    )
  }

  private def logIn(tu: TestUser)(implicit request: Request[AnyContent]): Future[Result] = {
    val sessionF = loginService.logIn(tu)
    val setTaxpayerResponseF = saStubConnector.setTaxpayerResponse(tu.utr)
    val setReturnsF = desStubConnector.setReturns(tu)
    val setDebitsF = desStubConnector.setDebits(tu)

    val result = for {
      session <- sessionF
      _ <- setTaxpayerResponseF
      _ <- setReturnsF
      _ <- setDebitsF
    } yield redirectToSessionView.withSession(session)
    result
  }


  def logInX(): Action[AnyContent] = Action.async { implicit request =>

    val tu = TestUser.exemplary()

    val sessionF = loginService.logIn(tu)
    val setTaxpayerResponseF = saStubConnector.setTaxpayerResponse(tu.utr)
    val setReturnsF = desStubConnector.setReturns(tu)

    for {
      session <- sessionF
      _ <- setTaxpayerResponseF
      _ <- setReturnsF
    } yield redirectToSessionView.withSession(session)
  }

  private lazy val redirectToSessionView = Redirect(routes.InspectorController.inspect())
}


