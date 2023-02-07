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

import journey.PaymentToday
import play.api.Logger
import play.api.mvc.Request
import ssttpcalculator.model.PaymentsCalendar
import times.ClockProvider
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

import java.time.LocalDate
import javax.inject.Inject

class PaymentDatesService @Inject() (
                                    clockProvider: ClockProvider,
                                    config: ServicesConfig
                                    ){
  import clockProvider._

  val logger = Logger(getClass)

  val minimumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.minimumLengthOfPaymentPlan")
  val maximumLengthOfPaymentPlan: Int = config.getInt("paymentDatesConfig.maximumLengthOfPaymentPlan")
  val daysToProcessUpfrontPayment: Int = config.getInt("paymentDatesConfig.daysToProcessPayment")
  val minGapBetweenPayments: Int = config.getInt("paymentDatesConfig.minGapBetweenPayments")
  val firstPaymentDayOfMonth: Int = config.getInt("paymentDatesConfig.firstPaymentDayOfMonth")
  val lastPaymentDayOfMonth: Int = config.getInt("paymentDatesConfig.lastPaymentDayOfMonth")


  // TODO OPS-9610: deal with upfront payment (or lack there of)
  // TODO OPS-9610: consider change to defaultRegularPaymentsDay if no upfront payment
  // TODO OPS-9610: deal with payment date that is more than 28
  def paymentsCalendar(
                        maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth],
                        maybePaymentToday: Option[PaymentToday]
                      )(
    implicit request: Request[_],
    config: ServicesConfig
  ): PaymentsCalendar = {
    val validLengthOfPaymentPlan = minimumLengthOfPaymentPlan to maximumLengthOfPaymentPlan
    val today = clockProvider.nowDate()

    val defaultRegularPaymentsDay = today
      .plusDays(daysToProcessUpfrontPayment)
      .plusDays(minGapBetweenPayments)
      .getDayOfMonth

    PaymentsCalendar(
      createdOn = today,
      upfrontPaymentDate = maybePaymentToday
        .map(_ => dateWithValidPaymentDay(
          today.plusDays(daysToProcessUpfrontPayment),
          firstPaymentDayOfMonth,
          lastPaymentDayOfMonth
        )),
      regularPaymentsDay = maybeArrangementDayOfMonth.map(_.dayOfMonth).getOrElse(defaultRegularPaymentsDay)
    )
  }

  private def dateWithValidPaymentDay(
                                       date: LocalDate,
                                       firstPaymentDayOfMonth: Int,
                                       lastPaymentDayOfMonth: Int
                                     ): LocalDate = {
    val dayOfMonth = date.getDayOfMonth
    if (dayOfMonth >= firstPaymentDayOfMonth && dayOfMonth <= lastPaymentDayOfMonth ) {
      date
    } else if (dayOfMonth < firstPaymentDayOfMonth) {
      date.withDayOfMonth(firstPaymentDayOfMonth)
    } else {
      date.plusMonths(1).withDayOfMonth(1)
    }
  }
}
