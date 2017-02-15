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

import java.util.Calendar

import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.libs.json.Json

class CalculatorFormSpec extends PlaySpec {

  "CalculatorForm.amountDueForm" must {

    "return no errors with valid data " in {
      val postData = Json.obj("amount" -> "2000.05",
            "dueBy.dueByDay" -> "20",
            "dueBy.dueByMonth" -> "2",
            "dueBy.dueByYear" -> "2017"
      )

      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return an error if amount is empty" in {
      val postData = Json.obj(

      )
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.amounts_due.amount.required"))))
    }

    "return an error if amount is zero" in {
      val postData = Json.obj("amount" -> "0")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.amounts_due.amount.positive"))))
    }

    "return an error if amount is negative" in {
      val postData = Json.obj("amount" -> "-10")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.amounts_due.amount.positive"))))
    }

    "return an error if amount is too high" in {
      val postData = Json.obj("amount" -> "9999999999999999999999999999999999999999999999999999999999999999999999999999999")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.amounts_due.amount.less-than-maxval"))))
    }

    "return an error if dueByDay is empty" in {
      val postData = Json.obj("dueBy.dueByDay" -> "")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByDay", List("ssttp.calculator.form.amounts_due.due_by.required-day"))))
    }

    "return an error if dueByDay is not a whole number" in {
      val postData = Json.obj("dueBy.dueByDay" -> "1.5")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByDay", List("ssttp.calculator.form.amounts_due.due_by.not-valid-day"))))
    }

    "return an error if dueByDay is less than 1" in {
      val postData = Json.obj("dueBy.dueByDay" -> "0")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByDay", List("ssttp.calculator.form.amounts_due.due_by.not-valid-day"))))
    }

    "return an error if dueByDay is more than 31" in {
      val postData = Json.obj("dueBy.dueByDay" -> "32")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByDay", List("ssttp.calculator.form.amounts_due.due_by.not-valid-day"))))
    }

    "return an error if dueByMonth is empty" in {
      val postData = Json.obj()
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByMonth", List("ssttp.calculator.form.amounts_due.due_by.required-month"))))
    }

    "return an error if dueByMonth is less than 1" in {
      val postData = Json.obj("dueBy.dueByMonth" -> "0")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByMonth", List("ssttp.calculator.form.amounts_due.due_by.not-valid-month"))))
    }

    "return an error if dueByMonth is more than 12" in {
      val postData = Json.obj("dueBy.dueByMonth" -> "13")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByMonth", List("ssttp.calculator.form.amounts_due.due_by.not-valid-month"))))
    }

    "return an error if dueByYear is empty" in {
      val postData = Json.obj("dueBy.dueByYear" -> "")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByYear", List("ssttp.calculator.form.amounts_due.due_by.required-year"))))
    }


    "return an error if dueByYear is not a whole number" in {
      val postData = Json.obj("dueBy.dueByYear" -> "2000.5")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByYear", List("ssttp.calculator.form.amounts_due.due_by.not-valid-year"))))
    }


    "return an error if dueByYear is before 1996" in {
      val postData = Json.obj("dueBy.dueByYear" -> "1995")
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByYear", List("ssttp.calculator.form.amounts_due.due_by.not-valid-year-too-low"))))
    }

    "return an error if dueByYear is more than 1 year ahead" in {
      val postData = Json.obj("dueBy.dueByYear" -> (Calendar.getInstance().get(Calendar.YEAR) + 2).toString)
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByYear", List("ssttp.calculator.form.amounts_due.due_by.not-valid-year-too-high"))))
    }

    "return an error if dueBy is not a real date" in {
      val postData = Json.obj(
        "dueBy.dueByYear" -> "2015",
        "dueBy.dueByMonth" -> "2",
        "dueBy.dueByDay" -> "31"
      )
      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy", List("ssttp.calculator.form.amounts_due.due_by.not-valid-date"))))
    }

    "return errors when user inputs invalid characters in dueByYear " in {
      val postData = Json.obj("amount" -> "2000.05",
        "dueBy.dueByDay" -> "20",
        "dueBy.dueByMonth" -> "2",
        "dueBy.dueByYear" -> "201.e7"
      )

      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByYear", List("ssttp.calculator.form.amounts_due.due_by.not-valid-year"))))
    }

    "return errors when user inputs invalid characters in dueByDay" in {
      val postData = Json.obj("amount" -> "2000.05",
        "dueBy.dueByDay" -> "2.1e",
        "dueBy.dueByMonth" -> "2",
        "dueBy.dueByYear" -> "2017"
      )

      val validatedForm = CalculatorForm.amountDueForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("dueBy.dueByDay", List("ssttp.calculator.form.amounts_due.due_by.not-valid-day"))))
    }
    "payTodayForm return   errors when nothing is selected  when " in {
      val postData = Json.obj()

      val validatedForm = CalculatorForm.payTodayForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("paytoday", List("ssttp.calculator.form.payment_today_question.required"))))
    }
    "payTodayForm should return no errors when user does not select anything in " in {
      val postData = Json.obj("paytoday" -> "true")

      val validatedForm = CalculatorForm.payTodayForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

  }
}
