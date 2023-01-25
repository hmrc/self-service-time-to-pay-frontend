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

import enumeratum.EnumEntry
import enumformat.EnumFormat
import play.api.libs.json.{Format, Json, OFormat}

sealed trait IncomeCategory {
  val name: String
  val amount: BigDecimal
}

object IncomeCategory {
  implicit val format: OFormat[IncomeCategory] = Json.format[IncomeCategory]
}

final case class MonthlyIncome(amount: BigDecimal = 0) extends IncomeCategory {
  val name = "monthly-income"
}

object MonthlyIncome {
  implicit val format: OFormat[MonthlyIncome] = Json.format[MonthlyIncome]

  def apply: MonthlyIncome = MonthlyIncome()
}

final case class Benefits(amount: BigDecimal = 0) extends IncomeCategory {
  val name = "benefits"
}

object Benefits {
  implicit val format: OFormat[Benefits] = Json.format[Benefits]
}

final case class OtherIncome(amount: BigDecimal = 0) extends IncomeCategory {
  val name = "other-income"
}

object OtherIncome {
  implicit val format: OFormat[OtherIncome] = Json.format[OtherIncome]
}
