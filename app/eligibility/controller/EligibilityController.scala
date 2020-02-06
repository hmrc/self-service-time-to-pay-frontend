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

package eligibility.controller

import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import eligibility.connector.IaService
import eligibility.model._
import eligibility.service.EligibilityService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityController @Inject() (ia: IaService, cc: ControllerComponents) extends BackendController(cc) with EligibilityService {
  /**
   * Takes a user's details and returns any reasons for ineligibility for Self Service Time To Pay if any
   */
  def eligibility(utr: String) = Action.async(parse.json) { implicit request =>

    request.body.validate[EligibilityRequest] match {
      case JsSuccess(er, _) => ia.checkIaUtr(utr).map(onIa => Ok(Json.toJson(determineEligibility(er, onIa))))
      case jsE: JsError     => Future.successful(BadRequest(jsE.toString))
    }
  }

}

