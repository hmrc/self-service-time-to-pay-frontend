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

sealed trait Expense extends EnumEntry with EnumEntry.Uncapitalised {
  val messageKey: String
}
object Expense extends Enum[Expense] with PlayInsensitiveJsonEnum[Expense] {
  val values: immutable.IndexedSeq[Expense] = findValues

  case object HousingExp extends Expense {
    val messageKey: String = "ssttp.affordability.your-monthly-spending.form.housing"
  }
  case object PensionContributionsExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.pension-contributions"
  }
  case object CouncilTaxExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.council-tax"
  }
  case object UtilitiesExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.utilities"
  }
  case object DebtRepaymentsExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.debt-repayments"
  }
  case object TravelExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.travel"
  }
  case object ChildcareExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.childcare"
  }
  case object InsuranceExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.insurance"
  }
  case object GroceriesExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.groceries"
  }
  case object HealthExp extends Expense {
    val messageKey = "ssttp.affordability.your-monthly-spending.form.health"
  }
}

final case class Expenses(category: Expense, amount: BigDecimal = 0)

object Expenses {
  implicit val format: OFormat[Expenses] = Json.format[Expenses]
}
