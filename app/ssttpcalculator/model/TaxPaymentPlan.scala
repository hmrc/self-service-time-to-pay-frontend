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
import journey.PaymentToday

import java.time.LocalDate
import play.api.libs.json.{Json, JsonValidationError, OFormat, OWrites}
import uk.gov.hmrc.selfservicetimetopay.models.ArrangementDayOfMonth

case class TaxPaymentPlan(
    taxLiabilities:             Seq[TaxLiability],
    upfrontPayment:             BigDecimal,
    planStartDate:              LocalDate,
    regularPaymentAmount:       BigDecimal,
    maybeArrangementDayOfMonth: Option[ArrangementDayOfMonth] = None,
    maybePaymentToday:          Option[PaymentToday]          = None
)(implicit config: AppConfig) {
  import TaxPaymentPlan._

  private val minimumLengthOfPaymentPlan = config.minimumLengthOfPaymentPlan
  private val maximumLengthOfPaymentPlan = config.maximumLengthOfPaymentPlan
  private val minGapBetweenPayments = config.minGapBetweenPayments
  private val daysToProcessFirstPayment = config.daysToProcessFirstPayment
  private val firstPaymentDayOfMonth = config.firstPaymentDayOfMonth
  private val lastPaymentDayOfMonth = config.lastPaymentDayOfMonth

  val maybeUpfrontPaymentDate: Option[LocalDate] = maybePaymentToday.map(_ => {
    validPaymentDate(planStartDate.plusDays(daysToProcessFirstPayment))
  })

  val firstRegularPaymentDate: Option[LocalDate] = regularPaymentDates.headOption

  def remainingLiability: BigDecimal = taxLiabilities.map(_.amount).sum - upfrontPayment

  def actualStartDate: LocalDate = planStartDate

  def outstandingLiabilities: Seq[TaxLiability] = {
    val result = taxLiabilities.sortBy(_.dueDate).foldLeft((upfrontPayment, Seq.empty[TaxLiability])){
      case ((p, s), lt) if p <= 0         => (p, s :+ lt.copy(dueDate = if (planStartDate.isBefore(lt.dueDate)) lt.dueDate else planStartDate))

      case ((p, s), lt) if p >= lt.amount => (p - lt.amount, s)

      case ((p, s), lt) => (0, s :+ lt.copy(amount  = lt.amount - p,
                                            dueDate = if (planStartDate.plusWeeks(1).isBefore(lt.dueDate)) lt.dueDate else planStartDate.plusWeeks(1)))
    }
    result._2
  }
  def regularPaymentsDay: Int = validCustomerPreferredRegularPaymentDay.getOrElse(validDefaultRegularPaymentsDay)

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

  private def validCustomerPreferredRegularPaymentDay: Option[Int] = {
    maybeArrangementDayOfMonth.map(arrangementDayOfMonth => {
      validPaymentDate(planStartDate.withDayOfMonth(arrangementDayOfMonth.dayOfMonth)).getDayOfMonth
    })
  }

  private def validDefaultRegularPaymentsDay: Int = {
    validPaymentDate(planStartDate.plusDays(daysToProcessFirstPayment).plusDays(minGapBetweenPayments)).getDayOfMonth
  }

  private def validPaymentDate(date: LocalDate): LocalDate = {
    val dayOfMonth = date.getDayOfMonth
    if (dayOfMonth >= firstPaymentDayOfMonth && dayOfMonth <= lastPaymentDayOfMonth) {
      date
    } else if (dayOfMonth < firstPaymentDayOfMonth) {
      date.withDayOfMonth(firstPaymentDayOfMonth)
    } else {
      date.plusMonths(1).withDayOfMonth(1)
    }
  }
}

object TaxPaymentPlan {

  private def reads(implicit config: AppConfig) = Json.reads[TaxPaymentPlan]
    .filter(JsonValidationError("'debits' was empty, it should have at least one debit."))(_.taxLiabilities.nonEmpty)
    .filter(JsonValidationError("The 'initialPayment' can't be less than 0"))(_.upfrontPayment >= 0)

  private def writes(implicit config: AppConfig): OWrites[TaxPaymentPlan] = Json.writes[TaxPaymentPlan]

  implicit def format(implicit config: AppConfig): OFormat[TaxPaymentPlan] = OFormat(reads, writes)

  implicit def orderingLocalDate: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
}
