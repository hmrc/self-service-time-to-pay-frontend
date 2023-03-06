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

package audit.model

import play.api.libs.json.{Json, OFormat, Writes}
import ssttpcalculator.model.{Instalment, PaymentSchedule}

import scala.math.BigDecimal.RoundingMode.HALF_UP

case class AuditPaymentSchedule(
    totalPayable:                BigDecimal,
    instalmentDate:              Int,
    instalments:                 Seq[AuditInstalment],
    initialPaymentAmount:        BigDecimal,
    totalNoPayments:             Int,
    totalInterestCharged:        BigDecimal,
    totalPaymentWithoutInterest: BigDecimal
)

object AuditPaymentSchedule {

  def apply(paymentSchedule: PaymentSchedule): AuditPaymentSchedule = AuditPaymentSchedule(
    totalPayable                = paymentSchedule.totalPayable.setScale(2, HALF_UP),
    instalmentDate              = paymentSchedule.instalments.headOption.map(_.paymentDate.getDayOfMonth).getOrElse(throw new IllegalArgumentException(
      "could not find first instalment in payment schedule"
    )),
    instalments                 = (1 to paymentSchedule.instalments.length).zip(paymentSchedule.instalments)
      .map(
        i => AuditInstalment(
          i._2.amount.setScale(2, HALF_UP),
          i._1,
          i._2.paymentDate
        )
      ),
    initialPaymentAmount        = paymentSchedule.initialPayment,
    totalNoPayments             = {
      if (paymentSchedule.initialPayment == 0) paymentSchedule.instalments.length
      else paymentSchedule.instalments.length + 1
    },
    totalInterestCharged        = paymentSchedule.totalInterestCharged.setScale(2, HALF_UP),
    totalPaymentWithoutInterest = paymentSchedule.amountToPay
  )

  val writes: Writes[AuditPaymentSchedule] = (o: AuditPaymentSchedule) => Json.obj(
    "totalPayable" -> o.totalPayable,
    "instalmentDate" -> o.instalmentDate,
    "instalments" -> Json.toJson(o.instalments),
    "initialPaymentAmount" -> o.initialPaymentAmount,
    "totalNoPayments" -> o.totalNoPayments,
    "totalInterestCharged" -> o.totalInterestCharged,
    "totalPaymentWithoutInterest" -> o.totalPaymentWithoutInterest
  )

  implicit val format: OFormat[AuditPaymentSchedule] = Json.format[AuditPaymentSchedule]
}
