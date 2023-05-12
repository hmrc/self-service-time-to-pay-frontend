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
import ssttpaffordability.model.IncomeSpendingEnum
import ssttpaffordability.model.IncomeCategory.{Benefits, MonthlyIncome, OtherIncome}
import ssttpaffordability.model.forms.helper.FormErrorWithFieldMessageOverrides
import util.CurrencyUtil

import scala.util.Try

object AffordabilityForm {

  val incomeForm: Form[IncomeInput] = Form(
    mapping(
      "monthlyIncome" -> validateFormField(text, MonthlyIncome.messageSuffix, IncomeSpendingEnum.Income),
      "benefits" -> validateFormField(text, Benefits.messageSuffix, IncomeSpendingEnum.Income),
      "otherIncome" -> validateFormField(text, OtherIncome.messageSuffix, IncomeSpendingEnum.Income)
    )(IncomeInput.apply)(IncomeInput.unapply)
  )

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
      "housing" -> validateFormField(text, "housing", IncomeSpendingEnum.Spending),
      "pension-contributions" -> validateFormField(text, "pension-contributions", IncomeSpendingEnum.Spending),
      "council-tax" -> validateFormField(text, "council-tax", IncomeSpendingEnum.Spending),
      "utilities" -> validateFormField(text, "utilities", IncomeSpendingEnum.Spending),
      "debt-repayments" -> validateFormField(text, "debt-repayments", IncomeSpendingEnum.Spending),
      "travel" -> validateFormField(text, "travel", IncomeSpendingEnum.Spending),
      "childcare" -> validateFormField(text, "childcare", IncomeSpendingEnum.Spending),
      "insurance" -> validateFormField(text, "insurance", IncomeSpendingEnum.Spending),
      "groceries" -> validateFormField(text, "groceries", IncomeSpendingEnum.Spending),
      "health" -> validateFormField(text, "health", IncomeSpendingEnum.Spending)
    )(SpendingInput.apply)(SpendingInput.unapply)
  )

  private def validateFormField(mappingStr: Mapping[String], inputField: String, category: IncomeSpendingEnum): Mapping[String] = {
    mappingStr
      .verifying(s"ssttp.affordability.your-monthly-${category.entryName}.error.non-numerals.$inputField", { i: String =>
        i.isEmpty | i.matches(CurrencyUtil.regex)
      })
      .verifying(s"ssttp.affordability.your-monthly-${category.entryName}.error.negative.$inputField", { i: String =>
        if (Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess && BigDecimal(CurrencyUtil.cleanAmount(i)).scale <= 2) BigDecimal(CurrencyUtil.cleanAmount(i)) >= 0.00 else true
      })
      .verifying(s"ssttp.affordability.your-monthly-${category.entryName}.error.decimal-places.$inputField", { i: String =>
        if (Try(BigDecimal(CurrencyUtil.cleanAmount(i))).isSuccess) BigDecimal(CurrencyUtil.cleanAmount(i)).scale <= 2 else true
      })
  }

  def parseStringToBigDecimal(string: String): BigDecimal = string match {
    case s if s.isEmpty => BigDecimal(0)
    case s              => BigDecimal(CurrencyUtil.cleanAmount(s))
  }
}
