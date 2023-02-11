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

  lazy val regularPaymentDates: Seq[LocalDate] = {
    maybeUpfrontPaymentDate match {
      case Some(upfrontPaymentDate) =>
        calibrateStartOfRegularPayment(upfrontPaymentDate, timeForRegularPaymentThisMonthAfterUpfrontPayment)
      case None =>
        calibrateStartOfRegularPayment(planStartDate, timeForRegularPaymentThisMonthAfterStartDate)
    }
  }

  private def calibrateStartOfRegularPayment(
                                              baselineDate: LocalDate,
                                              timeForRegularPaymentThisMonth: LocalDate => Boolean
                                            ): Seq[LocalDate] = {
    if (!timeForRegularPaymentThisMonth(baselineDate)) {
        regularPaymentDatesFromNextMonth(baselineDate)
    } else {
      regularPaymentDatesFromNextMonth(baselineDate.minusMonths(1))
    }
  }

  private def regularPaymentDatesFromNextMonth(baselineDate: LocalDate): Seq[LocalDate] = {
    (config.minimumLengthOfPaymentPlan to config.maximumLengthOfPaymentPlan)
      .map(baselineDate.plusMonths(_).withDayOfMonth(regularPaymentsDay))
  }

  private def timeForRegularPaymentThisMonthAfterStartDate(startDate: LocalDate): Boolean = {
    !startDate.withDayOfMonth(regularPaymentsDay).minusDays(config.daysToProcessFirstPayment).isBefore(startDate)
  }

  private def timeForRegularPaymentThisMonthAfterUpfrontPayment(upfrontPaymentDate: LocalDate): Boolean = {
    !upfrontPaymentDate.withDayOfMonth(regularPaymentsDay).minusDays(config.minGapBetweenPayments).isBefore(upfrontPaymentDate)
  }
}

