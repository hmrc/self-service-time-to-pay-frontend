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

package journey

import journey.Statuses.{FinishedApplicationSuccessful, InProgress}
import org.mongodb.scala.model.{Filters, Updates}
import play.api.mvc.{Request, Result, Results}
import req.RequestSupport
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyService @Inject() (journeyRepo: JourneyRepo)(implicit ec: ExecutionContext) {

  import RequestSupport._
  import playsession.PlaySessionSupport._

  def saveJourney(journey: Journey)(implicit request: Request[_]): Future[Unit] = Mdc.preservingMdc {
    journeyRepo
      .upsert(journey)
  }

  def getMaybeJourney()(implicit request: Request[_]): Future[Option[Journey]] = Mdc.preservingMdc {
    request.readJourneyId.fold[Future[Option[Journey]]](Future.successful(None)){ id =>
      journeyRepo.findById(id).recoverWith{
        case err: java.lang.RuntimeException if err.getMessage.contains("Failed to parse json as journey.Journey") && err.getMessage.contains("createdTTL") =>
          JourneyLogger.info(s"Fixed Journey with missing 'createdTTL'")
          journeyRepo.collection.updateOne(filter = Filters.equal("_id", id.value), Updates.set("createdTTL", LocalDateTime.now(ZoneOffset.UTC)))
            .headOption()
            .flatMap(_ => journeyRepo.findById(id))
      }
    }
  }

  def getJourney()(implicit request: Request[_]): Future[Journey] = Mdc.preservingMdc {
    getMaybeJourney().map(_.getOrElse(throw new RuntimeException(s"Journey not found [ID: ${request.readJourneyId}]")))
  }

  /**
   * Manages code blocks where the user should be logged in and meet certain eligibility criteria
   */
  def authorizedForSsttp(block: Journey => Future[Result])(implicit request: Request[_]): Future[Result] = {
    JourneyLogger.info(s"${this.getClass.getSimpleName}: $request")

    for {
      journey <- getJourney()
      result <- journey match {
        case journey if journey.status == InProgress =>
          JourneyLogger.info(s"${this.getClass.getSimpleName}.authorizedForSsttp: currentSubmission", journey)
          journey.requireIsEligible()
          block(journey)

        case journey if journey.status == FinishedApplicationSuccessful =>
          JourneyLogger.info(s"${this.getClass.getSimpleName}.authorizedForSsttp: currentSubmission (no eligible journey in progress)", journey)
          Future.successful(Results.Redirect(ssttparrangement.routes.ArrangementController.applicationComplete()))

      }
    } yield result
  }
}
