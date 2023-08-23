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

package ssttpcalculator

sealed trait CalculatorType {
  val value: String
  lazy val messagesKeySuffix: String = value.toLowerCase
}

object CalculatorType {
  case object Legacy extends CalculatorType {
    val value: String = "Legacy"
  }

  case object PaymentOptimised extends CalculatorType {
    val value: String = "PaymentOptimised"
  }
}
