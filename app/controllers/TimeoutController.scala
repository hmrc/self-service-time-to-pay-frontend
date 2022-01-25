/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.action.Actions
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.Views

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TimeoutController @Inject() (actions: Actions,
                                   views:   Views,
                                   mcc:     MessagesControllerComponents)
  (implicit ec: ExecutionContext) extends FrontendController(mcc) {
  implicit def toFuture(r: Result): Future[Result] = Future.successful(r)

  def keepAliveSession(): Action[AnyContent] = Action(NoContent)

  def killSession(): Action[AnyContent] = Action { implicit request =>
    Ok(views.delete_answers()).withNewSession
  }
}
