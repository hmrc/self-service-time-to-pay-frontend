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

package audit

import journey.Journey
import play.api.libs.json.{JsObject, Json, OWrites}
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

object DataEventFactory {
  def manualAffordabilityCheck(journey: Journey): ExtendedDataEvent = {
    val detail = Json.obj(
      "totalDebt" -> "5000",
      "spending" -> "9001.56",
      "income" -> "5000",
      "halfDisposalIncome" -> "-4001.56",
      "status" -> "Negative Disposable Income",
      "utr"  -> "012324729"
    )
    ExtendedDataEvent(
      auditSource = "pay-what-you-owe",
      auditType = "ManualAffordabilityCheck",
      detail = detail,

    )
  }
  def manualAffordabilityPlanSetUp(): ExtendedDataEvent = ???
}
