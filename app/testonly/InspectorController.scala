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

package testonly

import config.AppConfig
import controllers.FrontendBaseController

import javax.inject.{Inject, Singleton}
import journey.JourneyService
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import req.RequestSupport
import ssttpcalculator.CalculatorService
import util.SelectedScheduleHelper
import views.Views

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class InspectorController @Inject() (
    val calculatorService: CalculatorService,
    cc:                    MessagesControllerComponents,
    journeyService:        JourneyService,
    views:                 Views,
    requestSupport:        RequestSupport
)(implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendBaseController(cc)
  with SelectedScheduleHelper {

  import requestSupport._

  def clearPlaySession(): Action[AnyContent] = Action { _ =>
    Redirect(routes.InspectorController.inspect()).withSession()
  }

  def inspect(): Action[AnyContent] = Action.async { implicit request =>
    for {
      maybeJourney <- journeyService.getMaybeJourney()
    } yield Ok(views.inspector(
      request.session.data,
      List(
        "debitDate" -> maybeJourney.flatMap(_.debitDate).json,
        "taxpayer" -> maybeJourney.flatMap(_.maybeTaxpayer).json,
        "schedule" -> Try(maybeJourney.map(maybeSelectedSchedule(_))).toOption.json,
        "bankDetails" -> maybeJourney.flatMap(_.maybeBankDetails).json,
        "eligibilityStatus" -> maybeJourney.map(_.maybeEligibilityStatus).json
      ),
      "not supported - todo remove it",
      request.headers.toSimpleMap.toSeq
    ))
  }

  implicit class JsonOps[A: Writes](a: A) {
    def json: String = Json.prettyPrint(Json.toJson(a))
  }
}
