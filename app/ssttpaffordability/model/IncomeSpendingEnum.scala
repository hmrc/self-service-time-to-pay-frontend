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

import scala.collection.immutable

sealed trait IncomeSpendingEnum extends EnumEntry with EnumEntry.Uncapitalised {
}
object IncomeSpendingEnum extends Enum[IncomeSpendingEnum] with PlayInsensitiveJsonEnum[IncomeSpendingEnum] {
  val values: immutable.IndexedSeq[IncomeSpendingEnum] = findValues

  case object Income extends IncomeSpendingEnum {
  }
  case object Spending extends IncomeSpendingEnum {
  }
}

