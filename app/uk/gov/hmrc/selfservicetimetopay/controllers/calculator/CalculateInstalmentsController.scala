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

import uk.gov.hmrc.selfservicetimetopay.config.TimeToPayController
import uk.gov.hmrc.selfservicetimetopay.connectors.EligibilityConnector

import scala.concurrent.Future

class CalculateInstalmentsController(eligibilityConnector: EligibilityConnector) extends TimeToPayController {

  def submit() = Action.async { implicit request =>

//    sessionCache.get.map { result =>
//      eligibilityConnector.checkEligibility(result.get.taxPayer.get.selfAssessment)
//    }
    Future.successful(Ok)
  }

//  def getCalculateInstalments(monthsOption:Option[String]): Action[AnyContent] = Action.async { implicit request =>
//
//  }
//
//  def submitCalculateInstalments: Action[AnyContent] = Action.async { implicit request =>
//
//  }

}