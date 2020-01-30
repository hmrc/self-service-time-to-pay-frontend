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

package controllers

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import scala.concurrent.Future

abstract class FrontendBaseController(cc: ControllerComponents)
  extends BaseControllerHelpers {

  import req.RequestSupport._

  override val controllerComponents: ControllerComponents = cc

  val Action = controllerComponents.actionBuilder

  implicit def toFutureResult(result: Result): Future[Result] = Future.successful(result)

  //TODO: move it to some auth service
  def isSignedIn(implicit request: Request[_]): Boolean = hc.authorization.isDefined

  def redirectOnError(implicit request: Request[_]): Result = {
    //Instead of silently redirecting users to start page on erronous situation now we throw exception
    //so it is visible that application doesn't work!
    JourneyLogger.info(s"${this.getClass.getSimpleName}: redirecting on error")
    throw new RuntimeException("Something went wrong. Inspect stack trace and fix bad code")
  }

  def isWelsh(implicit request: Request[_]): Boolean = {
    val currantLang: String = request.cookies.get("PLAY_LANG").fold("en")(cookie => cookie.value)
    if (currantLang == "cy") true else false
  }
}
