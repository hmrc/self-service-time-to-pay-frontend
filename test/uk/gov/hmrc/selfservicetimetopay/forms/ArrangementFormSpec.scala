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

package uk.gov.hmrc.selfservicetimetopay.forms

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.libs.json.Json

class ArrangementFormSpec extends PlaySpec {

  "ArrangementForm.directDebitForm" must {

    "return no errors with valid data" in {
      val postData = Json.obj(
        "dayOfMonth" -> "10"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return error with number less than 1" in {
      val postData = Json.obj(
        "dayOfMonth" -> "0"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.out-of-range"))))
    }

    "return error with number greater than 28" in {
      val postData = Json.obj(
        "dayOfMonth" -> "29"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.out-of-range"))))
    }

    "return error with number with decimal place" in {
      val postData = Json.obj(
        "dayOfMonth" -> "5.4"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.out-of-range"))))
    }

    "return error with text instead of numbers" in {
      val postData = Json.obj(
        "dayOfMonth" -> "three"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.out-of-range"))))
    }

    "return error with special characters instead of numbers" in {
      val postData = Json.obj(
        "dayOfMonth" -> "%##"
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.out-of-range"))))
    }

    "return error with blank entry" in {
      val postData = Json.obj(
        "dayOfMonth" -> ""
      )

      val validatedForm = ArrangementForm.dayOfMonthForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dayOfMonth", List("ssttp.arrangement.instalment-summary.payment-day.required"))))
    }
  }
}
