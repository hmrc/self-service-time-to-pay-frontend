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

package uk.gov.hmrc.selfservicetimetopay.models

import play.api.libs.functional.syntax.toAlternativeOps
import play.api.libs.json.{Format, Json, Reads, Writes, __}

import scala.math.BigDecimal.RoundingMode.{CEILING, HALF_UP}

final case class PlanSectionRadioButtonChoice(selection: Either[CannotAfford, PlanSelection])

final case class PlanSelection(selection: Either[SelectedPlan, CustomPlanRequest]) {
  def mongoSafe: PlanSelection = {
    val safeDecimal128Precision = 10
    this.copy(selection = this.selection match {
      case Left(SelectedPlan(amount))       => Left(SelectedPlan(amount.setScale(safeDecimal128Precision, CEILING)))
      case Right(CustomPlanRequest(amount)) => Right(CustomPlanRequest(amount.setScale(safeDecimal128Precision, CEILING)))
    })
  }
}

object PlanSelection {
  def apply(selectedPlan: SelectedPlan): PlanSelection = PlanSelection(Left(selectedPlan))
  def apply(customPlanRequest: CustomPlanRequest): PlanSelection = PlanSelection(Right(customPlanRequest))

  val reads: Reads[PlanSelection] = {
    (__ \ "selectedPlan").read[BigDecimal].map(amount => PlanSelection(Left(new SelectedPlan(amount)))) |
      (__ \ "customPlanRequest").read[BigDecimal].map(amount => PlanSelection(Right(new CustomPlanRequest(amount))))
  }

  val writes: Writes[PlanSelection] = (o: PlanSelection) => Json.obj(
    o.selection.fold(
      selectedPlan => "selectedPlan" -> selectedPlan.instalmentAmount,
      customPlanRequest => "customPlanRequest" -> customPlanRequest.customAmount
    )
  )

  implicit val format: Format[PlanSelection] = Format(reads, writes)
}

final case class SelectedPlan(instalmentAmount: BigDecimal)

object SelectedPlan {
  implicit val format: Format[SelectedPlan] = Json.format[SelectedPlan]
}

final case class CustomPlanRequest(customAmount: BigDecimal)

object CustomPlanRequest {
  implicit val format: Format[CustomPlanRequest] = Json.format[CustomPlanRequest]
}

final case class CannotAfford()
