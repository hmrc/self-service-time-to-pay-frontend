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

package sttpsubmission

import controllers.ErrorHandler
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{Request, Result, Results}
import req.RequestSupport
import sessioncache.SsttpSessionCache
import token.TTPSessionId
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityStatus, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.concurrent.{ExecutionContext, Future}

class SubmissionService @Inject() (
    sessionCache: SsttpSessionCache)(implicit ec: ExecutionContext) {

  import RequestSupport._

  val sessionKey: String = "ttpSubmission"

  def putTtpSessionCarrier(body: TTPSubmission)(implicit writes: Writes[TTPSubmission], request: Request[_]): Future[CacheMap] = {
    sessionCache.cache[TTPSubmission](sessionKey, body)
  }

  def getTtpSubmission(implicit request: Request[_], reads: Reads[TTPSubmission]): Future[Option[TTPSubmission]] = {
    sessionCache.fetchAndGetEntry[TTPSubmission](sessionKey)
  }

  def putAmount(amount: BigDecimal)(implicit writes: Writes[BigDecimal], request: Request[_]): Future[CacheMap] = {
    sessionCache.cache[BigDecimal]("amount", amount)
  }

  def getAmount()(implicit request: Request[_], reads: Reads[BigDecimal]): Future[Option[BigDecimal]] = {
    sessionCache.fetchAndGetEntry[BigDecimal]("amount")
  }

  def putIsBPath(isBpath: Boolean)(implicit writes: Writes[Boolean], request: Request[_]): Future[CacheMap] = {
    sessionCache.cache[Boolean]("isBPath", isBpath)
  }

  def getIsBpath()(implicit request: Request[_], reads: Reads[Boolean]): Future[Option[Boolean]] = {
    sessionCache.fetchAndGetEntry[Boolean]("isBPath")
  }

  def remove()(implicit request: Request[_]): Future[HttpResponse] = {
    sessionCache.remove()
  }

  /**
   * Manages code blocks where the user should be logged in and meet certain eligibility criteria
   */
  def authorizedForSsttp(block: TTPSubmission => Future[Result])(implicit request: Request[_]): Future[Result] = {
    this.getTtpSubmission.flatMap[Result] {
      case Some(submission @ TTPSubmission(Some(_), _, _, Some(_), _, _, Some(EligibilityStatus(true, _)), _, _, _)) =>
        block(submission)
      case _ =>
        Future.successful(ErrorHandler.redirectOnError)
    }
  }

  protected def updateOrCreateInCache(found: TTPSubmission => TTPSubmission, notFound: () => TTPSubmission)(implicit request: Request[_]): Future[CacheMap] = {
    this.getTtpSubmission.flatMap {
      case Some(ttpSubmission) =>
        Logger.info("TTP data found - merging record")
        this.putTtpSessionCarrier(found(ttpSubmission))
      case None =>
        Logger.info("No TTP Submission data found in cache")
        this.putTtpSessionCarrier(notFound())
    }
  }

}
