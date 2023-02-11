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

import config.AppConfig
import journey.PaymentToday
import play.api.Logger
import ssttpcalculator.model.PaymentsCalendar
import times.ClockProvider
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate
import javax.inject.Inject

class PaymentDatesService @Inject() (
    clockProvider: ClockProvider,
    config:        AppConfig
) {
  val logger: Logger = Logger(getClass)

  val daysToProcessUpfrontPayment: Int = config.daysToProcessUpfrontPayment
  val minGapBetweenPayments: Int = config.minGapBetweenPayments
  val firstPaymentDayOfMonth: Int = config.firstPaymentDayOfMonth
  val lastPaymentDayOfMonth: Int = config.lastPaymentDayOfMonth

  // TODO OPS-9610: deal with upfront payment (or lack there of)
  // TODO OPS-9610: consider change to defaultRegularPaymentsDay if no upfront payment
  // TODO OPS-9610: deal with payment date that is more than 28
  def paymentsCalendar(
      maybePaymentToday:          Option[PaymentToday],
      maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth],
      dateToday:                  LocalDate
  )(implicit config: AppConfig): PaymentsCalendar = {

    val maybeUpfrontPaymentDate: Option[LocalDate] = maybePaymentToday.map(_ => {
      dateWithValidPaymentDay(
        date = dateToday.plusDays(daysToProcessUpfrontPayment),
        firstPaymentDayOfMonth = firstPaymentDayOfMonth,
        lastPaymentDayOfMonth = lastPaymentDayOfMonth
      )
    })

    val validDefaultRegularPaymentsDay: Int = {
      dateWithValidPaymentDay(
        dateToday.plusDays(daysToProcessUpfrontPayment).plusDays(minGapBetweenPayments),
        firstPaymentDayOfMonth,
        lastPaymentDayOfMonth
      ).getDayOfMonth
    }

    val validCustomerPreferredRegularPaymentDay: Option[Int] = {
      maybeArrangementDayOfMonth.map( arrangementDayOfMonth => {
        dateWithValidPaymentDay(
          dateToday.withDayOfMonth(arrangementDayOfMonth.dayOfMonth),
          firstPaymentDayOfMonth,
          lastPaymentDayOfMonth
        ).getDayOfMonth
      })
    }

    PaymentsCalendar(
      planStartDate           = dateToday,
      maybeUpfrontPaymentDate = maybeUpfrontPaymentDate,
      regularPaymentsDay      = validCustomerPreferredRegularPaymentDay.getOrElse(validDefaultRegularPaymentsDay)
    )
  }

  private def dateWithValidPaymentDay(
      date:                   LocalDate,
      firstPaymentDayOfMonth: Int,
      lastPaymentDayOfMonth:  Int
  ): LocalDate = {
    val dayOfMonth = date.getDayOfMonth
    if (dayOfMonth >= firstPaymentDayOfMonth && dayOfMonth <= lastPaymentDayOfMonth) { date }
    else if (dayOfMonth < firstPaymentDayOfMonth) { date.withDayOfMonth(firstPaymentDayOfMonth) }
    else { date.plusMonths(1).withDayOfMonth(1) }
  }
}
