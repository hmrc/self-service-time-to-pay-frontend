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
import play.api.mvc.{Result, Results}
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

  val sessionKey: String = "ttpSubmission"

  def ttpSessionCarrier()(implicit hc: HeaderCarrier): HeaderCarrier = hc.copy(sessionId = hc.extraHeaders.toMap.get(TTPSessionId.ttpSessionId).map(SessionId))

  def putTtpSessionCarrier(body: TTPSubmission)(implicit writes: Writes[TTPSubmission], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    sessionCache.cache[TTPSubmission](sessionKey, body)(writes, ttpSessionCarrier, ec)
  }

  def getTtpSessionCarrier(implicit hc: HeaderCarrier, reads: Reads[TTPSubmission], ec: ExecutionContext): Future[Option[TTPSubmission]] = {
    sessionCache.fetchAndGetEntry[TTPSubmission](sessionKey)(ttpSessionCarrier, reads, ec)
  }

  def putAmount(amount: BigDecimal)(implicit writes: Writes[BigDecimal], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    sessionCache.cache[BigDecimal]("amount", amount)(writes, ttpSessionCarrier, ec)
  }

  def getAmount()(implicit hc: HeaderCarrier, reads: Reads[BigDecimal], ec: ExecutionContext): Future[Option[BigDecimal]] = {
    sessionCache.fetchAndGetEntry[BigDecimal]("amount")(ttpSessionCarrier, reads, ec)
  }

  def putIsBPath(isBpath: Boolean)(implicit writes: Writes[Boolean], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    sessionCache.cache[Boolean]("isBPath", isBpath)(writes, ttpSessionCarrier, ec)
  }

  def getIsBpath()(implicit hc: HeaderCarrier, reads: Reads[Boolean], ec: ExecutionContext): Future[Option[Boolean]] = {
    sessionCache.fetchAndGetEntry[Boolean]("isBPath")(ttpSessionCarrier, reads, ec)
  }

  def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    sessionCache.remove()(ttpSessionCarrier, ec)
  }

  /**
   * Manages code blocks where the user should be logged in and meet certain eligibility criteria
   */
  def authorizedForSsttp(block: TTPSubmission => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    this.getTtpSessionCarrier.flatMap[Result] {
      case Some(submission @ TTPSubmission(Some(_), _, _, Some(_), _, _, Some(EligibilityStatus(true, _)), _, _, _)) =>
        block(submission)
      case _ =>
        Future.successful(ErrorHandler.redirectOnError)
    }
  }

  protected def updateOrCreateInCache(found: TTPSubmission => TTPSubmission, notFound: () => TTPSubmission)(implicit hc: HeaderCarrier): Future[CacheMap] = {
    this.getTtpSessionCarrier.flatMap {
      case Some(ttpSubmission) =>
        Logger.info("TTP data found - merging record")
        this.putTtpSessionCarrier(found(ttpSubmission))
      case None =>
        Logger.info("No TTP Submission data found in cache")
        this.putTtpSessionCarrier(notFound())
    }
  }

}