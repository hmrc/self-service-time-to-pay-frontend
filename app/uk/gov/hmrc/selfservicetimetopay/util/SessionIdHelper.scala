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

package uk.gov.hmrc.selfservicetimetopay.util

import java.util.UUID

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.controllers.routes

import scala.concurrent.Future

trait SessionProvider {
  def createTtpCookie() = Cookie(name = ttpSessionId, value = s"ttp-session-${UUID.randomUUID}")
}

trait CheckSessionAction extends ActionBuilder[Request] with ActionFilter[Request] {
  val sessionProvider: SessionProvider

  protected lazy val redirectToStartPage = Results.Redirect(routes.SelfServiceTimeToPayController.start())

  def filter[A](request: Request[A]): Future[Option[Result]] = {
    Future.successful(
      request.cookies.find(_.name == ttpSessionId).fold[Option[Result]](
        Some(redirectToStartPage.withCookies(sessionProvider.createTtpCookie()))
      ) {
        _ => None
      }
    )
  }
}
