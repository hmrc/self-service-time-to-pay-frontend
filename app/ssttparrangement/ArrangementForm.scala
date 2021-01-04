/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import scala.util.control.Exception._

object ArrangementForm {

  val dayOfMonthForm: Form[ArrangementDayOfMonth] = {
      def isInt(input: String): Boolean = (catching(classOf[NumberFormatException]) opt input.toInt).nonEmpty

    Form(mapping(
      "dayOfMonth" -> text
        .verifying("ssttp.arrangement.change_day.payment-day.required", { i: String => i.nonEmpty })
        .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => i.isEmpty || (i.nonEmpty && isInt(i)) })
        .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt >= 1)) })
        .verifying("ssttp.arrangement.change_day.payment-day.out-of-range", { i => !isInt(i) || (isInt(i) && (i.toInt <= 28)) })
    )(dayOfMonth => ArrangementDayOfMonth(dayOfMonth.toInt))(data => Some(data.dayOfMonth.toString))
    )
  }
}
