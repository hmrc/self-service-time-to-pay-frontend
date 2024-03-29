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

package ssttparrangement

import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import play.api.libs.json.{Format, Json}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import scala.util.control.Exception._

final case class ArrangementForm(
    dayOfMonthOpt: Option[Int]
) {
  lazy val dayOfMonth: Int = dayOfMonthOpt.getOrElse(28)
}

object ArrangementForm {

  def dayOfMonthForm: Form[ArrangementForm] = Form(
    mapping(
      "dayOfMonth" -> mandatoryIfTrue("other", dayOfMonth),
      "other" -> optional(boolean)
        .verifying("ssttp.arrangement.change_day.payment-day.required", {
          _.isDefined
        })
    ){ case (s, _) => ArrangementForm(s.map(_.toInt)) }{ data =>
        Option {
          (data.dayOfMonthOpt.map(_.toString), data.dayOfMonthOpt.map(day => if (day == 28) { false } else { true }))
        }
      }
  )

  val dayOfMonth: Mapping[String] = text
    .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i: String => i.nonEmpty })
    .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => i.isEmpty || (i.nonEmpty && isInt(i)) })
    .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt >= 1)) })
    .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt <= 28)) })

  def isInt(input: String): Boolean = (catching(classOf[NumberFormatException]) opt input.toInt).nonEmpty

  implicit val format: Format[ArrangementForm] = Json.format[ArrangementForm]
}
