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

import com.google.inject.Inject
import config.ViewConfig
import play.api.i18n.Messages
import play.api.mvc.Results.{NotFound, Unauthorized}
import play.api.mvc.{Request, Results}
import req.RequestSupport

class UnhappyPathResponses @Inject() (
    viewConfig:     ViewConfig,
    errorHandler:   ErrorHandler,
    requestSupport: RequestSupport) {

  import requestSupport._

  def notFound(implicit request: Request[_]) = NotFound(
    errorHandler.standardErrorTemplate(
      Messages("global.error.pageNotFound404.title"),
      Messages("global.error.pageNotFound404.heading"),
      Messages("global.error.pageNotFound404.message")))

  def unauthorised(buttonLink: String)(implicit request: Request[_]) = Unauthorized(
    errorHandler.standardErrorTemplate(
      Messages("error.auth.title"),
      Messages("error.auth.heading"),
      Messages("error.auth.message")))

  def unauthorised(implicit request: Request[_]) = Unauthorized(
    errorHandler.standardErrorTemplate(
      Messages("unauthorised.title"),
      Messages("unauthorised.heading"),
      ""))

  def gone(buttonLink: String)(implicit request: Request[_]) = Results.Gone(
    errorHandler.standardErrorTemplate(
      Messages("error.gone.title"),
      Messages("error.gone.heading"),
      Messages("error.gone.message")))
}
