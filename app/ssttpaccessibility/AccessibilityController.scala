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

package ssttpaccessibility

import config.AppConfig
import controllers.FrontendBaseController
import controllers.action.Actions
import javax.inject.Inject
import journey.JourneyService
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import req.RequestSupport
import ssttpdirectdebit.DirectDebitConnector
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.jlogger.JourneyLogger
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class AccessibilityController @Inject() (
    mcc:                  MessagesControllerComponents,
    directDebitConnector: DirectDebitConnector,
    as:                   Actions,
    submissionService:    JourneyService,
    requestSupport:       RequestSupport,
    views:                Views,
    clockProvider:        ClockProvider)(
    implicit
    appConfig: AppConfig,
    ec:        ExecutionContext
) extends FrontendBaseController(mcc) {

  import requestSupport._

  def getAccessibilityStatement(): Action[AnyContent] = as.authorisedSaUser.async { implicit request =>
    JourneyLogger.info(s"AccessibilityController.getAccessibilityStatement: $request")
    Future.successful(Ok(views.accessibility_statement()))
  }
}