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

package uk.gov.hmrc.selfservicetimetopay.models

import java.time.LocalDate

import play.api.libs.json.Json
import uk.gov.hmrc.selfservicetimetopay.modelsFormat.localDateOrdering

final case class CalculatorPaymentSchedule(startDate:            Option[LocalDate],
                                           endDate:              Option[LocalDate],
                                           initialPayment:       BigDecimal,
                                           amountToPay:          BigDecimal,
                                           instalmentBalance:    BigDecimal,
                                           totalInterestCharged: BigDecimal,
                                           totalPayable:         BigDecimal,
                                           instalments:          Seq[CalculatorPaymentScheduleInstalment]) {
  def getMonthlyInstalment: BigDecimal = instalments.head.amount
  def getMonthlyInstalmentDate: Int = instalments.head.paymentDate.getDayOfMonth
  def initialPaymentScheduleDate: LocalDate = instalments.map(_.paymentDate).min
  def getUpFrontPayment: BigDecimal = initialPayment
  def getMonthlyDateFormatted: String = {
    val date = getMonthlyInstalmentDate.toString
    val postfix = {
      if (date == "11" || date == "12" || date == "13") "th"
      else if (date.endsWith("1")) "st"
      else if (date.endsWith("2")) "nd"
      else if (date.endsWith("3")) "rd"
      else "th"
    }
    s"$date$postfix"
  }
}

case class CalculatorPaymentScheduleExt(
    months:   Int, //it's unique in the list
    schedule: CalculatorPaymentSchedule
)

final case class CalculatorPaymentScheduleInstalment(paymentDate: LocalDate, amount: BigDecimal) {
  def getDateInReadableFormat: String = s"${paymentDate.getMonth} ${paymentDate.getYear.toString}".toLowerCase.capitalize
}
