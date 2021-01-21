/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.action

import com.google.inject.Inject
import play.api.mvc._
import playsession.PlaySessionSupport
import playsession.PlaySessionSupport._

import scala.concurrent.{ExecutionContext, Future}

class AddJourneyIdToSessionAction @Inject() ()(
    implicit
    ec: ExecutionContext
) extends ActionFunction[AuthenticatedRequestWithJourney, AuthenticatedRequestWithJourney] {

  @SuppressWarnings(Array("org.wartremover.warts.ExplicitImplicitTypes"))
  override def invokeBlock[A](request: AuthenticatedRequestWithJourney[A], block: AuthenticatedRequestWithJourney[A] => Future[Result]): Future[Result] = {
    implicit val r = request
    block(request).map(_.placeInSession(request.journey._id))
  }

  override protected def executionContext: ExecutionContext = ec
}
