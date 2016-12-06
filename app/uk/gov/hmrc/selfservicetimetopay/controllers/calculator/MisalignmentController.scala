/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.controllers.calculator

import play.api.mvc._
import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.controllerVariables.{fakeAmountsDue, fakeDebits}
import views.html.selfservicetimetopay.calculator.misalignment
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

class MisalignmentController extends TimeToPayController {

  def getMisalignmentPage: Action[AnyContent] = Action { implicit request =>
    Ok(misalignment.render(fakeAmountsDue, fakeDebits, request))
  }

  def submitRecalculate: Action[AnyContent] = Action.async { implicit request =>
    sessionCache.get.map {
      case _ => Ok(misalignment.render(fakeAmountsDue, fakeDebits, request))
    }
  }
}