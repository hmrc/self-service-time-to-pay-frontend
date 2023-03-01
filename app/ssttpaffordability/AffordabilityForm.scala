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

package ssttpaffordability

import play.api.data.Forms.{text, _}
import play.api.data.{Form, FormError, Mapping}
import ssttpaffordability.model.IncomeCategory
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model.forms.helper.FormErrorWithFieldMessageOverrides
import scala.util.Try

object AffordabilityForm {

  val incomeForm: Form[IncomeInput] = Form(
    mapping(
      "monthlyIncome" -> validateIncome(text, MonthlyIncome),
      "benefits" -> validateIncome(text, Benefits),
      "otherIncome" -> validateIncome(text, OtherIncome)
    )(IncomeInput.apply)(IncomeInput.unapply)
  )

  private def validateIncome(mappingStr: Mapping[String], incomeCategory: IncomeCategory): Mapping[String] = {
    mappingStr.verifying(s"ssttp.affordability.your-monthly-income.error.non-numerals.${incomeCategory.messageSuffix}", { i: String =>
      if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
    })
      .verifying(s"ssttp.affordability.your-monthly-income.error.negative.${incomeCategory.messageSuffix}", { i: String =>
        if (i.nonEmpty && Try(BigDecimal(i)).isSuccess && BigDecimal(i).scale <= 2) BigDecimal(i) >= 0.00 else true
      })
      .verifying(s"ssttp.affordability.your-monthly-income.error.decimal-places.${incomeCategory.messageSuffix}", { i =>
        if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
      })
  }

  def validateIncomeInputTotal(form: Form[IncomeInput]): Form[IncomeInput] = {
    if (!form.get.hasPositiveTotal) {
      val formErrorsWithTotalError = form.errors :+ incomeInputTotalNotPositiveSeed
      form.copy(errors = formErrorsWithTotalError)
    } else form
  }

  private val incomeInputTotalNotPositiveSeed: FormError = FormError(
    key      = "monthlyIncome",
    messages = Seq("ssttp.affordability.your-monthly-income.error.required")
  )

  private val allInputsOverrides: Seq[FormError] = Seq(
    FormError("monthlyIncome", ""),
    FormError("benefits", ""),
    FormError("otherIncome", ""),
    FormError("allIncomeInputs", "ssttp.affordability.your-monthly-income.error.required")
  )

  val incomeInputTotalNotPositiveOverride: FormErrorWithFieldMessageOverrides = {
    FormErrorWithFieldMessageOverrides(
      formError             = incomeInputTotalNotPositiveSeed,
      fieldMessageOverrides = allInputsOverrides
    )
  }

  val spendingForm: Form[SpendingInput] = Form(
    mapping(
      "housing" -> validateSpending(text),
      "pension-contributions" -> validateSpending(text),
      "council-tax" -> validateSpending(text),
      "utilities" -> validateSpending(text),
      "debt-repayments" -> validateSpending(text),
      "travel" -> validateSpending(text),
      "childcare" -> validateSpending(text),
      "insurance" -> validateSpending(text),
      "groceries" -> validateSpending(text),
      "health" -> validateSpending(text)
    )(SpendingInput.apply)(SpendingInput.unapply)
  )

  private def validateSpending(mappingStr: Mapping[String]) = {
    mappingStr.verifying("ssttp.affordability.your-monthly-spending.error.non-numerals", { i: String =>
      if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
    })
  }

  def parseStringToBigDecimal(string: String): BigDecimal = string match {
    case s if s.isEmpty => BigDecimal(0)
    case s              => BigDecimal(s)
  }
}
