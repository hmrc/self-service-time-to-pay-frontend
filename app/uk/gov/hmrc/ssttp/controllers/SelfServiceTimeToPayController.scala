/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.ssttp.controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.ssttp._
import scala.concurrent.Future

object SelfServiceTimeToPayController extends FrontendController{

  def present:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(service_start.render(request)))
  }

  def submit:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Redirect(routes.EligibilityController.present()))
  }

  def ttpCallUs:Action[AnyContent] =  Action.async { implicit request =>
    Future.successful(Ok(ttp_call_us.render(request)))
  }

  def ssttpCallUs:Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(ssttp_call_us.render(request)))
  }

}