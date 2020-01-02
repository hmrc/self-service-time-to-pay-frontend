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

package journey

import controllers.ErrorHandler
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Request, RequestHeader, Result, Session}
import req.RequestSupport
import sessioncache.SsttpSessionCache
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.selfservicetimetopay.models.EligibilityStatus

import scala.concurrent.{ExecutionContext, Future}

class JourneyService @Inject() (
    sessionCache: SsttpSessionCache,
    journeyRepo:  JourneyRepo)(
    implicit
    ec: ExecutionContext
) {

  import RequestSupport._
  import repo.RepoResultChecker._
  import playsession.PlaySessionSupport._

  def saveJourney(journey: Journey)(implicit request: Request[_]): Future[Unit] =
    journeyRepo
      .upsert(journey._id, journey)
      .checkResult

  def getJourney()(implicit request: Request[_]): Future[Journey] = for {
    journeyId <- Future.successful(()).map(_ => request.readJourneyId)
    maybeJourney <- journeyRepo.findById(journeyId)
    journey = maybeJourney.getOrElse(throw new RuntimeException(s"Journey not found [$journeyId]"))
  } yield journey

  /**
   * Manages code blocks where the user should be logged in and meet certain eligibility criteria
   */
  def authorizedForSsttp(block: Journey => Future[Result])(implicit request: Request[_]): Future[Result] = {
    for {
      journey <- getJourney()
      result <- journey match {
        case submission @ Journey(_, _, Some(_), _, _, Some(_), _, _, Some(EligibilityStatus(true, _)), _, _) => block(submission)
        case _ => Future.successful(ErrorHandler.redirectToStartPage)
      }
    } yield result
  }

}
