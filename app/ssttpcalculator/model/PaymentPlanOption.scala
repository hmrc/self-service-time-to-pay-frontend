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

package ssttpcalculator.model

import enumeratum._

import scala.collection.immutable

sealed trait PaymentPlanOption extends EnumEntry with EnumEntry.Uncapitalised with Product with Serializable {
}

object PaymentPlanOption extends Enum[PaymentPlanOption] with PlayInsensitiveJsonEnum[PaymentPlanOption] {
  val values: immutable.IndexedSeq[PaymentPlanOption] = findValues

  case object Basic extends PaymentPlanOption

  case object Higher extends PaymentPlanOption

  case object Additional extends PaymentPlanOption

  case object Custom extends PaymentPlanOption {
    val idKey = "custom"
  }

}

