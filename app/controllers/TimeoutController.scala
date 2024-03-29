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

package controllers

import config.ViewConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import util.Logging
import views.Views

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TimeoutController @Inject() (views:      Views,
                                   mcc:        MessagesControllerComponents,
                                   viewConfig: ViewConfig) extends FrontendController(mcc) with Logging {
  implicit def toFuture(r: Result): Future[Result] = Future.successful(r)

  val keepAliveSession: Action[AnyContent] = Action(NoContent)

  val killSession: Action[AnyContent] = Action { implicit request =>
    appLogger.info("Kill session")

    Ok(views.delete_answers(controllers.routes.TimeoutController.signInAgain)).withNewSession
  }

  val signInAgain: Action[AnyContent] = Action { _ =>
    Redirect(
      viewConfig.loginUrl,
      Map(
        "continue" -> Seq(viewConfig.frontendBaseUrl + ssttpeligibility.routes.SelfServiceTimeToPayController.start.url),
        "origin" -> Seq("pay-online")
      )
    )
  }

}
