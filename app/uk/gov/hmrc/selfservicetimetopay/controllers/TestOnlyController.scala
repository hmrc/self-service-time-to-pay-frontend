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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.DAYS
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import javax.inject._

import play.api.Logger
import play.api.libs.json.{Format, Json, Writes}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.selfservicetimetopay.connectors._
import uk.gov.hmrc.selfservicetimetopay.forms.ArrangementForm
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._
import views.html.selfservicetimetopay.arrangement.{application_complete, instalment_plan_summary}

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.math.BigDecimal
import scala.util.control.NonFatal

class TestOnlyController @Inject()(val messagesApi: play.api.i18n.MessagesApi, ddConnector: DirectDebitConnector,
                                   calculatorConnector: CalculatorConnector,
                                   taxPayerConnector: TaxPayerConnector,
                                   eligibilityConnector: EligibilityConnector)
extends TimeToPayController with play.api.i18n.I18nSupport {

  import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

  def showSession() = Action.async { implicit request =>

    val sessionCacheF = sessionCache.get
    val authorityF = authConnector.currentAuthority.map(_.json).recover{
      //TODO: don't know why it always throws exception here
      case NonFatal(e) => e.getMessage
    }

    for {
      maybeSubmission <- sessionCacheF
      authority <- authorityF

    } yield Ok(views.html.selfservicetimetopay.testonly.session(
        request.session.data,
        List(
          "eligibilityTypeOfTax" -> maybeSubmission.flatMap(_.eligibilityTypeOfTax).json,
          "eligibilityExistingTtp" -> maybeSubmission.flatMap(_.eligibilityExistingTtp).json,
          "debitDate" -> maybeSubmission.flatMap(_.debitDate).json,
          "taxpayer" -> maybeSubmission.flatMap(_.taxpayer).json,
          "schedule" -> maybeSubmission.flatMap(_.schedule).json,
          "bankDetails" -> maybeSubmission.flatMap(_.bankDetails).json,
          "existingDDBanks" -> maybeSubmission.flatMap(_.existingDDBanks).json,

          "calculatorData" -> maybeSubmission.map(_.calculatorData).json,
          "durationMonths" -> maybeSubmission.map(_.durationMonths).json,
          "eligibilityStatus" -> maybeSubmission.map(_.eligibilityStatus).json
        ),
        authority
      ))

  }



  implicit class JsonOps[A: Writes](a: A){
    def json: String = Json.prettyPrint(Json.toJson(a))
  }
}
