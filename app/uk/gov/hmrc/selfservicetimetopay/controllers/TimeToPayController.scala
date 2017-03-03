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

package uk.gov.hmrc.selfservicetimetopay.controllers

import play.api.mvc.{ActionBuilder, AnyContent, Request, Result, Action => PlayAction}
import play.api.{Logger, Play}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.auth.{SaGovernmentGateway, SaRegime}
import uk.gov.hmrc.selfservicetimetopay.config.SsttpFrontendConfig.ttpSessionId
import uk.gov.hmrc.selfservicetimetopay.config._
import uk.gov.hmrc.selfservicetimetopay.connectors.{SessionCacheConnector => KeystoreConnector}
import uk.gov.hmrc.selfservicetimetopay.models.{EligibilityExistingTTP, EligibilityStatus, EligibilityTypeOfTax, TTPSubmission}
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import uk.gov.hmrc.selfservicetimetopay.util.{CheckSessionAction, SessionProvider}

import scala.concurrent.Future

trait TimeToPayController extends FrontendController with Actions with CheckSessionAction {
  checkSessionAction: CheckSessionAction =>

  override val sessionProvider: SessionProvider = new SessionProvider() {}
  override lazy val authConnector: AuthConnector = FrontendAuthConnector
  implicit lazy val sessionCache: KeystoreConnector = Play.current.injector.instanceOf[KeystoreConnector]
  protected lazy val Action: ActionBuilder[Request] = checkSessionAction andThen PlayAction
  protected type AsyncPlayUserRequest = AuthContext => Request[AnyContent] => Future[Result]
  protected lazy val authenticationProvider: GovernmentGateway = SaGovernmentGateway
  protected lazy val saRegime = SaRegime(authenticationProvider)

  protected val validTypeOfTax = Some(EligibilityTypeOfTax(hasSelfAssessmentDebt = true))
  protected val validExistingTTP = Some(EligibilityExistingTTP(Some(false)))

  protected def redirectOnError: Result = Redirect(routes.SelfServiceTimeToPayController.start())

  private val timeToPayConfidenceLevel = new IdentityConfidencePredicate(ConfidenceLevel.L200, Future.successful(Redirect(routes.SelfServiceTimeToPayController.getUnavailable())))

  def authorisedSaUser(body: AsyncPlayUserRequest): PlayAction[AnyContent] = AuthorisedFor(saRegime, timeToPayConfidenceLevel).async(body)

  def authorizedForSsttp(block: (TTPSubmission => Future[Result]))(implicit authContext: AuthContext, hc: HeaderCarrier): Future[Result] = {
    sessionCache.get.flatMap[Result] {
      case Some(submission@TTPSubmission(Some(_), _, _, Some(_), `validTypeOfTax`,
      `validExistingTTP`, _, _, Some(EligibilityStatus(true, _)), _)) =>
        block(submission)
      case _ =>
        Future.successful(redirectOnError)
    }
  }

  override implicit def hc(implicit request: Request[_]): HeaderCarrier = {
    request.cookies.find(_.name == ttpSessionId).fold(super.hc(request)) { id =>
      super.hc(request).withExtraHeaders(ttpSessionId -> id.value)
    }
  }

  protected def updateOrCreateInCache(found: (TTPSubmission) => TTPSubmission, notFound: () => TTPSubmission)(implicit hc: HeaderCarrier) = {
    sessionCache.get.flatMap {
      case Some(ttpSubmission) =>
        Logger.info("TTP data found - merging record")
        sessionCache.put(found(ttpSubmission))
      case None =>
        Logger.info("No TTP Submission data found in cache")
        sessionCache.put(notFound())
    }
  }
}
