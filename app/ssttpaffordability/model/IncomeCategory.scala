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

import play.api.libs.json.{Json, OFormat}

sealed trait IncomeCategory {
  val heading: String
  val messageKey: String
  val amount: BigDecimal
}

object IncomeCategory {
  implicit val format: OFormat[IncomeCategory] = Json.format[IncomeCategory]
}

final case class MonthlyIncome(amount: BigDecimal = 0) extends IncomeCategory {
  val heading = "Monthly income after text"
  val messageKey = "ssttp.affordability.your-monthly-income.form.monthly-income"
}

object MonthlyIncome {
  implicit val format: OFormat[MonthlyIncome] = Json.format[MonthlyIncome]

}

final case class Benefits(amount: BigDecimal = 0) extends IncomeCategory {
  val heading = "Benefits"
  val messageKey = "ssttp.affordability.your-monthly-income.form.benefits"
}

object Benefits {
  implicit val format: OFormat[Benefits] = Json.format[Benefits]
}

final case class OtherIncome(amount: BigDecimal = 0) extends IncomeCategory {
  val heading = "Other monthly income"
  val messageKey = "ssttp.affordability.your-monthly-income.form.other-income"
}

object OtherIncome {
  implicit val format: OFormat[OtherIncome] = Json.format[OtherIncome]
}
