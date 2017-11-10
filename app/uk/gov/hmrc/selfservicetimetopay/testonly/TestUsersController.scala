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
import play.api.mvc.{Action, AnyContent}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.controllers.TimeToPayController


class TestUsersController @Inject()(
  val messagesApi: MessagesApi,
  loginService: LoginService,
  saStubConnector: SaStubConnector
)
extends TimeToPayController with I18nSupport with ServicesConfig {

  /**
    * Logs in and sets up test relevant test data in stubs
    */
  def logIn(): Action[AnyContent] = Action.async { implicit request =>
    val tu = TestUser()
    val sessionF = loginService.logIn(tu)
    val setTaxpayerResponseF = saStubConnector.setTaxpayerResponse(tu.utr)

    for {
      session <- sessionF
      _ <- setTaxpayerResponseF
    } yield redirectToSessionView.withSession(session)
  }

  private lazy val redirectToSessionView = Redirect(routes.InspectorController.inspect())
}


