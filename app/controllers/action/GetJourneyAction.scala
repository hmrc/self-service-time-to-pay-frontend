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
import controllers.Assets.Redirect
import journey.{Journey, JourneyService}
import play.api.Logger
import play.api.mvc._
import times.ClockProvider
import timetopaytaxpayer.cor.model.SaUtr
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments}

import scala.concurrent.{ExecutionContext, Future}

final class AuthenticatedRequestWithJourney[A](
    val request:         Request[A],
    val enrolments:      Enrolments,
    val confidenceLevel: ConfidenceLevel,
    val maybeUtr:        Option[SaUtr],
    val credentials:     Option[Credentials],
    val journey:         Journey
) extends WrappedRequest[A](request) {

  lazy val hasActiveSaEnrolment: Boolean = enrolments.enrolments.exists(e => e.key == "IR-SA" && e.isActivated)
}

class GetJourneyAction @Inject() (
    journeyService: JourneyService,
    cc:             MessagesControllerComponents,
    clockProvider:  ClockProvider)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[AuthenticatedRequest, AuthenticatedRequestWithJourney] {

  import clockProvider._

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthenticatedRequestWithJourney[A]]] = {
    implicit val r: Request[A] = request

    for {
      maybeNotFinishedJourney <- journeyService.findJourney().map(_.filterNot(_.isFinished))
      //pre-creates a new journey if its not there or if it's finished
      //otherwise returns what we already have in mongo
      journey <- maybeNotFinishedJourney match {
        case Some(journey) => Future.successful(journey)
        case None =>
          val newJourney = Journey.newJourney
          journeyService.saveJourney(newJourney).map(_ => newJourney)
      }
    } yield Right(new AuthenticatedRequestWithJourney[A](
      request         = request,
      enrolments      = request.enrolments,
      confidenceLevel = request.confidenceLevel,
      maybeUtr        = request.maybeUtr,
      credentials     = request.credentials,
      journey         = journey
    ))
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}

