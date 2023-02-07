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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

case class PaymentsCalendar(
                           createdOn: LocalDate,
                           upfrontPaymentDate: Option[LocalDate],
                           regularPaymentsDay: Int
)(implicit config: ServicesConfig) {
  val daysFromCreatedDateToProcessFirstPayment: Int = config.getInt("paymentDatesConfig.daysToProcessPayment")
  val minimumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.minimumLengthOfPaymentPlan")
  val maximumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.maximumLengthOfPaymentPlan")

  lazy val regularPaymentDates: Seq[LocalDate] = upfrontPaymentDate
    .map(regularPaymentDatesFromDate)
    .getOrElse(
      if (regularPaymentsDaySufficientlyAfterDate(createdOn)) {
        regularPaymentDatesFromDate(createdOn)
      } else {
        regularPaymentDatesFromDate(createdOn.plusMonths(1))
      }
    )

  private def regularPaymentDatesFromDate(initialDate: LocalDate): Seq[LocalDate] = {
    (minimumLengthOfPaymentPlan to maximumLengthOfPaymentPlan)
      .map(initialDate.plusMonths(_).withDayOfMonth(regularPaymentsDay))
  }

  private def regularPaymentsDaySufficientlyAfterDate(date: LocalDate): Boolean = {
    regularPaymentsDay.compareTo(date.getDayOfMonth) >= daysFromCreatedDateToProcessFirstPayment
  }
}

