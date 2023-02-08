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
                             maybeUpfrontPaymentDate: Option[LocalDate],
                             regularPaymentsDay: Int
)(implicit config: ServicesConfig) {
  val daysFromCreatedDateToProcessFirstPayment: Int = config.getInt("paymentDatesConfig.daysToProcessPayment")
  val minGapBetweenPayments: Int = config.getInt("paymentDatesConfig.minGapBetweenPayments")
  val minimumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.minimumLengthOfPaymentPlan")
  val maximumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.maximumLengthOfPaymentPlan")

  lazy val regularPaymentDates: Seq[LocalDate] = {
    val baselineDate = maybeUpfrontPaymentDate.getOrElse(createdOn)

    if (noTimeForRegularPaymentDateThisMonth(baselineDate)) {
      regularPaymentDatesFromNextMonth(baselineDate)
    } else {
      regularPaymentDatesFromNextMonth(baselineDate.minusMonths(1))
    }
  }


  private def regularPaymentDatesFromNextMonth(baselineDate: LocalDate): Seq[LocalDate] = {
    (minimumLengthOfPaymentPlan to maximumLengthOfPaymentPlan)
      .map(baselineDate.plusMonths(_).withDayOfMonth(regularPaymentsDay))
  }


  // TODO OPS-9610 check old implementation. About how long the first regular payment is compared to:
  // - createdOn date when no upfront payment
  // - upfront payment when there is one.
  // this implementation ensures there's at least 14 days (max of 10 and 14)
  // but old implementation maybe went for 24 days from createdOn regardless of whether there's an upfront payment
  private def noTimeForRegularPaymentDateThisMonth(date: LocalDate): Boolean = {
    date.withDayOfMonth(regularPaymentsDay).minusDays(Math.max(
      daysFromCreatedDateToProcessFirstPayment,
      minGapBetweenPayments)).isBefore(date)
  }
}

