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

package eligibility.model

import play.api.libs.json._

sealed trait EligibilityResult {
  def eligible: Boolean

  def reasons: Seq[Reason]
}

object EligibilityResult {
  def apply(eligible: Boolean, reasons: Seq[Reason]): EligibilityResult = {
    if (eligible) Eligible else Ineligible(reasons)
  }

  def unapply(eligibilityResult: EligibilityResult): Option[(Boolean, Seq[Reason])] = {
    Some((eligibilityResult.eligible, eligibilityResult.reasons))
  }

  implicit val reasonWriter: Writes[Reason] = Reason.Writer
  implicit val eligibilityResultWriter: OWrites[EligibilityResult] = OWrites[EligibilityResult] {
    case e: Eligible.type => Json.obj(
      "eligible" -> true,
      "reasons" -> e.reasons
    )
    case e: Ineligible => Json.obj(
      "eligible" -> false,
      "reasons" -> e.reasons
    )
  }
}

case object Eligible extends EligibilityResult {
  override def eligible: Boolean = true

  override def reasons: Seq[Reason] = Seq.empty
}

case class Ineligible(override val reasons: Seq[Reason]) extends EligibilityResult {
  override def eligible: Boolean = false
}

object Reason {

  object Writer extends Writes[Reason] {
    override def writes(reason: Reason) = JsString(reason.name)
  }

}

sealed abstract class Reason(val name: String)

case object NoDebt extends Reason("NoDebt")

case object DebtIsInsignificant extends Reason("DebtIsInsignificant")

case object OldDebtIsTooHigh extends Reason("OldDebtIsTooHigh")

case object TotalDebtIsTooHigh extends Reason("TotalDebtIsTooHigh")

case object IsNotOnIa extends Reason("IsNotOnIa")

case class ReturnNeedsSubmitting(year: Int) extends Reason(s"ReturnNeedsSubmitting$year")

