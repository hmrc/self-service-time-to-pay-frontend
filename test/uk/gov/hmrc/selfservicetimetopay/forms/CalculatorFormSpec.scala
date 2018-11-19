/*
 * Copyright 2018 HM Revenue & Customs
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

class CalculatorFormSpec extends PlaySpec {

  "CalculatorForm.amountDueForm" must {

    "return errors when nothing is selected in payTodayForm" in {
      val postData = Json.obj()

      val validatedForm = CalculatorForm.payTodayForm.bind(postData)

      assert(validatedForm.errors.contains(FormError("paytoday", List("ssttp.calculator.form.payment_today_question.required"))))
    }

    "return no errors when true is selected in payTodayForm" in {
      val postData = Json.obj("paytoday" -> "true")

      val validatedForm = CalculatorForm.payTodayForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return no errors when false is selected in payTodayForm" in {
      val postData = Json.obj("paytoday" -> "false")

      val validatedForm = CalculatorForm.payTodayForm.bind(postData)

      assert(validatedForm.errors.isEmpty)
    }

    "return errors when amount is empty" in {
      val postData = Json.obj("amount" -> "")

      val validatedForm = CalculatorForm.createSinglePaymentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.what-you-owe-amount.amount.required"))))
    }

    "return errors when amount is less then zero" in {
      val postData = Json.obj("amount" -> "-5")

      val validatedForm = CalculatorForm.createSinglePaymentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.what-you-owe-amount.amount.required.min"))))
    }

    "return errors when amount is too high" in {
      val postData = Json.obj("amount" -> "1000000000001")

      val validatedForm = CalculatorForm.createSinglePaymentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("amount", List("ssttp.calculator.form.what-you-owe-amount.amount.less-than-maxval"))))
    }

    "return no errors when amount is valid" in {
      val postData = Json.obj("amount" -> "1234")

      val validatedForm = CalculatorForm.createSinglePaymentForm().bind(postData)

      assert(validatedForm.errors.isEmpty)
    }
  }

  "CalculatorForm.createInstalmentForm" must {

    "return errors when nothing is there" in {
      val postData = Json.obj()

      val validatedForm = CalculatorForm.createInstalmentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("months", List("error.required"))))
    }

    "return errors when nothing there is letters" in {
      val postData = Json.obj("months" -> "January")

      val validatedForm = CalculatorForm.createInstalmentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("months", List("ssttp.calculator.results.month.required"))))
    }

    "return errors when the max month exceeds the range " in {
      val postData = Json.obj("months" -> "12")

      val validatedForm = CalculatorForm.createInstalmentForm().bind(postData)

      assert(validatedForm.errors.contains(FormError("months", List("ssttp.calculator.results.month.out-of-range"))))
    }

    "return errors when the max month exceeds the range when we set it in " in {
      val postData = Json.obj("months" -> "4")

      val validatedForm = CalculatorForm.createInstalmentForm(Seq("2","3")).bind(postData)

      assert(validatedForm.errors.contains(FormError("months", List("ssttp.calculator.results.month.out-of-range"))))
    }
  }
}
