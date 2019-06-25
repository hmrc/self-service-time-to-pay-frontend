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

import config.{AppConfig, ViewConfig}
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Request, Result, Results}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

@Singleton
class ErrorHandler @Inject() (i18nSupport:              I18nSupport,
                              override val messagesApi: MessagesApi)(implicit viewConfig: ViewConfig,
                                                                     appConfig: AppConfig
) extends FrontendErrorHandler {

  import i18nSupport._

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.selfservicetimetopay.error_template(pageTitle, heading, message)
}

object ErrorHandler {

  def redirectOnError: Result = Results.Redirect(ssttpeligibility.routes.SelfServiceTimeToPayController.start())
}