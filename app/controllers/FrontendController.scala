/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.{Request, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class FrontendController extends uk.gov.hmrc.play.bootstrap.controller.FrontendController {

  implicit def toFuture(r: Result): Future[Result] = Future.successful(r)

  //TODO: move it to some auth service
  def isSignedIn(implicit hc: HeaderCarrier): Boolean = hc.authorization.isDefined

  //TODO: remove it from this place and investigate correctness of it's usages
  protected def redirectOnError: Result = ErrorHandler.redirectOnError

  def isWelsh(implicit request: Request[_], hc: HeaderCarrier): Boolean = {
    val currantLang: String = request.cookies.get("PLAY_LANG").fold("en")(cookie => cookie.value)
    if (currantLang == "cy") true else false
  }

  //TODO: verify whre it's used, named it correctly. There are few start pages
  protected lazy val redirectToStartPage: Result = Results.Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start())
}