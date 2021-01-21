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

import javax.inject.Inject
import play.api.mvc._
import playsession.PlaySessionSupport.ResultOps

import scala.concurrent.{ExecutionContext, Future}

class Actions @Inject() (
    authenticatedAction:         AuthenticatedAction,
    getJourneyAction:            GetJourneyAction,
    authorisedSaUserAction:      AuthorisedSaUserAction,
    addJourneyIdToSessionAction: AddJourneyIdToSessionAction,
    defaultActionBuilder:        DefaultActionBuilder
)(implicit ec: ExecutionContext) {

  /**
   * This action fetches enrolments and journey
   */
  def authenticatedSaUser: ActionBuilder[AuthenticatedRequestWithJourney, AnyContent] =
    defaultActionBuilder andThen authenticatedAction andThen getJourneyAction andThen addJourneyIdToSessionAction

  /**
   * This action makes sure that user has a journey and all required enrolments.
   */
  def authorisedSaUser: ActionBuilder[AuthorisedSaUserRequest, AnyContent] =
    defaultActionBuilder andThen authenticatedAction andThen getJourneyAction andThen addJourneyIdToSessionAction andThen authorisedSaUserAction

  //  //We do this in order to place journeyId in the session.
  //  //A journey has been created in action, which can't change the session
  //  def withAuthorisedSaUserAsync(
  //      body: AuthorisedSaUserRequest[AnyContent] => Future[Result]
  //  ): Action[AnyContent] = authorisedSaUser.async { implicit request: AuthorisedSaUserRequest[AnyContent] =>
  //    body(request).map(_.placeInSession(request.journey._id))
  //  }
  //
  //  def withAuthorisedSaUser(body: AuthorisedSaUserRequest[AnyContent] => Result): Action[AnyContent] = withAuthorisedSaUserAsync((request: AuthorisedSaUserRequest[AnyContent]) =>
  //    Future.successful(body(request))
  //  )

  def action: ActionBuilder[Request, AnyContent] = defaultActionBuilder
}
