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

package testonly

import config.AppConfig
import controllers.FrontendBaseController
import javax.inject.Inject
import journey.{Journey, JourneyService}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.MessagesControllerComponents
import req.RequestSupport
import ssttpcalculator.CalculatorConnector
import ssttpdirectdebit.DirectDebitConnector
import ssttpeligibility.EligibilityConnector
import timetopaytaxpayer.cor.TaxpayerConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.Views

import scala.concurrent.{ExecutionContext, Future}

class InspectorController @Inject() (
    ddConnector:          DirectDebitConnector,
    calculatorConnector:  CalculatorConnector,
    taxPayerConnector:    TaxpayerConnector,
    eligibilityConnector: EligibilityConnector,
    cc:                   MessagesControllerComponents,
    submissionService:    JourneyService,
    views:                Views,
    requestSupport:       RequestSupport)(implicit appConfig: AppConfig,
                                          ec: ExecutionContext
) extends FrontendBaseController(cc) {

  import requestSupport._

  def clearPlaySession() = Action { implicit request =>
    redirectToInspectorView.withSession()
  }

  def inspect() = Action.async { implicit request =>

    val sessionCacheF: Future[Option[Journey]] = submissionService.getJourney.map(Some(_)).recover {
      case e: RuntimeException => None
    }

    for {
      maybeSubmission <- sessionCacheF

    } yield Ok(views.inspector(
      request.session.data,
      List(
        "debitDate" -> maybeSubmission.flatMap(_.debitDate).json,
        "taxpayer" -> maybeSubmission.flatMap(_.maybeTaxpayer).json,
        "schedule" -> maybeSubmission.flatMap(_.schedule).json,
        "bankDetails" -> maybeSubmission.flatMap(_.bankDetails).json,
        "existingDDBanks" -> maybeSubmission.flatMap(_.existingDDBanks).json,

        "calculatorData" -> maybeSubmission.map(_.maybeCalculatorData).json,
        "durationMonths" -> maybeSubmission.map(_.durationMonths).json,
        "eligibilityStatus" -> maybeSubmission.map(_.maybeEligibilityStatus).json
      ),
      "not supported - todo remove it",
      hc.headers
    ))
  }

  lazy val redirectToInspectorView = Redirect(routes.InspectorController.inspect())

  implicit class JsonOps[A: Writes](a: A) {
    def json: String = Json.prettyPrint(Json.toJson(a))
  }
}
