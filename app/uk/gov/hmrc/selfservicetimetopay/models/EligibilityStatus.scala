/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.selfservicetimetopay.models

import play.api.libs.json._
import uk.gov.hmrc.selfservicetimetopay.modelsFormat.parseFromString

sealed abstract class Reason(val name: String)
case object NoDebt extends Reason("NoDebt")
case object DebtIsInsignificant extends Reason("DebtIsInsignificant")
case object OldDebtIsTooHigh extends Reason("OldDebtIsTooHigh")
case object TotalDebtIsTooHigh extends Reason("TotalDebtIsTooHigh")
case object TTPIsLessThenTwoMonths extends Reason("TTPIsLessThenTwoMonths")
case object ReturnNeedsSubmitting extends Reason("ReturnNeedsSubmitting")
case object IsNotOnIa extends Reason("IsNotOnIa")

object Reason {

  implicit val formatEligibilityReasons = new Format[Reason] {
    override def writes(o: Reason): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[Reason] = json match {
      case o: JsString => parseFromString(o.value).fold[JsResult[Reason]](JsError(s"Failed to parse $json as Reason"))(JsSuccess(_))
      case _           => JsError(s"Failed to parse $json as Reason")
    }
  }

}

case class EligibilityStatus(
    eligible: Boolean,
    reasons:  Seq[Reason]
)

object EligibilityStatus {
  implicit val format: Format[EligibilityStatus] = Json.format[EligibilityStatus]
}
