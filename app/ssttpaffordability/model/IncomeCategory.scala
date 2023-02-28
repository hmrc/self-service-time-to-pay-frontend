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

package ssttpaffordability.model

import enumeratum._
import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable

sealed trait IncomeCategory extends EnumEntry with EnumEntry.Uncapitalised {
  val messageKey: String
}

object IncomeCategory extends Enum[IncomeCategory] with PlayInsensitiveJsonEnum[IncomeCategory] {
  val values: immutable.IndexedSeq[IncomeCategory] = findValues

  case object MonthlyIncome extends IncomeCategory {
    val messageKey = "ssttp.affordability.your-monthly-income.form.monthly-income"
  }

  case object Benefits extends IncomeCategory {
    val messageKey = "ssttp.affordability.your-monthly-income.form.benefits"
  }

  case object OtherIncome extends IncomeCategory {
    val messageKey = "ssttp.affordability.your-monthly-income.form.other-income"
  }
}

final case class IncomeBudgetLine(
    category: IncomeCategory,
    amount:   BigDecimal     = 0
)

object IncomeBudgetLine {
  implicit val format: OFormat[IncomeBudgetLine] = Json.format[IncomeBudgetLine]
}
