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

import scala.util.Try

object AffordabilityForm {

  def validateIncomeInputTotal(form: Form[IncomeInput]): Form[IncomeInput] = {
    if (!form.get.hasPositiveTotal) {
      val formErrorsWithTotalError = form.errors :+ FormError(
        key      = "monthlyIncome",
        messages = Seq("ssttp.affordability.your-monthly-income.error.required")
      )
      form.copy(errors = formErrorsWithTotalError)
    } else form
  }

  val incomeForm: Form[IncomeInput] = Form(
    mapping(
      "monthlyIncome" -> validateIncomeInput(text),
      "benefits" -> validateIncomeInput(text),
      "otherIncome" -> validateIncomeInput(text)
    )(IncomeInput.apply)(IncomeInput.unapply)
  )

  private def validateIncomeInput(mappingStr: Mapping[String]): Mapping[String] = mappingStr
    .verifying("ssttp.affordability.your-monthly-income.error.non-numerals", { i: String =>
      if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
    })
    .verifying("ssttp.affordability.your-monthly-income.error.decimal-places", { i =>
      if (Try(BigDecimal(i)).isSuccess) BigDecimal(i).scale <= 2 else true
    })

  val spendingForm: Form[SpendingInput] = Form(
    mapping(
      "housing" -> validate(text),
      "pension-contributions" -> validate(text),
      "council-tax" -> validate(text),
      "utilities" -> validate(text),
      "debt-repayments" -> validate(text),
      "travel" -> validate(text),
      "childcare" -> validate(text),
      "insurance" -> validate(text),
      "groceries" -> validate(text),
      "health" -> validate(text)
    )(SpendingInput.apply)(SpendingInput.unapply)
  )

  private def validate(mappingStr: Mapping[String]) = mappingStr.verifying("ssttp.affordability.your-monthly-spending.error.non-numerals", { i: String =>
    if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
  })

  def parseStringToBigDecimal(string: String): BigDecimal = string match {
    case s if s.isEmpty => BigDecimal(0)
    case s              => BigDecimal(s)
  }
}
