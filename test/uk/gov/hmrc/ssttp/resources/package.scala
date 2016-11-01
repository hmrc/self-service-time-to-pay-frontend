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

package uk.gov.hmrc.ssttp

import play.api.libs.json.Json

import scala.io.Source

package object resources {
  val getBanksResponseJSON = Json.parse(Source.fromFile(s"test/uk/gov/hmrc/ssttp/resources/GetBanksResponse.json").mkString)
  val getInstructionPaymentResponseJSON = Json.parse(
    Source.fromFile(s"test/uk/gov/hmrc/ssttp/resources/GetDirectDebitInstructionPaymentPlanResponse.json")
      .mkString)
}
