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

package uk.gov.hmrc.selfservicetimetopay

import play.api.libs.json.Json
import uk.gov.hmrc.selfservicetimetopay.models._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat._

import scala.io.Source

package object resources {
  val getBanksResponseJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/GetBanksResponse.json")
      .mkString)
  val createPaymentPlanResponseJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/GetDirectDebitInstructionPaymentPlanResponse.json")
      .mkString)
  val submitArrangementResponse = Json.fromJson[TTPArrangement](Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/GetArrangementResponse.json")
      .mkString)).get
  val submitDebitsRequest = Json.fromJson[CalculatorInput](Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/SubmitLiabilitiesRequest.json")
      .mkString)).get
  val submitLiabilitiesResponseJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/SubmitLiabilitiesResponse.json")
      .mkString)
  val getBankResponseJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/GetBank.json")
      .mkString)
  val createPaymentRequestJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/CreatePaymentPlanRequest.json")
      .mkString)
  val checkEligibilityTrueRequest = Json.fromJson[SelfAssessment](Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityTrueRequest.json")
      .mkString)).get
  val checkEligibilityTrueResponse = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityTrueResponse.json")
      .mkString)
  val checkEligibilityFalseRequest = Json.fromJson[SelfAssessment](Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityFalseRequest.json")
      .mkString)).get
  val checkEligibilityFalseResponse = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/selfservicetimetopay/resources/CheckEligibilityFalseResponse.json")
      .mkString)
}
