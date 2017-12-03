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

package uk.gov.hmrc.selfservicetimetopay.util

import java.util.UUID

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.controllers.routes
import uk.gov.hmrc.selfservicetimetopay.util.TTPSessionId._

import scala.concurrent.Future

case class TTPSessionId(v: String)

object TTPSessionId {
  lazy val ttpSessionId: String = "ttpSessionId"
  def newTTPSession(): (String, String) = ttpSessionId -> s"ttp-session-${UUID.randomUUID}"

  implicit class GetTTPSessionOps[A](request: Request[A]) {
    def maybeTTPSessionId: Option[TTPSessionId] = request.session.get(ttpSessionId).map(TTPSessionId.apply)

    def getTTPSessionId: TTPSessionId = maybeTTPSessionId.getOrElse(
      throw new RuntimeException(s"Expected $ttpSessionId to be in the play session")
    )
  }
}


object CheckSessionAction extends ActionBuilder[Request] with ActionFilter[Request] {

  protected lazy val redirectToStartPage = Results.Redirect(routes.SelfServiceTimeToPayController.start())

  def filter[A](request: Request[A]): Future[Option[Result]] = {
      val response: Option[Result] = request.maybeTTPSessionId.fold[Option[Result]] (
        Some(redirectToStartPage.withSession(request.session + TTPSessionId.newTTPSession()))
      ) (_ => None)

    Future.successful(response)
  }
}
