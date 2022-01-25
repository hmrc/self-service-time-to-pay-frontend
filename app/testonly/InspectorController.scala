/*
 * Copyright 2022 HM Revenue & Customs
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

import bars.BarsConnector
import config.AppConfig
import controllers.FrontendBaseController
import javax.inject.Inject
import journey.{Journey, JourneyService}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import req.RequestSupport
import ssttpcalculator.CalculatorService
import timetopaytaxpayer.cor.TaxpayerConnector
import views.Views

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class InspectorController @Inject() (
    ddConnector:       BarsConnector,
    calculatorService: CalculatorService,
    taxPayerConnector: TaxpayerConnector,
    cc:                MessagesControllerComponents,
    journeyService:    JourneyService,
    views:             Views,
    requestSupport:    RequestSupport)(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendBaseController(cc) {

  import requestSupport._

  def clearPlaySession(): Action[AnyContent] = Action { implicit request =>
    Redirect(routes.InspectorController.inspect()).withSession()
  }

  def inspect(): Action[AnyContent] = Action.async { implicit request =>
    for {
      maybeJourney <- journeyService.getMaybeJourney
    } yield Ok(views.inspector(
      request.session.data,
      List(
        "debitDate" -> maybeJourney.flatMap(_.debitDate).json,
        "taxpayer" -> maybeJourney.flatMap(_.maybeTaxpayer).json,
        "schedule" -> Try(maybeJourney.map(calculatorService.computeSchedule(_))).toOption.json,
        "bankDetails" -> maybeJourney.flatMap(_.maybeBankDetails).json,
        "existingDDBanks" -> maybeJourney.flatMap(_.existingDDBanks).json,
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
