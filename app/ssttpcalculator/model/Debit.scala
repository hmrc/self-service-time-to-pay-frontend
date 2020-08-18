/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, Year}

import play.api.libs.json.{Json, OFormat}

case class Debit(
    amount:  BigDecimal,
    dueDate: LocalDate,
    endDate: LocalDate,
    rate:    InterestRate
) {

  def historicDailyRate: BigDecimal = rate.rate / BigDecimal(Year.of(dueDate.getYear).length()) / BigDecimal(100)

  def asDebitInput(debit: Debit): DebitInput = DebitInput(
    debit.amount,
    debit.dueDate
  )
}

case class Interest(amountAccrued: BigDecimal, calculationDate: LocalDate)

object Interest {
  implicit val formatInterest: OFormat[Interest] = Json.format[Interest]

}
