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
import play.api.libs.json.{Json, OFormat, OWrites, Reads}
import uk.gov.hmrc.selfservicetimetopay.models.RegularPaymentDay

case class PaymentsCalendar(
    planStartDate:           LocalDate,
    maybeUpfrontPaymentDate: Option[LocalDate],
    regularPaymentDates:     Seq[LocalDate],
    regularPaymentsDay:      Int
)(implicit config: AppConfig)

object PaymentsCalendar {
  val defaultRegularPaymentDay = 28

  def generate(
      taxLiabilities:         Seq[TaxLiability],
      upfrontPaymentAmount:   BigDecimal,
      dateNow:                LocalDate,
      maybeRegularPaymentDay: Option[RegularPaymentDay] = None
  )(implicit config: AppConfig): PaymentsCalendar = PaymentsCalendar(
    maybeUpfrontPaymentDate = upfrontPaymentDateIfViable(dateNow, upfrontPaymentAmount, taxLiabilities),
    planStartDate           = dateNow,
    regularPaymentDates     = regularPaymentDates(dateNow, upfrontPaymentAmount, taxLiabilities, maybeRegularPaymentDay),
    regularPaymentsDay      = safeRegularPaymentsDay(maybeRegularPaymentDay),
  )

  private def upfrontPaymentDateIfViable(
      dateNow:              LocalDate,
      upfrontPaymentAmount: BigDecimal,
      taxLiabilities:       Seq[TaxLiability]
  )(implicit config: AppConfig): Option[LocalDate] = {
    if (upfrontPaymentAmount > 0 &&
      !((taxLiabilities.map(_.amount).sum - upfrontPaymentAmount) < BigDecimal.exact("32.00"))) {
      Some(validPaymentDate(dateNow.plusDays(config.daysToProcessFirstPayment))(config))
    } else None
  }

  private def regularPaymentDates(
      dateNow:                LocalDate,
      upfrontPaymentAmount:   BigDecimal,
      taxLiabilities:         Seq[TaxLiability],
      maybeRegularPaymentDay: Option[RegularPaymentDay]
  )(implicit config: AppConfig): Seq[LocalDate] = {
    upfrontPaymentDateIfViable(dateNow, upfrontPaymentAmount, taxLiabilities)(config) match {
      case Some(upfrontPaymentDate) => validMonthlyDatesFrom(upfrontPaymentDate, config.minGapBetweenPayments, maybeRegularPaymentDay)
      case None                     => validMonthlyDatesFrom(dateNow, config.daysToProcessFirstPayment, maybeRegularPaymentDay)
    }
  }

  private def safeRegularPaymentsDay(maybeRegularPaymentDay: Option[RegularPaymentDay])(implicit config: AppConfig): Int = {
    maybeRegularPaymentDay.fold(defaultRegularPaymentDay)(rpd =>
      if (rpd.dayOfMonth <= config.lastPaymentDayOfMonth) rpd.dayOfMonth else config.firstPaymentDayOfMonth
    )
  }

  private def validMonthlyDatesFrom(
      date:                   LocalDate,
      setUpPeriod:            Int,
      maybeRegularPaymentDay: Option[RegularPaymentDay])(implicit config: AppConfig): Seq[LocalDate] = {
    (config.minimumLengthOfPaymentPlan to config.maximumLengthOfPaymentPlan)
      .map(i => {
        val regularPaymentDateFirstMonth = date.withDayOfMonth(safeRegularPaymentsDay(maybeRegularPaymentDay))
        if (regularPaymentDateFirstMonth.isAfter(date.plusDays(setUpPeriod - 1))) {
          regularPaymentDateFirstMonth.plusMonths(i - 1)
        } else {
          if (regularPaymentDateFirstMonth.plusMonths(1).isAfter(date.plusDays(setUpPeriod - 1))) {
            regularPaymentDateFirstMonth.plusMonths(i)
          } else {
            regularPaymentDateFirstMonth.plusMonths(i + 1)
          }
        }
      })
  }

  private def validPaymentDate(date: LocalDate)(implicit config: AppConfig): LocalDate = {
    val dayOfMonth = date.getDayOfMonth
    if (dayOfMonth >= config.firstPaymentDayOfMonth && dayOfMonth <= config.lastPaymentDayOfMonth) {
      date
    } else if (dayOfMonth < config.firstPaymentDayOfMonth) {
      date.withDayOfMonth(config.firstPaymentDayOfMonth)
    } else {
      date.plusMonths(1).withDayOfMonth(1)
    }
  }

  private def reads(implicit config: AppConfig): Reads[PaymentsCalendar] = Json.reads[PaymentsCalendar]

  private def writes(implicit config: AppConfig): OWrites[PaymentsCalendar] = Json.writes[PaymentsCalendar]

  implicit def format(implicit config: AppConfig): OFormat[PaymentsCalendar] = OFormat(reads, writes)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
