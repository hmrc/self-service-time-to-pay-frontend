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


sealed trait Expenses {
  val messageKey: String
  val amount: BigDecimal
}

object Expenses {
  implicit val format: OFormat[Expenses] = Json.format[Expenses]
}

final case class Housing(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.housing"
}

object Housing {
  implicit val format: OFormat[Housing] = Json.format[Housing]

}

final case class PensionContributions(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.pension-contributions"
}

object PensionContributions {
  implicit val format: OFormat[PensionContributions] = Json.format[PensionContributions]

}

final case class CouncilTax(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.council-tax"
}

object CouncilTax {
  implicit val format: OFormat[CouncilTax] = Json.format[CouncilTax]

}

final case class Utilities(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.utilities"
}

object Utilities {
  implicit val format: OFormat[Utilities] = Json.format[Utilities]

}

final case class DebtRepayments(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.debt-repayments"
}

object DebtRepayments {
  implicit val format: OFormat[DebtRepayments] = Json.format[DebtRepayments]

}

final case class Travel(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.travel"
}

object Travel {
  implicit val format: OFormat[Travel] = Json.format[Travel]

}

final case class Childcare(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.childcare"
}

object Childcare {
  implicit val format: OFormat[Childcare] = Json.format[Childcare]

}

final case class Insurance(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.insurance"
}

object Insurance {
  implicit val format: OFormat[Insurance] = Json.format[Insurance]

}

final case class Groceries(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.groceries"
}

object Groceries {
  implicit val format: OFormat[Groceries] = Json.format[Groceries]

}

final case class Health(amount: BigDecimal = 0) extends Expenses {
  val messageKey = "ssttp.affordability.your-monthly-spending.form.health"
}

object Health {
  implicit val format: OFormat[Health] = Json.format[Health]

}
