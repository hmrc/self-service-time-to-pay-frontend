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
import play.api.data.Form
import scala.util.Try

final case class MonthlySpendingForm()

object MonthlySpendingForm {

  def createMonthlySpendingForm(): Form[SpendingForm] = {
    Form(spendingMapping)
  }

  val spendingMapping = mapping(
    "housing" -> text
      .verifying("ssttp.affordability.your-monthly-income.error.non-numerals", { i: String =>
        if (i.nonEmpty) Try(BigDecimal(i)).isSuccess else true
      }),
    "pension-contributions" -> text,
    "council-tax" -> text,
    "utilities" -> text,
    "debt-repayments" -> text,
    "travel" -> text,
    "childcare" -> text,
    "insurance" -> text,
    "groceries" -> text,
    "health" -> text
  )((housing, pensionContribution, councilTax, utilities, debtRepayments, travel, childcare, insurance, groceries, health) => SpendingForm(
      housing,
      pensionContribution,
      councilTax,
      utilities,
      debtRepayments,
      travel,
      childcare,
      insurance,
      groceries,
      health
    ))(spendingForm => {
      Some(
        (spendingForm.housing.toString(),
          spendingForm.pensionContribution.toString(),
          spendingForm.councilTax.toString(),
          spendingForm.utilities.toString(),
          spendingForm.debtRepayments.toString(),
          spendingForm.travel.toString(),
          spendingForm.childcare.toString(),
          spendingForm.insurance.toString(),
          spendingForm.groceries.toString(),
          spendingForm.health.toString()
        )
      )
    })

}
