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

import config.AppConfig

import java.time.LocalDate

case class PaymentsCalendar(
    planStartDate:           LocalDate,
    maybeUpfrontPaymentDate: Option[LocalDate],
    regularPaymentsDay:      Int
)(implicit config: AppConfig) {

  private val minimumLengthOfPaymentPlan = config.minimumLengthOfPaymentPlan
  private val maximumLengthOfPaymentPlan = config.maximumLengthOfPaymentPlan
  private val minGapBetweenPayments = config.minGapBetweenPayments
  private val daysToProcessFirstPayment = config.daysToProcessFirstPayment
  private val firstPaymentDayOfMonth = config.firstPaymentDayOfMonth
  private val lastPaymentDayOfMonth = config.lastPaymentDayOfMonth

  def regularPaymentDates: Seq[LocalDate] = {
    (minimumLengthOfPaymentPlan to maximumLengthOfPaymentPlan)
      .map(i => maybeUpfrontPaymentDate match {
        case Some(upfrontPaymentDate) =>
          val regularPaymentDateFirstMonth = upfrontPaymentDate.withDayOfMonth(regularPaymentsDay)
          if (regularPaymentDateFirstMonth.isAfter(upfrontPaymentDate.plusDays(minGapBetweenPayments - 1))) {
            regularPaymentDateFirstMonth.plusMonths(i - 1)
          } else {
            if (regularPaymentDateFirstMonth.plusMonths(1).isAfter(upfrontPaymentDate.plusDays(minGapBetweenPayments - 1))) {
              regularPaymentDateFirstMonth.plusMonths(i)
            } else {
              regularPaymentDateFirstMonth.plusMonths(i + 1)
            }
          }
        case None =>
          val validBaselineDate = validPaymentDate(planStartDate)
          val validRegularPaymentDateFirstMonth = validBaselineDate.withDayOfMonth(regularPaymentsDay)
          if (validRegularPaymentDateFirstMonth.isAfter(validBaselineDate.plusDays(daysToProcessFirstPayment - 1))) {
            validRegularPaymentDateFirstMonth.plusMonths(i - 1)
          } else {
            if (validRegularPaymentDateFirstMonth.plusMonths(i).isAfter(validBaselineDate.plusDays(daysToProcessFirstPayment - 1))) {
              validRegularPaymentDateFirstMonth.plusMonths(i)
            } else {
              validRegularPaymentDateFirstMonth.plusMonths(i + 1)
            }
          }
      })
  }

  def validPaymentDate(date: LocalDate): LocalDate = {
    val dayOfMonth = date.getDayOfMonth
    if (dayOfMonth >= firstPaymentDayOfMonth && dayOfMonth <= lastPaymentDayOfMonth) { date }
    else if (dayOfMonth < firstPaymentDayOfMonth) { date.withDayOfMonth(firstPaymentDayOfMonth) }
    else { date.plusMonths(1).withDayOfMonth(1) }
  }
}

object PaymentsCalendar {

}

