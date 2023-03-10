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

import java.time.LocalDate
import play.api.libs.json.{Json, OFormat}

import scala.math.BigDecimal.RoundingMode.HALF_UP

final case class PaymentSchedule(
    startDate:            LocalDate,
    endDate:              LocalDate,
    initialPayment:       BigDecimal,
    amountToPay:          BigDecimal,
    instalmentBalance:    BigDecimal,
    totalInterestCharged: BigDecimal,
    totalPayable:         BigDecimal,
    instalments:          Seq[Instalment]
) {
  def firstInstalment: Instalment =
    instalments.reduceOption(first).getOrElse(throw new RuntimeException(s"No instalments for [$this]"))

  private def first(earliest: Instalment, next: Instalment): Instalment =
    if (next.paymentDate.toEpochDay < earliest.paymentDate.toEpochDay) next else earliest

  def initialPaymentScheduleDate: LocalDate = firstInstalment.paymentDate

  lazy val lastPaymentDate: LocalDate = instalments.map(_.paymentDate).reduceLeftOption((a, b) => if (a.isAfter(b)) a else b).getOrElse(
    throw new UnsupportedOperationException(s"Instalments were empty [${this}]")
  )
}

object PaymentSchedule {
  implicit val format: OFormat[PaymentSchedule] = Json.format[PaymentSchedule]
}
